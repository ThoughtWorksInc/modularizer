package com.thoughtworks.modularizer.js.services

import com.thoughtworks.binding
import com.thoughtworks.binding.bindable._
import com.thoughtworks.binding.Binding
import com.thoughtworks.binding.Binding.{BindingSeq, Constants, SingletonBindingSeq}
import com.thoughtworks.modularizer.js.models._
import org.scalablytyped.runtime.StringDictionary
import typings.graphlibLib.graphlibMod.Graph
import typings.graphlibLib.graphlibMod.Path

import scala.scalajs.js

/**
  * @author 杨博 (Yang Bo)
  */
trait ClusteringService {
  def dependentPaths: Binding[StringDictionary[StringDictionary[Path]]]

  def getParent(nodeId: NodeId): Binding[Option[ClusterId]]

  def isUsedByCluster(dependency: NodeId, dependent: ClusterId): Binding[Boolean]
  def dependsOnCluster(dependent: NodeId, dependency: ClusterId): Binding[Boolean]

  def children(clusterId: ClusterId): BindingSeq[NodeId]

  def unassignedNodeIds: BindingSeq[NodeId]

  /** Returns a [[Binding]] whose value is
    * either [[Some]] of the underlying [[typings.graphlibLib.graphlibMod.Graph Graph]] of the clustering result,
    * or [[None]] if there is a pending calculation of clustering.
    */
  def underlyingCompoundGraph: Binding[Option[Graph]]

}

object ClusteringService {

  /** The new [[ClusteringService]] based on [[CompoundGraph]]
    */
  final class NewClusteringService(compoundGraph: Binding[CompoundGraph]) extends ClusteringService {
    def dependentPaths: Binding[StringDictionary[StringDictionary[Path]]] = ???

    def getParent(nodeId: NodeId): Binding[Option[ClusterId]] = ???

    def isUsedByCluster(dependency: NodeId, dependent: ClusterId): Binding[Boolean] = ???

    def dependsOnCluster(dependent: NodeId, dependency: ClusterId): Binding[Boolean] = ???

    def children(clusterId: ClusterId): BindingSeq[NodeId] = ???

    def unassignedNodeIds: BindingSeq[NodeId] = ???

    def underlyingCompoundGraph: Binding[Option[Graph]] = ???
  }

  @deprecated("Use NewClusteringService instead")
  final class LegacyClusteringService(clusteringReport: Binding[ClusteringReport]) extends ClusteringService {
    def getParent(nodeId: NodeId): Binding[Option[ClusterId]] = Binding {
      clusteringReport.bind.compoundGraph.parent(nodeId) match {
        case clusterId if js.isUndefined(clusterId) =>
          None
        case clusterId =>
          Some(ClusterId(clusterId.toString))
      }
    }

    def isUsedByCluster(dependency: NodeId, dependent: ClusterId): Binding[Boolean] = Binding {
      ClusteringReport.isReachable(clusteringReport.bind.dependencyPaths, dependency, dependent)
    }

    def dependsOnCluster(dependent: NodeId, dependency: ClusterId): Binding[Boolean] = Binding {
      ClusteringReport.isReachable(clusteringReport.bind.dependentPaths, dependency, dependent)
    }

    def children(clusterId: ClusterId): BindingSeq[NodeId] = clusteringReport.bindSeq.flatMap {
      report: ClusteringReport =>
        val _ = report.unassignedNodes.bind

        js.UndefOr
          .any2undefOrA(NodeIdArray(report.compoundGraph.children(clusterId)))
          .getOrElse(js.Array())
          .bindSeq
    }

    def underlyingCompoundGraph: Binding[Option[Graph]] = Binding {
      Some(clusteringReport.bind.compoundGraph)
    }

    def unassignedNodeIds: BindingSeq[NodeId] = clusteringReport.bindSeq.flatMap { report: ClusteringReport =>
      NodeIdArray(report.unassignedNodes.bind).bindSeq
    }

    def dependentPaths: Binding[StringDictionary[StringDictionary[Path]]] = Binding {
      clusteringReport.bind.dependentPaths
    }
  }

}
