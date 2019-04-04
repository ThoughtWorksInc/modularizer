package com.thoughtworks.modularizer.server
import java.nio.file.Files

import akka.Done
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.RequestEntity
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{Route, StandardRoute}
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.FileIO
import com.thoughtworks.akka.http.WebJarsSupport._
import com.thoughtworks.dsl.Dsl
import com.thoughtworks.dsl.keywords.{Await, Using}
import com.typesafe.scalalogging.Logger
import io.github.lhotari.akka.http.health.HealthEndpoint._
import org.eclipse.jgit.api.ResetCommand.ResetType
import org.eclipse.jgit.api.errors.TransportException
import org.eclipse.jgit.lib.Constants._
import org.eclipse.jgit.revwalk.RevWalk
import org.eclipse.jgit.transport.RemoteRefUpdate.Status._
import org.eclipse.jgit.transport.{RefSpec, RemoteRefUpdate, UsernamePasswordCredentialsProvider}

import scala.collection.JavaConverters._

/**
  * @author 杨博 (Yang Bo)
  */
private object Server {
  val logger = Logger[Server]
}
import com.thoughtworks.modularizer.server.Server.logger
class Server(configuration: Configuration, gitPool: GitPool)(implicit system: ActorSystem,
                                                             materializer: ActorMaterializer) {
  import system.dispatcher

  implicit def standardRouteDsl[Keyword, Value](
      implicit routeDsl: Dsl[Keyword, Route, Value]): Dsl[Keyword, StandardRoute, Value] =
    (keyword: Keyword, handler: Value => StandardRoute) => {
      StandardRoute(routeDsl.cpsApply(keyword, handler))
    }

  val serverBinding = {
    Http().bindAndHandle(route, configuration.listeningHost(), configuration.listeningPort())
  }
  private val credentialsProviderOption: Option[UsernamePasswordCredentialsProvider] =
    configuration.gitUsername
      .map(new UsernamePasswordCredentialsProvider(_, configuration.gitPassword()))
      .toOption

  def route = {
    pathPrefix("api") {
      pathPrefix("git-storages") {
        pathPrefix(Segment) { branch: String =>
          pathSuffix(Segment) { fileName: String =>
            path(Segments ~ Slash) { directorySegments: List[String] =>
              logger.debug(s"Access $fileName to $directorySegments on branch $branch")
              val git = !Using(gitPool.acquire())
              val directory = directorySegments.foldLeft(git.getRepository.getWorkTree.toPath)(_.resolve(_))
              val fullPath = directory.resolve(fileName)
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
                  forceCheckoutBranch()
                  getFromFile(fullPath.toFile)
                } catch {
                  case e: TransportException =>
                    logger.info("Failed to call git fetch", e)
                    reject
                }
              } ~ put {
                def retry(numberOfRetries: Int): Route = {
                  if (numberOfRetries >= configuration.maxRetriesForUploading()) {
                    reject
                  } else {
                    logger.debug(s"Uploading file to $directory on branch $branch")
                    extractRequestEntity { entity: RequestEntity =>
                      try {
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
                            .call()

                          git
                            .reset()
                            .setMode(ResetType.HARD)
                            .call()
                      }

                      val requestBody = entity.dataBytes

                      logger.debug(s"Saving request body to file $fullPath")
                      Files.createDirectories(directory)
                      val ioResult = !Await(requestBody.runWith(FileIO.toPath(fullPath)))
                      val _ = ioResult.status.get

                      git
                        .add()
                        .addFilepattern(".")
                        .call()

                      git
                        .commit()
                        .setMessage(s"Update $directory on branch $branch")
                        .setAuthor("Modularizer", "atryyang@thoughtworks.com")
                        .call()

                      val pushResults = git
                        .push()
                        .setRemote(configuration.gitUri())
                        .setCredentialsProvider(credentialsProviderOption.orNull)
                        .setRefSpecs(new RefSpec().setSourceDestination(HEAD, R_HEADS + branch))
                        .call()

                      if (pushResults.asScala.exists(_.getRemoteUpdates.asScala.exists(_.getStatus != OK))) {
                        retry(numberOfRetries + 1)
                      } else {
                        complete(Done)
                      }
                    }
                  }
                }
                retry(0)
              }
            }
          }
        }
      } ~ createDefaultHealthRoute()
    } ~ sbtWeb
  }
}
