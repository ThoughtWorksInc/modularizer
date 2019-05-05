//package com.thoughtworks.modularizer.js.models
//
//import com.thoughtworks.binding.Binding.Vars
//import com.thoughtworks.dsl.Dsl.reset
//import com.thoughtworks.dsl.keywords.{Each, Shift}
//import com.thoughtworks.modularizer.js.models.CompoundGraph.{ClusterAssignment, ClusterId, NodeId}
//import org.scalajs.dom.window
//
//import scala.concurrent.{ExecutionContext, Future, Promise}
//import scala.scalajs.js
//
///**
//  * @author 杨博 (Yang Bo)
//  */
//class ClusteringReport2(compoundGraph: CompoundGraph)(implicit executionContext: ExecutionContext) { // TODO: Rename to ClusteringReport
//  import ClusteringReport2._
//  private val initialUnassignedNodeIds: js.Array[NodeId] = compoundGraph.unassignedNodeIds
//  val unassignedNodeIds = Vars(compoundGraph.unassignedNodeIds: _*)
//  val assignedNodeIdsByCluster: Map[ClusterId, Vars[String]] =
//    compoundGraph.clusterIds.map(_ -> Vars.empty[String]).toMap
//
//  def startAssign(): Unit = {
//    val assignments = compoundGraph.assignAll(initialUnassignedNodeIds)
//    val assignmentGroup = !Each(new Iterable[Stream[ClusterAssignment]] {
//      def iterator: Iterator[Stream[ClusterAssignment]] = assignments.grouped(MaxNodeIdAssignedPerFrame)
//    })
//    !Shift[Unit, Double](window.requestAnimationFrame(_))
//
//    unassignedNodeIds.value --= assignmentGroup.map(_.childId)
//    val (clusterId, assignmentByCluster) = !Each(assignmentGroup.groupBy(_.parentId))
//    assignedNodeIdsByCluster(clusterId).value ++= assignmentByCluster.map(_.childId)
//  }: @reset
//
//}
//
//object ClusteringReport2 {
//
//  private final val MaxNodeIdAssignedPerFrame = 1000
//
//}
