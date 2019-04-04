package com.thoughtworks.modularizer.server

import java.io.{File, IOException}
import java.util.concurrent.ArrayBlockingQueue

import com.typesafe.scalalogging.StrictLogging
import org.eclipse.jgit.api.Git

import scala.concurrent.ExecutionContext

/**
  * @author 杨博 (Yang Bo)
  */
object GitPool extends StrictLogging {

  def apply(workTrees: Iterable[File])(implicit executionContext: ExecutionContext): GitPool = {
    val queue = new ArrayBlockingQueue[Git](workTrees.size)
    executionContext.execute { () =>
      for (workTree <- workTrees) {
        val ordinaryGit = openOrCreate(workTree)
        val autoReleaseGit = new Git(ordinaryGit.getRepository) {
          override def close(): Unit = {
            try {
              clean()
                .setForce(true)
                .setCleanDirectories(true)
                .call()
            } finally {
              queue.offer(this).ensuring(_ == true)
              logger.debug(s"Released git work tree at ${getRepository.getWorkTree}")
            }
          }
        }
        queue.offer(autoReleaseGit).ensuring(_ == true)
      }
    }
    new GitPool(queue)
  }

  private def openOrCreate(workTree: File): Git = {
    logger.info(s"Opening work tree at ${workTree.getAbsoluteFile}")
    try {
      Git.open(workTree)
    } catch {
      case e: IOException =>
        logger.info(s"Cannot open $workTree as a work tree. Calling git init for the directory...", e)
        Git.init().setDirectory(workTree).call()
    }
  }
}

class GitPool private (private val queue: ArrayBlockingQueue[Git]) extends AnyVal {
  def acquire(): Git = {
    queue.take()
  }
}
