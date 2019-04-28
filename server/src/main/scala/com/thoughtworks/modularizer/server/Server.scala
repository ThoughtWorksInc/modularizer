package com.thoughtworks.modularizer.server

import java.nio.file.{Files, Paths}
import java.util.UUID

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.model.headers.CacheDirectives.`max-age`
import akka.http.scaladsl.model.headers.{ETag, EntityTag, `Cache-Control`, `If-Match`}
import akka.http.scaladsl.model.{HttpRequest, RequestEntity}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.util.Tuple
import akka.http.scaladsl.server.{Directive, Directive1, Route, StandardRoute, ValidationRejection}
import akka.stream.Materializer
import akka.stream.scaladsl.FileIO
import com.thoughtworks.akka.http.WebJarsSupport._
import com.thoughtworks.dsl.Dsl
import com.thoughtworks.dsl.Dsl.!!
import com.thoughtworks.dsl.keywords.NoneSafe.implicitNoneSafe
import com.thoughtworks.dsl.keywords.{Await, Return, Using}
import com.typesafe.scalalogging.Logger
import io.github.lhotari.akka.http.health.HealthEndpoint._
import org.eclipse.jgit.api.ResetCommand.ResetType
import org.eclipse.jgit.api.errors.TransportException
import org.eclipse.jgit.lib.Constants._
import org.eclipse.jgit.lib.Ref
import org.eclipse.jgit.revwalk.{RevCommit, RevWalk}
import org.eclipse.jgit.transport.RemoteRefUpdate.Status
import org.eclipse.jgit.transport.{PushResult, RefSpec, UsernamePasswordCredentialsProvider}

import scala.collection.JavaConverters._

/**
  * @author 杨博 (Yang Bo)
  */
private object Server {
  val logger: Logger = Logger[Server]
}

import com.thoughtworks.modularizer.server.Server.logger
class Server(configuration: Configuration, gitPool: GitPool)(implicit system: ActorSystem, materializer: Materializer) {
  import system.dispatcher

  implicit def directiveDsl[Keyword, Value, T: Tuple](
      implicit continuationDsl: Dsl[Keyword, Route !! T, Value]
  ): Dsl[Keyword, Directive[T], Value] = { (keyword, handler) =>
    Directive(continuationDsl.cpsApply(keyword, { value: Value =>
      handler(value).tapply
    }))
  }

  implicit def standardRouteDsl[Keyword, Value](
      implicit routeDsl: Dsl[Keyword, Route, Value]
  ): Dsl[Keyword, StandardRoute, Value] = { (keyword, handler) =>
    StandardRoute(routeDsl.cpsApply(keyword, handler))
  }

  val serverBinding = {
    Http.apply().bindAndHandle(route, configuration.listeningHost, configuration.listeningPort)
  }

  private def credentialsProviderOption: Option[UsernamePasswordCredentialsProvider] = {
    Some(new UsernamePasswordCredentialsProvider(!configuration.gitUsername, !configuration.gitPassword))
  }

  def route: Route = {
    respondWithHeader(`Cache-Control`(`max-age`(0))) {
      pathPrefix("api") {
        pathPrefix("git") {
          pathPrefix("branches") {
            pathPrefix(Segment) { branch: String =>
              pathPrefix("files") {
                pathSuffix(Segment) { fileName: String =>
                  rawPathPrefix((Slash ~ Segment).repeat(0, 128)) { directorySegments: List[String] =>
                    logger.debug(s"Acquiring a git work tree...")
                    val git = !Using(gitPool.acquire())
                    val parentPath = Paths.get(git.getRepository.getWorkTree.getPath, directorySegments: _*)
                    val fullPath = parentPath.resolve(fileName)

                    def forceCheckoutBranch: Directive1[Option[Ref]] = Directive { continue =>
                      val branchRef = R_HEADS + branch
                      try {
                        val result = git
                          .fetch()
                          .setRemote(configuration.gitUri)
                          .setCredentialsProvider(credentialsProviderOption.orNull)
                          .setRefSpecs(branchRef)
                          .call()

                        git
                          .branchCreate()
                          .setForce(true)
                          .setName(branch)
                          .setStartPoint {
                            val revWalk = new RevWalk(git.getRepository)
                            try {
                              revWalk.parseCommit(result.getAdvertisedRef(branchRef).getObjectId)
                            } finally {
                              revWalk.close()
                            }
                          }
                          .call()

                        val ref = git
                          .checkout()
                          // Disable setForced because jgit will throw a NullPointerException when work tree is clean
                          //.setForced(true)
                          .setForceRefUpdate(true)
                          .setName(branch)
                          .call()

                        continue(Tuple1(Some(ref)))
                      } catch {
                        case e: IllegalArgumentException =>
                          logger.debug("Failed to call git fetch", e)
                          reject(ValidationRejection(e.getMessage, Some(e)))
                        case e: TransportException =>
                          // FIXME: distinguishing "branch not found" from "network error"
                          logger.debug("Failed to call git fetch", e)
                          continue(Tuple1(None))
                      }
                    }
                    get {
                      forceCheckoutBranch {
                        case Some(ref) =>
                          val eTag = EntityTag(ref.getObjectId.name)
                          conditional(eTag) {
                            mapSettings(_.withFileGetConditional(false)) {
                              getFromFile(fullPath.toFile)
                            }
                          }
                        case None =>
                          reject
                      }
                    } ~ put {
                      extractRequest { request: HttpRequest =>
                        logger.debug(s"Uploading file to $parentPath on branch $branch")

                        forceCheckoutBranch { refOption: Option[Ref] =>
                          logger.debug(s"Checked out $refOption")
                          val oldRef = refOption.getOrElse {
                            git
                              .checkout()
                              .setName(UUID.randomUUID().toString)
                              .setOrphan(true)
                              .setForced(true)
                              .setForceRefUpdate(true)
                              .call()

                            git
                              .reset()
                              .setMode(ResetType.HARD)
                              .call()
                          }

                          def upload: Directive[(RevCommit, Iterable[PushResult])] = {
                            extractRequestEntity.flatMap { entity: RequestEntity =>
                              val requestBody = entity.dataBytes
                              logger.debug(s"Saving request body to file $fullPath")
                              Files.createDirectories(parentPath)
                              val ioResult = !Await(requestBody.runWith(FileIO.toPath(fullPath)))
                              val _ = ioResult.status.get

                              git
                                .add()
                                .addFilepattern(".")
                                .call()

                              val commit = git
                                .commit()
                                .setMessage(s"Update $parentPath on branch $branch")
                                .setAuthor("Modularizer", "atryyang@thoughtworks.com")
                                .setAllowEmpty(true)
                                .call()

                              logger.debug(s"Commit ${commit.toObjectId.name} created")

                              val uploadResults = git
                                .push()
                                .setRemote(configuration.gitUri)
                                .setCredentialsProvider(credentialsProviderOption.orNull)
                                .setRefSpecs(new RefSpec().setSourceDestination(commit.toObjectId.name,
                                                                                s"$R_HEADS$branch"))
                                .call()
                                .asScala

                              Directive(_(commit, uploadResults))
                            }
                          }

                          def create() = {
                            upload { (commit, pushResults) =>
                              if (pushResults.exists(_.getRemoteUpdates.asScala.exists(_.getStatus != Status.OK))) {
                                complete(Conflict)
                              } else {
                                respondWithHeader(ETag(commit.getId.name)) {
                                  complete(Created)
                                }
                              }
                            }
                          }

                          def modify() = {
                            upload { (commit, pushResults) =>
                              if (pushResults.exists(_.getRemoteUpdates.asScala.exists(_.getStatus != Status.OK))) {
                                complete(PreconditionFailed)
                              } else {
                                respondWithHeader(ETag(commit.getId.name)) {
                                  complete(NoContent)
                                }
                              }
                            }
                          }

                          if (oldRef == null || oldRef.getObjectId == null) {
                            if (request.header[`If-Match`].isDefined) {
                              complete(PreconditionFailed)
                            } else {
                              create()
                            }
                          } else {
                            if (Files.exists(fullPath)) {
                              logger.debug(s"Found previous revision ${oldRef.getObjectId.name}")
                              if (request.header[`If-Match`].isDefined) {
                                conditional(EntityTag(oldRef.getObjectId.name)) {
                                  modify()
                                }
                              } else {
                                complete(PreconditionRequired)
                              }
                            } else {
                              conditional(EntityTag(oldRef.getObjectId.name)) {
                                create()
                              }
                            }
                          }
                        }
                      }
                    }
                  }
                }
              }
            }
          }
        } ~ createDefaultHealthRoute()
      } ~ pathSingleSlash {
        getFromResource(webJarAssetLocator.getFullPath("index.html"))
      } ~ sbtWeb
    }
  }
}
