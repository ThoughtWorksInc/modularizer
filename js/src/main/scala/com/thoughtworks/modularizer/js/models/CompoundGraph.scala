package com.thoughtworks.modularizer.js.models
import com.thoughtworks.modularizer.js.utilities._
import com.thoughtworks.modularizer.js.models.ClusteringRule.Cluster
import org.scalablytyped.runtime.StringDictionary
import typings.graphlibLib.graphlibMod.{Edge, Graph, GraphOptions, Path, algNs}

import scala.scalajs.js

/**
  * @author 杨博 (Yang Bo)
  */
final class CompoundGraph(val underlying: Graph, val clusterIds: IndexedSeq[CustomClusterId]) {
  import CompoundGraph._
  private def notCluster(nodeId: String): Boolean = underlying.children(nodeId).isEmpty
  private def notAssignedToCluster(nodeId: String): Boolean = js.isUndefined(underlying.parent(nodeId))

  def unassignedNodeIds: js.Array[NodeId] = underlying.nodes().collect {
    case nodeId if notCluster(nodeId) && notAssignedToCluster(nodeId) =>
      NodeId(nodeId)
  }

  def assignAll(nodeIds: js.Array[NodeId]): Stream[ClusterAssignment] = {
    def loop(
        clusterIndex: Int,
        nodeIds: js.Array[NodeId],
        dependencyPaths: Map[CustomClusterId, StringDictionary[Path]],
        dependentPaths: Map[CustomClusterId, StringDictionary[Path]],
    ): Stream[ClusterAssignment] = {
      if (clusterIndex < clusterIds.length) {
        val clusterId = clusterIds(clusterIndex)

        def innerLoop(nodeIndex: Int,
                      numberOfAssignedNode: Int,
                      restNodeIds: js.Array[NodeId]): Stream[ClusterAssignment] = {
          if (nodeIndex < nodeIds.length) {
            val nodeId = nodeIds(nodeIndex)
            def assign() = {
              underlying.setParent(nodeId, clusterId)
              ClusterAssignment(nodeId, clusterId, dependencyPaths, dependentPaths) #:: {
                innerLoop(nodeIndex + 1, numberOfAssignedNode + 1, restNodeIds)
              }
            }
            findNearestClusters(dependentPaths, clusterIds, nodeId) match {
              case Seq(`clusterId`) =>
                assign()
              case _ =>
                findNearestClusters(dependencyPaths, clusterIds, nodeId) match {
                  case Seq(`clusterId`) =>
                    assign()
                  case _ =>
                    innerLoop(nodeIndex + 1, numberOfAssignedNode, (restNodeIds += nodeId).result())
                }
            }
          } else {
            if (numberOfAssignedNode == 0) {
              loop(
                clusterIndex + 1,
                restNodeIds,
                dependencyPaths,
                dependentPaths,
              )
            } else {
              loop(
                clusterIndex + 1,
                restNodeIds,
                calculateDependencies(underlying, clusterIds),
                calculateDependents(underlying, clusterIds),
              )
            }
          }
        }
        innerLoop(0, 0, new js.Array[NodeId](0))
      } else {
        Stream.Empty
      }
    }

    loop(
      0,
      nodeIds,
      calculateDependencies(underlying, clusterIds),
      calculateDependents(underlying, clusterIds),
    )
  }
}

object CompoundGraph {
  private val EmptyArray = new js.Array[Edge](0)

  def lookupDependencies(graph: Graph, currentNodeId: String): js.Array[Edge] = {
    val childrenEdges = childrenAsEdges(graph, currentNodeId)
    val parentEdges = parentAsEdges(graph, currentNodeId)
    val outEdges = graph.outEdges(currentNodeId).getOrElse(EmptyArray)
    childrenEdges.concat(parentEdges, outEdges)
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
  // TODO: Port
  private[modularizer] def calculateDependencies(
      graph: Graph,
      clusterIds: Seq[CustomClusterId]): Map[CustomClusterId, StringDictionary[Path]] = {
    clusterIds.map { clusterId: CustomClusterId =>
      clusterId -> algNs.dijkstra(
        graph,
        clusterId,
        Function.const(1.0),
        lookupDependencies(graph, _)
      )
    }.toMap
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
      clusterIds: Seq[CustomClusterId]): Map[CustomClusterId, StringDictionary[Path]] = {

    clusterIds.map { clusterId: CustomClusterId =>
      clusterId -> algNs.dijkstra(
        graph,
        clusterId,
        Function.const(1.0),
        lookupDependents(graph, _)
      )
    }.toMap
  }

  private def isReachable(paths: Map[CustomClusterId, StringDictionary[Path]],
                          from: String,
                          clusterId: CustomClusterId): Boolean = {
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
  def findNearestClusters(paths: Map[CustomClusterId, StringDictionary[Path]],
                          clusterIds: Seq[CustomClusterId],
                          currentNodeId: String): Seq[CustomClusterId] = {
    val allReachableClusterIds = clusterIds.filter(isReachable(paths, currentNodeId, _))
    allReachableClusterIds.filterNot { clusterId =>
      allReachableClusterIds.exists { existingClusterId =>
        isReachable(paths, existingClusterId, clusterId) && !isReachable(paths, clusterId, existingClusterId)
      }
    }
  }
  def apply(simpleGraph: Graph, rule: ClusteringRule): CompoundGraph = {
    val underlying = new Graph(GraphOptions(compound = true, directed = true, multigraph = false))

    underlying.setNodes(simpleGraph.nodes())
    for (edge <- simpleGraph.edges()) {
      underlying.setEdge(edge)
    }

    for ((v, w) <- rule.breakingEdges) {
      underlying.removeEdge(v, w)
    }

    for (Cluster(parent, children) <- rule.clusters) {
      for (child <- children) {
        underlying.setParent(child, parent)
      }
    }
    new CompoundGraph(underlying, rule.clusters.view.map { cluster =>
      CustomClusterId(cluster.parent)
    }.toIndexedSeq)
  }

  final case class ClusterAssignment(childId: NodeId,
                                     parentId: ClusterId,
                                     dependencyPaths: Map[CustomClusterId, StringDictionary[Path]],
                                     dependentPaths: Map[CustomClusterId, StringDictionary[Path]])

}
