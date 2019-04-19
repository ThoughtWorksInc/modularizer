package com.thoughtworks.modularizer.models

import com.thoughtworks.modularizer.models.ClusteringRule.Cluster
import com.thoughtworks.modularizer.utilities._
import org.scalablytyped.runtime.StringDictionary
import typings.graphlibLib.graphlibMod._

import scala.annotation.tailrec
import scala.scalajs.js
import scala.scalajs.js.JSConverters._

/**
  * @author 杨博 (Yang Bo)
  */
final class ClusteringReport(simpleGraph: Graph, rule: ClusteringRule) {
  import ClusteringReport._
  import rule._
  val compoundGraph = new Graph(new GraphOptions {
    compound = true
    directed = true
    multigraph = false
  })

  compoundGraph.setNodes(simpleGraph.nodes())
  for (edge <- simpleGraph.edges()) {
    if (!breakingEdges(edge.v -> edge.w)) {
      compoundGraph.setEdge(edge)
    }
  }

  // TODO: 确保所有 cluster 之间不能互相依赖
  for (Cluster(parent, children) <- clusters) {
    for (child <- children) {
      compoundGraph.setParent(child, parent)
    }
  }
  val clusterIds = clusters.view.map(_.parent).toJSArray

  val dependencyPaths = calculateDependencies(compoundGraph, clusterIds)
  val dependentPaths = calculateDependents(compoundGraph, clusterIds)

  private def notCluster(node: String) = compoundGraph.children(node).isEmpty

  private def notAssignedToCluster(currentNodeId: String) = js.isUndefined(compoundGraph.parent(currentNodeId))

  {
    val priorities = clusterIds.view.zipWithIndex.toMap
    /*
    1. 如果只直接依赖一个cluster，那么属于这个cluster
    2. 如果只被一个cluster直接依赖，那么属于这个cluster
    3. 如果是不被任何cluster依赖，放入 Source 包
    4. 如果是不依赖任何cluster，放入 Sink 包
    5. 否则不放入任何cluster
     */
    for (currentNodeId <- compoundGraph.nodes()) {
      if (notCluster(currentNodeId) && notAssignedToCluster(currentNodeId)) {
        findNearestClusters(dependentPaths, clusterIds, currentNodeId) match {
          case Seq(clusterId) =>
            findNearestClusters(dependencyPaths, clusterIds, currentNodeId) match {
              case Seq(clusterId2) =>
                compoundGraph.setParent(currentNodeId, js.Array(clusterId, clusterId2).minBy(priorities))
              case _ =>
                compoundGraph.setParent(currentNodeId, clusterId)
            }
          case Seq() =>
            findNearestClusters(dependencyPaths, clusterIds, currentNodeId) match {
              case Seq(clusterId) =>
                compoundGraph.setParent(currentNodeId, clusterId)
              case Seq() =>
                compoundGraph.setParent(currentNodeId, "Facades")
              case _ =>
                compoundGraph.setParent(currentNodeId, "Utilities")
            }
          case _ =>
            findNearestClusters(dependencyPaths, clusterIds, currentNodeId) match {
              case Seq(clusterId) =>
                compoundGraph.setParent(currentNodeId, clusterId)
              case Seq() =>
                compoundGraph.setParent(currentNodeId, "Facades")
              case _ =>
            }
        }
      }
    }
  }
  dependencyPaths("Facades") =
    algNs.dijkstra(compoundGraph, "Facades", Function.const(1.0), lookupDependencies(compoundGraph, _))

  dependentPaths("Utilities") =
    algNs.dijkstra(compoundGraph, "Utilities", Function.const(1.0), lookupDependents(compoundGraph, _))

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
    *          import com.thoughtworks.modularizer.models.ClusteringReport._
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
    *       import com.thoughtworks.modularizer.models.ClusteringReport.calculateDependents
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
    *       import com.thoughtworks.modularizer.models.ClusteringReport.calculateDependencies
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
