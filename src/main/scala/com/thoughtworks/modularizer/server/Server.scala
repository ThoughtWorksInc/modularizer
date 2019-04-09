package com.thoughtworks.modularizer.server

import java.nio.file.Files

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.model.headers.CacheDirectives.`max-age`
import akka.http.scaladsl.model.headers.{ETag, EntityTag, `Cache-Control`, `If-Match`}
import akka.http.scaladsl.model.{HttpRequest, RequestEntity}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.util.Tuple
import akka.http.scaladsl.server.{Directive, Route, StandardRoute}
import akka.stream.Materializer
import akka.stream.scaladsl.FileIO
import com.thoughtworks.akka.http.WebJarsSupport._
import com.thoughtworks.dsl.Dsl
import com.thoughtworks.dsl.Dsl.!!
import com.thoughtworks.dsl.keywords.NullSafe._
import com.thoughtworks.dsl.keywords.{Await, Using}
import com.typesafe.scalalogging.Logger
import io.github.lhotari.akka.http.health.HealthEndpoint._
import org.eclipse.jgit.api.ResetCommand.ResetType
import org.eclipse.jgit.api.errors.TransportException
import org.eclipse.jgit.lib.Constants._
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
    Http.apply().bindAndHandle(route, configuration.listeningHost(), configuration.listeningPort())
  }

  private val credentialsProviderOption: Option[UsernamePasswordCredentialsProvider] =
    configuration.gitUsername
      .map(new UsernamePasswordCredentialsProvider(_, configuration.gitPassword()))
      .toOption

  def route: Route = {
    respondWithHeader(`Cache-Control`(`max-age`(0))) {
      pathPrefix("api") {
        pathPrefix("git-storages") {
          pathPrefix(Segment) { branch: String =>
            pathSuffix(Segment) { fileName: String =>
              rawPathPrefix((Slash ~ Segment).repeat(0, 128)) { directorySegments: List[String] =>
                logger.debug(s"Access $fileName to $directorySegments on branch $branch")
                val git = !Using(gitPool.acquire())
                val workTreePath = git.getRepository.getWorkTree.toPath
                val parentPath = directorySegments.foldLeft(workTreePath)(_.resolve(_))
                val fullPath = parentPath.resolve(fileName)
                def forceCheckoutBranch() = {
                  val branchRef = R_HEADS + branch
                  val result = git
                    .fetch()
                    .setRemote(configuration.gitUri())
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

                  git
                    .checkout()
                    //.setForced(true) // Disable setForced because jgit will throw a NullPointerException when worktree is clean
                    .setForceRefUpdate(true)
                    .setName(branch)
                    .call()
                }
                get {
                  try {
                    val ref = forceCheckoutBranch()
                    val etag = EntityTag(ref.getObjectId.name)
                    conditional(etag) {
                      mapSettings(_.withFileGetConditional(false)) {
                        getFromFile(fullPath.toFile)
                      }
                    }
                  } catch {
                    case e: TransportException =>
                      logger.info("Failed to call git fetch", e)
                      reject
                  }
                } ~ put {
                  extractRequest { request: HttpRequest =>
                    logger.debug(s"Uploading file to $parentPath on branch $branch")

                    val oldRef = try {
                      forceCheckoutBranch()
                    } catch {
                      case e: TransportException =>
                        logger.info(s"Failed to call git fetch, creating orphan branch $branch...", e)
                        git
                          .checkout()
                          .setName(branch)
                          .setOrphan(true)
                          .setForced(true)
                          .setForceRefUpdate(true)
                          .setAllPaths(true)
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
                          .call()

                        val uploadResults = git
                          .push()
                          .setRemote(configuration.gitUri())
                          .setCredentialsProvider(credentialsProviderOption.orNull)
                          .setRefSpecs(new RefSpec().setSourceDestination(HEAD, R_HEADS + branch))
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

                    oldRef.getObjectId match {
                      case null =>
                        if (request.header[`If-Match`].isDefined) {
                          complete(PreconditionFailed)
                        } else {
                          create()
                        }
                      case oldSha1 =>
                        if (Files.exists(fullPath)) {
                          if (request.header[`If-Match`].isDefined) {
                            conditional(EntityTag(oldSha1.name)) {
                              modify()
                            }
                          } else {
                            complete(PreconditionRequired)
                          }
                        } else {
                          conditional(EntityTag(oldSha1.name)) {
                            create()
                          }
                        }
                    }
                  }
                }
              }
            }
          }
        } ~ createDefaultHealthRoute()
      } ~ sbtWeb
    }
  }
}
