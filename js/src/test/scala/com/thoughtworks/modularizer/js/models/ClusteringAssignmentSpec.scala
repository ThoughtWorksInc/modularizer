package com.thoughtworks.modularizer.js.models

import com.thoughtworks.modularizer.js.TestBuildInfo.dependencyGraph
import org.scalatest.{FreeSpec, Matchers}
import typings.graphlibDashDotLib.graphlibDashDotMod
import typings.graphlibLib.graphlibMod.{Graph, GraphOptions}

import scala.concurrent.Future
import scala.scalajs.js

/**
  * @author 杨博 (Yang Bo)
  */
class ClusteringAssignmentSpec extends FreeSpec with Matchers {
//  "clusterIds should return all parents in the compound graph" in {
//    val compoundGraph = new Graph(GraphOptions(compound = true, directed = true, multigraph = false))
//    compoundGraph.setParent("child1", "parent1")
//    compoundGraph.setParent("child2", "parent2")
//    val assigner = new ClusterAssignment(compoundGraph)
//    new js.WrappedArray(assigner.clusterIds) should contain only ("parent1", "parent2")
//  }
//
//  "xx" in {
////    val compoundGraph = new Graph(GraphOptions(compound = true, directed = true, multigraph = false))
////    compoundGraph.setParent("child1", "parent1")
////    compoundGraph.setEdge("child1", "child2")
////    compoundGraph.setEdge("child3", "child2")
////    compoundGraph.setEdge("child3", "child4")
////    compoundGraph.setParent("child4", "parent2")
////    val assigner = new ClusterAssignment(compoundGraph)
////    new js.WrappedArray(assigner.clusterIds) should contain only ("parent1", "parent2")
////
////    assigner.
//
//  }

}

object FutureList {
  type FutureList[+A] = Future[NextValue[A]]

  trait NextValue[+A]
  case object Empty extends NextValue[Nothing]
  case class NonEmpty[A](head: A, tail: FutureList[A]) extends NextValue[A]
}
