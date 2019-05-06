package com.thoughtworks.modularizer.js.models

import com.thoughtworks.binding.Binding
import com.thoughtworks.binding.Binding.Var
import com.thoughtworks.modularizer.js.models.ClusteringRule.Cluster
import com.thoughtworks.modularizer.js.utilities._
import org.scalablytyped.runtime.StringDictionary
import typings.graphlibLib.graphlibMod._

import scala.annotation.tailrec
import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.scalajs.js
import scala.scalajs.js.JSConverters._
import org.scalajs.dom.window

/**
  * @author 杨博 (Yang Bo)
  */
final class ClusteringReport(simpleGraph: Graph, rule: ClusteringRule)(implicit executionContext: ExecutionContext) {
  import ClusteringReport._
  import rule._
  val compoundGraph = new Graph(GraphOptions(compound = true, directed = true, multigraph = false))

  compoundGraph.setNodes(simpleGraph.nodes())
  for (edge <- simpleGraph.edges()) {
    compoundGraph.setEdge(edge)
  }

  for ((v, w) <- breakingEdges) {
    compoundGraph.removeEdge(v, w)
  }

  for (Cluster(parent, children) <- clusters) {
    for (child <- children) {
      compoundGraph.setParent(child, parent)
    }
  }

  val clusterIds: js.Array[String] = clusters.view.map(_.parent).toJSArray
  private val priorities = clusterIds.view.zipWithIndex.toMap

  private def notCluster(node: String): Boolean = compoundGraph.children(node).isEmpty

  private def notAssignedToCluster(currentNodeId: String): Boolean = js.isUndefined(compoundGraph.parent(currentNodeId))

  private def findParent(dependentPaths: StringDictionary[StringDictionary[Path]],
                         dependencyPaths: StringDictionary[StringDictionary[Path]],
                         currentNodeId: String): Option[String] = {
    /*
    1. 如果只直接依赖一个cluster，那么属于这个cluster
    2. 如果只被一个cluster直接依赖，那么属于这个cluster
    3. 如果是不被任何cluster依赖，放入 Facades 包
    4. 如果是不依赖任何cluster，放入 Utilities 包
    5. 否则不放入任何cluster
     */
    findNearestClusters(dependentPaths, clusterIds, currentNodeId) match {
      case Seq(clusterId) =>
        findNearestClusters(dependencyPaths, clusterIds, currentNodeId) match {
          case Seq(clusterId2) =>
            Some(js.Array(clusterId, clusterId2).minBy(priorities))
          case _ =>
            Some(clusterId)
        }
      case Seq() =>
        findNearestClusters(dependencyPaths, clusterIds, currentNodeId) match {
          case Seq(clusterId) =>
            Some(clusterId)
          case Seq() =>
            None
          case _ =>
            Some("Utilities")
        }
      case _ =>
        findNearestClusters(dependencyPaths, clusterIds, currentNodeId) match {
          case Seq(clusterId) =>
            Some(clusterId)
          case Seq() =>
            Some("Facades")
          case _ =>
            Some("Conflicts")
        }
    }
  }

  private final class Assignment(nodeIds: js.Array[String]) {
    val dependencyPaths: StringDictionary[StringDictionary[Path]] = calculateDependencies(compoundGraph, clusterIds)
    val dependentPaths: StringDictionary[StringDictionary[Path]] = calculateDependents(compoundGraph, clusterIds)
    val unassigned = new js.Array[String](0)
    for (currentNodeId <- nodeIds) {
      findParent(dependentPaths, dependencyPaths, currentNodeId) match {
        case None =>
          unassigned += currentNodeId
        case Some(parent) =>
          compoundGraph.setParent(currentNodeId, parent)
      }
    }
  }

  private val initialAssignment = new Assignment(compoundGraph.nodes().filter { currentNodeId =>
    notCluster(currentNodeId) && notAssignedToCluster(currentNodeId)
  })

  val unassignedNodes: Var[js.Array[String]] = Var(initialAssignment.unassigned)

  def dependencyPaths: StringDictionary[StringDictionary[Path]] = initialAssignment.dependencyPaths
  def dependentPaths: StringDictionary[StringDictionary[Path]] = initialAssignment.dependentPaths

  // FIXME: 修复 Facades 依赖图绘制逻辑
  // FIXME: 修复 Utilities 依赖图绘制逻辑

  private def nextFrame(): Future[Unit] = {
    val p = Promise[Unit]
    window.requestAnimationFrame { _ =>
      p.success(())
    }
    p.future
  }

  def assignAll(nodeIds: js.Array[String] = initialAssignment.unassigned): Future[Unit] = {
    nextFrame().flatMap { _: Unit =>
      val restNodeIds = new Assignment(nodeIds).unassigned
      if (restNodeIds.length != nodeIds.length) {
        unassignedNodes.value = restNodeIds
        assignAll(restNodeIds)
      } else {
        Future.successful(())
      }
    }
  }

}

object ClusteringReport {
  private val EmptyArray = new js.Array[Edge](0)

  def isReachable(paths: StringDictionary[StringDictionary[Path]], from: String, clusterId: String): Boolean = {
    paths.get(clusterId).fold(false)(_.get(from).fold(false)(!_.distance.isInfinity))
  }

  /** Returns the nearest reachable nodes of `currentNodeId`.
    *
    * @example Find the nearest reachable nodes in the graph of A -> B -> C, A -> C and B -> D,
    *
    *          {{{
    *          import scala.scalajs.js
    *          import typings.graphlibLib.graphlibMod._
    *          import org.scalablytyped.runtime.StringDictionary
    *          import com.thoughtworks.modularizer.js.models.ClusteringReport._
    *          val paths = StringDictionary(
    *            "A" -> StringDictionary(
    *              "A" -> Path(0.0, null),
    *              "C" -> Path(1.0, "A"),
    *              "B" -> Path(1.0, "A"),
    *              "D" -> Path(Double.PositiveInfinity, null),
    *            ),
    *            "B" -> StringDictionary(
    *              "A" -> Path(Double.PositiveInfinity, null),
    *              "B" -> Path(0, null),
    *              "C" -> Path(1.0, "B"),
    *              "D" -> Path(1.0, "B"),
    *            ),
    *            "C" -> StringDictionary(
    *              "A" -> Path(Double.PositiveInfinity, null),
    *              "B" -> Path(Double.PositiveInfinity, null),
    *              "C" -> Path(0, null),
    *              "D" -> Path(Double.PositiveInfinity, null),
    *            ),
    *            "D" -> StringDictionary(
    *              "A" -> Path(Double.PositiveInfinity, null),
    *              "B" -> Path(Double.PositiveInfinity, null),
    *              "C" -> Path(Double.PositiveInfinity, null),
    *              "D" -> Path(0, null),
    *            ),
    *          )
    *
    *          findNearestClusters(paths, js.Array("B", "C", "D"), "A") should be(Seq.empty)
    *          findNearestClusters(paths, js.Array("A", "C", "D"), "B") should be(Seq("A"))
    *          findNearestClusters(paths, js.Array("B", "A", "D"), "C") should be(Seq("B"))
    *          findNearestClusters(paths, js.Array("B", "C", "A"), "D") should be(Seq("B"))
    *          findNearestClusters(paths, js.Array("C", "B", "A", "D"), "C") should be(Seq("C"))
    *          }}}
    *
    */
  def findNearestClusters(paths: StringDictionary[StringDictionary[Path]],
                          clusterIds: Seq[String],
                          currentNodeId: String): Seq[String] = {
    val allReachableClusterIds = clusterIds.filter(isReachable(paths, currentNodeId, _))
    allReachableClusterIds.filterNot { clusterId =>
      allReachableClusterIds.exists { existingClusterId =>
        isReachable(paths, existingClusterId, clusterId) && !isReachable(paths, clusterId, existingClusterId)
      }
    }
  }

  /** Returns dependent paths in the `graph` for `clusterIds`.
    *
    * @note Parents and their children are considered as connection.
    *
    *       {{{
    *       import scala.scalajs.js
    *       import typings.graphlibDashDotLib.graphlibDashDotMod
    *       val graph = graphlibDashDotMod.^.read("""
    *         digraph "your.jar" {
    *             F -> A -> B -> C
    *             A -> X -> Y -> E
    *             G -> X -> C
    *             D -> Y
    *         }
    *       """)
    *       graph.setParent("B", "cluster B")
    *       graph.setParent("F", "cluster F")
    *       graph.setParent("A", "cluster F")
    *       graph.setParent("D", "cluster D")
    *       graph.setParent("G", "cluster G")
    *       graph.setParent("E", "cluster E")
    *
    *       import com.thoughtworks.modularizer.js.models.ClusteringReport.calculateDependents
    *       val allPaths = calculateDependents(graph, js.Array("A", "F", "C"))
    *
    *
    *       allPaths("A")("A").distance should be(0)
    *       allPaths("A")("E").distance should be(Double.PositiveInfinity)
    *       allPaths("A")("F").distance shouldNot be(Double.PositiveInfinity)
    *       allPaths("F")("E").distance should be(Double.PositiveInfinity)
    *       allPaths("F")("A").distance shouldNot be(Double.PositiveInfinity)
    *       allPaths("C")("A").distance shouldNot be(Double.PositiveInfinity)
    *       allPaths("C")("F").distance shouldNot be(Double.PositiveInfinity)
    *       allPaths("F")("G").distance should be(Double.PositiveInfinity)
    *       }}}
    *
    */
  private[modularizer] def calculateDependents(
      graph: Graph,
      clusterIds: js.Array[String]
  ): StringDictionary[StringDictionary[Path]] = {

    StringDictionary(clusterIds.map { clusterId: String =>
      clusterId -> algNs.dijkstra(
        graph,
        clusterId,
        Function.const(1.0),
        lookupDependents(graph, _)
      )
    }: _*)
  }

  def lookupDependents(graph: Graph, currentNodeId: String): js.Array[Edge] = {
    val childrenEdges = childrenAsEdges(graph, currentNodeId)
    val parentEdges = parentAsEdges(graph, currentNodeId)
    val inEdges = graph.inEdges(currentNodeId).getOrElse(EmptyArray)
    childrenEdges.concat(parentEdges, inEdges)
  }
  private def parentAsEdges(graph: Graph, currentNodeId: String): js.Array[Edge] = {
    val nullableParent = graph.parent(currentNodeId)
    if (js.isUndefined(nullableParent)) {
      EmptyArray
    } else {
      js.Array(new Edge {
        var v: String = currentNodeId
        var w: String = nullableParent.asInstanceOf[String]
      })
    }
  }

  private def childrenAsEdges(graph: Graph, currentNodeId: String): js.Array[Edge] = {
    val g1 = graph
      .children(currentNodeId)
      .map { child: String =>
        new Edge {
          var v: String = currentNodeId
          var w: String = child
        }
      }
    g1
  }

  /** Returns dependency paths in the `graph` for `clusterIds`.
    *
    * @note Parents and their children are considered as connection.
    *
    *       {{{
    *       import scala.scalajs.js
    *       import typings.graphlibDashDotLib.graphlibDashDotMod
    *       val graph = graphlibDashDotMod.^.read("""
    *         digraph "your.jar" {
    *             F -> A -> B -> C
    *             A -> X -> Y -> E
    *             G -> X -> C
    *             D -> Y
    *         }
    *       """)
    *       graph.setParent("B", "cluster B")
    *       graph.setParent("F", "cluster F")
    *       graph.setParent("A", "cluster F")
    *       graph.setParent("D", "cluster D")
    *       graph.setParent("G", "cluster G")
    *       graph.setParent("E", "cluster E")
    *
    *       import com.thoughtworks.modularizer.js.models.ClusteringReport.calculateDependencies
    *       val allPaths = calculateDependencies(graph, js.Array("A", "F", "G"))
    *
    *
    *       allPaths("A")("A").distance should be(0)
    *       allPaths("A")("E").distance shouldNot be(Double.PositiveInfinity)
    *       allPaths("A")("F").distance shouldNot be(Double.PositiveInfinity)
    *       allPaths("F")("E").distance shouldNot be(Double.PositiveInfinity)
    *       allPaths("F")("A").distance shouldNot be(Double.PositiveInfinity)
    *       allPaths("G")("A").distance should be(Double.PositiveInfinity)
    *       allPaths("F")("G").distance should be(Double.PositiveInfinity)
    *       }}}
    *
    */
  private[modularizer] def calculateDependencies(
      graph: Graph,
      clusterIds: js.Array[String]
  ): StringDictionary[StringDictionary[Path]] = {

    StringDictionary(clusterIds.map { clusterId: String =>
      clusterId -> algNs.dijkstra(
        graph,
        clusterId,
        Function.const(1.0),
        lookupDependencies(graph, _)
      )
    }: _*)
  }

  def lookupDependencies(graph: Graph, currentNodeId: String): js.Array[Edge] = {
    val childrenEdges = childrenAsEdges(graph, currentNodeId)
    val parentEdges = parentAsEdges(graph, currentNodeId)
    val outEdges = graph.outEdges(currentNodeId).getOrElse(EmptyArray)
    childrenEdges.concat(parentEdges, outEdges)
  }

}
