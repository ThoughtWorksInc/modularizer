package com.thoughtworks.modularizer.js.services

import com.thoughtworks.binding.{Binding, FutureBinding}
import com.thoughtworks.binding.Binding.BindingInstances.monadSyntax._
import com.thoughtworks.binding.Binding.{BindingSeq, Constants, Var, Vars}
import com.thoughtworks.binding.bindable._
import com.thoughtworks.dsl.domains.task._
import com.thoughtworks.dsl.keywords.Shift
import com.thoughtworks.modularizer.js.models._
import org.scalablytyped.runtime.StringDictionary
import org.scalajs.dom.window
import typings.graphlibLib.graphlibMod.{Graph, Path}

import scala.concurrent.{ExecutionContext, Future}
import scala.scalajs.js

/**
  * @author 杨博 (Yang Bo)
  */
trait ClusteringService {
  def initialDependentPaths: Binding[Option[Map[CustomClusterId, StringDictionary[Path]]]]

  def parent(nodeId: NodeId): Binding[Option[ClusterId]]

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

  class ClusteringAnimation(val compoundGraph: CompoundGraph)(implicit executionContext: ExecutionContext) { // TODO: Rename to ClusteringReport
    import ClusteringAnimation._
    val initialDependentPaths: Var[Option[Map[CustomClusterId, StringDictionary[Path]]]] = Var(None)

    val dependencyPaths: Var[Option[Map[CustomClusterId, StringDictionary[Path]]]] = Var(None)
    val dependentPaths: Var[Option[Map[CustomClusterId, StringDictionary[Path]]]] = Var(None)
    val unassignedNodeIds = Vars(compoundGraph.unassignedNodeIds: _*)
    val assignedNodeIdsByCluster: Map[ClusterId, Vars[NodeId]] =
      compoundGraph.clusterIds.map(_ -> Vars.empty[NodeId]).toMap
    private val initialUnassignedNodeIds: js.Array[NodeId] = compoundGraph.unassignedNodeIds
    val assigning: Future[Unit] = {
      Task.toFuture(Task {
        val assignments = compoundGraph.assignAll(initialUnassignedNodeIds)

        initialDependentPaths.value = assignments.headOption.map(_.dependentPaths)

        val i = assignments.grouped(MaxNumberOfNodeIdsAssigningPerFrame)
        while (i.hasNext) {
          val currentFrame = i.next()

          !Shift[Unit, Double](window.requestAnimationFrame(_))

          dependencyPaths.value = Some(currentFrame.last.dependencyPaths)
          dependentPaths.value = Some(currentFrame.last.dependentPaths)
          unassignedNodeIds.value --= currentFrame.map(_.childId)
          val j = currentFrame.groupBy(_.parentId).iterator
          while (j.hasNext) {
            val (clusterId, assignmentByCluster) = j.next()
            assignedNodeIdsByCluster(clusterId).value ++= assignmentByCluster.map(_.childId)
          }
        }
      })
    }
  }

  /** The new [[ClusteringService]] based on [[CompoundGraph]] */
  final class NewClusteringService(clusteringAnimation: Binding[ClusteringAnimation])(
      implicit executionContext: ExecutionContext)
      extends ClusteringService {

    def parent(nodeId: NodeId): Binding[Option[ClusterId]] = Binding {
      val animation = clusteringAnimation.bind
      val _ = animation.unassignedNodeIds.all.bind
      animation.compoundGraph.underlying.parent(nodeId) match {
        case parent if js.isUndefined(parent) =>
          None
        case parent =>
          Some(ClusterId(parent.toString))
      }
    }

    def isUsedByCluster(dependency: NodeId, dependent: ClusterId): Binding[Boolean] = Binding {
      clusteringAnimation.bind.dependentPaths.bind match {
        case None =>
          false
        case Some(dependentPaths) =>
          dependent match {
            case BuiltInClusterId.Utility | BuiltInClusterId.Facade | BuiltInClusterId.Conflict |
                BuiltInClusterId.Isolated =>
              false // TODO: 细分内置组
            case customClusterId =>
              dependentPaths.get(CustomClusterId(customClusterId)) match {
                case None =>
                  false
                case Some(clusterPaths) =>
                  val path: Path = clusterPaths(dependency)
                  !path.distance.isInfinite
              }
          }
      }
    }

    def dependsOnCluster(dependent: NodeId, dependency: ClusterId): Binding[Boolean] = Binding {
      clusteringAnimation.bind.dependencyPaths.bind match {
        case None =>
          false
        case Some(dependencyPaths) =>
          dependent match {
            case BuiltInClusterId.Utility | BuiltInClusterId.Facade | BuiltInClusterId.Conflict |
                BuiltInClusterId.Isolated =>
              false // TODO: 细分内置组
            case customClusterId =>
              dependencyPaths.get(CustomClusterId(customClusterId)) match {
                case None =>
                  false
                case Some(clusterPaths) =>
                  val path: Path = clusterPaths(dependency)
                  !path.distance.isInfinite
              }
          }
      }
    }

    def initialDependentPaths: Binding[Option[Map[CustomClusterId, StringDictionary[Path]]]] = Binding {
      clusteringAnimation.bind.initialDependentPaths.bind
    }

    def children(clusterId: ClusterId): BindingSeq[NodeId] =
      Binding {
        clusteringAnimation.bind.assignedNodeIdsByCluster.getOrElse(clusterId, Constants.empty)
      }.toBindingSeq

    def unassignedNodeIds: BindingSeq[NodeId] =
      Binding {
        clusteringAnimation.bind.unassignedNodeIds
      }.toBindingSeq

    def underlyingCompoundGraph: Binding[Option[Graph]] = Binding {
      val animation = clusteringAnimation.bind
      FutureBinding(animation.assigning).bind match {
        case None =>
          None
        case _ =>
          Some(animation.compoundGraph.underlying)
      }
    }
  }

  @deprecated("Use NewClusteringService instead")
  final class LegacyClusteringService(clusteringReport: Binding[ClusteringReport]) extends ClusteringService {
    def parent(nodeId: NodeId): Binding[Option[ClusterId]] = Binding {
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

    def initialDependentPaths: Binding[Option[Map[CustomClusterId, StringDictionary[Path]]]] = Binding {
      val map: Map[String, StringDictionary[Path]] = clusteringReport.bind.dependentPaths.toMap
      Some(map.asInstanceOf[Map[CustomClusterId, StringDictionary[Path]]])
    }
  }

  object ClusteringAnimation {

    final val MaxNumberOfNodeIdsAssigningPerFrame = 1000

  }

  object NewClusteringService {
    def apply(compoundGraph: Binding[CompoundGraph])(implicit executionContext: ExecutionContext) =
      new NewClusteringService(compoundGraph.map(new ClusteringAnimation(_)))
  }

}
