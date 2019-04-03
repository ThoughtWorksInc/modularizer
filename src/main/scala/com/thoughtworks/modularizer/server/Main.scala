package com.thoughtworks.modularizer.server

import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{ContentTypes, HttpEntity}
import akka.http.scaladsl.server.Directives._
import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import org.rogach.scallop.ScallopConf

import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.io.StdIn
import io.github.lhotari.akka.http.health.HealthEndpoint._
import com.thoughtworks.akka.http.WebJarsSupport._
import akka.http.scaladsl.server.Route

/**
  * @author 杨博 (Yang Bo)
  */
object Main {

  class Configuration(arguments: Seq[String]) extends ScallopConf(arguments) {
    val host = opt[String](default = Some("localhost"))
    val port = opt[Int](default = Some(42019))
    verify()
  }

  def main(arguments: Array[String]): Unit = {
    val configuration = new Configuration(arguments)

    implicit val system = ActorSystem("my-system")
    implicit val materializer = ActorMaterializer()
    // needed for the future flatMap/onComplete in the end
    implicit val executionContext = system.dispatcher

    val route = {
      pathPrefix("api") {
        createDefaultHealthRoute()
      } ~ sbtWeb
    }

    Http().bindAndHandle(route, configuration.host(), configuration.port())

  }

}
