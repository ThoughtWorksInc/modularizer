package com.thoughtworks.modularizer.server

import caseapp._
import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.typesafe.scalalogging.StrictLogging
import Configuration.pathArgParser

/**
  * @author 杨博 (Yang Bo)
  */
object Main extends CaseApp[Configuration] with StrictLogging {
  def run(configuration: Configuration, remainingArgs: RemainingArgs): Unit = {
    implicit val system = ActorSystem("modularizer")
    implicit val materializer = ActorMaterializer()
    import system.dispatcher
    val workTrees = for (i <- 0 until configuration.numberOfTemporaryGitClones) yield {
      configuration.temporaryDirectory.resolve(s"work-tree-$i").toFile()
    }
    val gitPool = GitPool(workTrees)
    val server = new Server(configuration, gitPool)

  }

}
