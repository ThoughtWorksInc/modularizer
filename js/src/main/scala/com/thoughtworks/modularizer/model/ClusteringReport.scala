package com.thoughtworks.modularizer.model

import com.thoughtworks.modularizer.model.ClusteringRule.Cluster
import com.thoughtworks.modularizer.util._
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

  def notCluster(node: String) = compoundGraph.children(node).isEmpty

  def notAssignedToCluster(currentNodeId: String) = js.isUndefined(compoundGraph.parent(currentNodeId))

  /*
    1. 如果只被一个cluster直接依赖，那么属于这个cluster
    2. 如果只直接依赖一个cluster，那么属于这个cluster
    3. 如果是不被任何cluster依赖，放入 Source 包
    4. 如果是不依赖任何cluster，放入 Sink 包
    5. 否则不放入任何cluster
   */
  for (currentNodeId <- compoundGraph.nodes()) {
    if (notCluster(currentNodeId) && notAssignedToCluster(currentNodeId)) {
      findSingleNearestCluster(dependentPaths, clusterIds, currentNodeId) match {
        case NearestCluster.One(clusterId) =>
          compoundGraph.setParent(currentNodeId, clusterId)
        case NearestCluster.Zero =>
          findSingleNearestCluster(dependencyPaths, clusterIds, currentNodeId) match {
            case NearestCluster.One(clusterId) =>
              compoundGraph.setParent(currentNodeId, clusterId)
            case NearestCluster.Zero =>
              compoundGraph.setParent(currentNodeId, "source")
            case NearestCluster.Multiple =>
              compoundGraph.setParent(currentNodeId, "source")
          }
        case NearestCluster.Multiple =>
          findSingleNearestCluster(dependencyPaths, clusterIds, currentNodeId) match {
            case NearestCluster.One(clusterId) =>
              compoundGraph.setParent(currentNodeId, clusterId)
            case NearestCluster.Zero =>
              compoundGraph.setParent(currentNodeId, "sink")
            case NearestCluster.Multiple =>
          }
      }
    }
  }
}

object ClusteringReport {
  private val EmptyArray = new js.Array[Edge](0)

  /** Returns the nearest dependent of `currentNodeId`.
    *
    * @example Nearest dependents in a graph of A -> B -> C, A -> C and B -> D,
    *          {{{
    *          import scala.scalajs.js
    *          import typings.graphlibLib.graphlibMod._
    *          import org.scalablytyped.runtime.StringDictionary
    *          import com.thoughtworks.modularizer.model.ClusteringReport._
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
    *          findSingleNearestCluster(paths, js.Array("B", "C", "D"), "A") should be(NearestCluster.Zero)
    *          findSingleNearestCluster(paths, js.Array("A", "C", "D"), "B") should be(NearestCluster.One("A"))
    *          findSingleNearestCluster(paths, js.Array("B", "A", "D"), "C") should be(NearestCluster.One("B"))
    *          findSingleNearestCluster(paths, js.Array("B", "C", "A"), "D") should be(NearestCluster.One("B"))
    *          findSingleNearestCluster(paths, js.Array("C", "B", "A", "D"), "C") should be(NearestCluster.One("C"))
    *          }}}
    *
    */
  private[modularizer] def findSingleNearestCluster(
      paths: StringDictionary[StringDictionary[Path]],
      clusterIds: js.Array[String],
      currentNodeId: String
  ): NearestCluster = {
    @tailrec
    def hasDependency(clusterId: String, i: Int): NearestCluster = {
      if (i < clusterIds.length) {
        val newClusterId = clusterIds(i)
        paths(newClusterId)(currentNodeId).distance match {
          case Double.PositiveInfinity =>
            hasDependency(clusterId, i + 1)
          case _ =>
            if (!paths(clusterId)(newClusterId).distance.isPosInfinity) {
              hasDependency(newClusterId, i + 1)
            } else if (!paths(newClusterId)(clusterId).distance.isPosInfinity) {
              hasDependency(clusterId, i + 1)
            } else {
              NearestCluster.Multiple
            }
        }
      } else {
        NearestCluster.One(clusterId)
      }
    }

    @tailrec
    def noDependencyYet(i: Int): NearestCluster = {
      if (i < clusterIds.length) {
        val clusterId = clusterIds(i)
        paths(clusterId)(currentNodeId).distance match {
          case Double.PositiveInfinity =>
            noDependencyYet(i + 1)
          case _ =>
            hasDependency(clusterId, i + 1)
        }
      } else {
        NearestCluster.Zero
      }
    }

    noDependencyYet(0)
  }

  def findNearestClusters(paths: StringDictionary[StringDictionary[Path]],
                          clusterIds: Seq[String],
                          currentNodeId: String): js.Array[String] = {
    val result = collection.mutable.Set.empty[String]

    def isReachable(from: String, to: String): Boolean = {
      paths.get(from).flatMap(_.get(to)).fold(false)(!_.distance.isInfinity)
    }

    for (clusterId <- clusterIds) {
      if (isReachable(currentNodeId, clusterId)) {
        result.find(isReachable(clusterId, _)) match {
          case Some(existingClusterId) =>
            result -= existingClusterId
            result += clusterId
          case None =>
            if (result.forall(isReachable(_, clusterId))) {
              result += clusterId
            }
        }
      }
    }
    result.toJSArray
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
    *       import com.thoughtworks.modularizer.model.ClusteringReport.calculateDependents
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
    def lookupEdges(currentNodeId: String): js.Array[Edge] = {
      val childrenEdges = childrenAsEdges(graph, currentNodeId)
      val parentEdges = parentAsEdges(graph, currentNodeId)
      val inEdges = graph.inEdges(currentNodeId).getOrElse(EmptyArray)
      childrenEdges.concat(parentEdges, inEdges)
    }

    StringDictionary(clusterIds.map { clusterId: String =>
      clusterId -> algNs.dijkstra(
        graph,
        clusterId,
        Function.const(1.0),
        lookupEdges
      )
    }: _*)
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
    *       import com.thoughtworks.modularizer.model.ClusteringReport.calculateDependencies
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
    def lookupEdges(currentNodeId: String): js.Array[Edge] = {
      val childrenEdges = childrenAsEdges(graph, currentNodeId)
      val parentEdges = parentAsEdges(graph, currentNodeId)
      val outEdges = graph.outEdges(currentNodeId).getOrElse(EmptyArray)
      childrenEdges.concat(parentEdges, outEdges)
    }

    StringDictionary(clusterIds.map { clusterId: String =>
      clusterId -> algNs.dijkstra(
        graph,
        clusterId,
        Function.const(1.0),
        lookupEdges
      )
    }: _*)
  }

  private[modularizer] sealed trait NearestCluster

  private[modularizer] object NearestCluster {

    case object Zero extends NearestCluster

    case class One(clusterId: String) extends NearestCluster

    case object Multiple extends NearestCluster

  }

}
