package com.thoughtworks.modularizer.server

import java.io.File

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.typesafe.scalalogging.StrictLogging

/**
  * @author 杨博 (Yang Bo)
  */
object Main extends StrictLogging {
  def main(arguments: Array[String]): Unit = {
    implicit val system = ActorSystem("modularizer")
    implicit val materializer = ActorMaterializer()
    import system.dispatcher
    val configuration = new Configuration(arguments)
    val workTrees = for (i <- 0 until configuration.numberOfTemporaryGitClones()) yield {
      configuration.temporaryDirectory().resolve(s"work-tree-$i").toFile()
    }
    val gitPool = GitPool(workTrees)
    val server = new Server(configuration, gitPool)

  }

}
