package com.thoughtworks.modularizer.views.workboard
import com.thoughtworks.binding.Binding.{BindingSeq, Constants, Var, Vars}
import com.thoughtworks.binding.{Binding, LatestEvent, dom}
import com.thoughtworks.modularizer.models.{ClusteringReport, ClusteringRule, DraftCluster}
import DraftCluster._
import com.thoughtworks.modularizer.utilities._
import org.scalajs.dom.raw.{Event, HTMLLIElement, Node, UIEvent}
import typings.graphlibLib.graphlibMod.Graph

import scala.collection.immutable
import scala.scalajs.js

/**
  * @author 杨博 (Yang Bo)
  */
object DependencyExplorer {

  @dom
  def dependencyList(graph: Graph, items: js.Array[String]) = {
    <div class="list-group">
      <a href="#" class="list-group-item list-group-item-action active">
        Cras justo odio
      </a>
      <a href="#" class="list-group-item list-group-item-action">Dapibus ac facilisis in</a>
      <a href="#" class="list-group-item list-group-item-action">Morbi leo risus</a>
      <a href="#" class="list-group-item list-group-item-action">Porta ac consectetur ac</a>
      <a href="#" class="list-group-item list-group-item-action disabled">Vestibulum at eros</a>
    </div>

  }

  @dom
  def neighborList(graph: Graph,
                   clusteringReport: Binding[ClusteringReport],
                   nodeId: String,
                   draftClusters: Vars[DraftCluster]): Binding[BindingSeq[Node]] = {
    <div class="d-flex flex-row align-items-baseline">
      <span title={ nodeId } style:direction="rtl" class="mr-auto flex-shrink-1 text-right text-truncate">{
        nodeId
      }</span>
      {
        if (draftClusters.isEmpty.bind) {
          Constants()
        } else {
          val currentClusterSeq = for {
            draftCluster <- draftClusters
            clusterNodeId <- draftCluster.nodeIds
            if clusterNodeId == nodeId
          } yield draftCluster
          currentClusterSeq.length.bind match {
            case 0 =>
              val clusterColor = Binding {
                (clusteringReport.bind.compoundGraph.parent(nodeId): Any) match {
                  case () =>
                    UnassignedColorClass
                  case "Facades" =>
                    FacadeColorClass
                  case "Utilities" =>
                    UtilityColorClass
                  case customCluster: String =>
                    draftClusters.flatMap { draftCluster =>
                      if (draftCluster.name.bind == customCluster) {
                        Constants(draftCluster.color.bind)
                      } else {
                        Constants.empty
                      }
                    }.all.bind.headOption.getOrElse(UnassignedColorClass)
                }
              }
              Constants(
                <div class="dropdown">
                  <button
                    type="button"
                    data:data-toggle="dropdown"
                    class="badge badge-secondary dropdown-toggle"
                    style:backgroundColor={clusterColor.bind.backgroundColor}
                    style:color={clusterColor.bind.textColor}
                  >
                    <span class="fas fa-unlock"></span>
                    {
                      clusteringReport.bind.compoundGraph.parent(nodeId).fold("Unassigned") {
                        case "Facades" => "Facades"
                        case "Utilities" => "Utilities"
                        case customCluster => customCluster
                      }
                    }
                  </button>
                  <div class="dropdown-menu">{
                    for (draftCluster <- draftClusters) yield {
                      <button
                        class="dropdown-item"
                        type="button"
                        onclick={ _: Event =>
                          draftCluster.nodeIds.value += nodeId
                        }
                      >
                        Assign to
                        <span
                          class="badge badge-secondary"
                          style:backgroundColor={draftCluster.color.bind.backgroundColor}
                          style:color={draftCluster.color.bind.textColor}>{
                          draftCluster.name.bind
                        }</span>
                      </button>
                    }
                  }</div>
                </div>
              )
            case _ =>
              for (currentCluster <- currentClusterSeq) yield <div class="dropdown">
                <button
                  type="button"
                  data:data-toggle="dropdown"
                  class="badge badge-secondary dropdown-toggle"
                  style:backgroundColor={currentCluster.color.bind.backgroundColor} style:color={currentCluster.color.bind.textColor}
                >
                  <span class="fas fa-lock"></span>
                  { currentCluster.name.bind }
                </button>

                <div class="dropdown-menu">{
                  for (draftCluster <- draftClusters) yield {
                    if (draftCluster eq currentCluster) {
                      <button
                        class="dropdown-item"
                        type="button"
                        onclick={ _: Event =>
                          currentCluster.nodeIds.value -= nodeId
                        }
                      >
                        Unassign from
                        <span class="badge badge-secondary" style:backgroundColor={draftCluster.color.bind.backgroundColor} style:color={draftCluster.color.bind.textColor}>{
                          draftCluster.name.bind
                        }</span>
                      </button>
                    } else {
                      <button
                        class="dropdown-item"
                        type="button"
                        onclick={ _: Event =>
                          currentCluster.nodeIds.value -= nodeId
                          draftCluster.nodeIds.value += nodeId
                        }
                      >
                        Assign to
                        <span class="badge badge-secondary" style:backgroundColor={draftCluster.color.bind.backgroundColor} style:color={draftCluster.color.bind.textColor}>{
                          draftCluster.name.bind
                        }</span>
                      </button>
                    }
                  }
                }</div>
              </div>
          }
        }
      }
    </div>
    <div class="pl-2 border-left">
      { dependencyList(graph, clusteringReport, nodeId, draftClusters).bind }
      { dependentList(graph, clusteringReport, nodeId, draftClusters).bind }
    </div>
  }

  @dom
  def dependentList(graph: Graph,
                    clusteringReport: Binding[ClusteringReport],
                    nodeId: String,
                    draftClusters: Vars[DraftCluster]): Binding[Node] = {
    val dependents = for {
      edge <- graph.inEdges(nodeId).getOrElse(js.Array())
      if edge.v != edge.w
    } yield edge
    if (dependents.isEmpty) {
      <!-- No dependencies -->
    } else {
      <details id="nodeDetails">
        <summary>Dependents</summary>
        {
          if ({
            val _ = new LatestEvent[Event](nodeDetails, "toggle").bind
            nodeDetails.asInstanceOf[js.Dynamic].open.asInstanceOf[Boolean]
          }) {
            Constants(dependents: _*).flatMapBinding { edge =>
              neighborList(graph, clusteringReport, edge.v, draftClusters)
            }
          } else {
            Constants()
          }
        }
      </details>
    }
  }

  @dom
  def dependencyList(graph: Graph,
                     clusteringReport: Binding[ClusteringReport],
                     nodeId: String,
                     draftClusters: Vars[DraftCluster]): Binding[Node] = {
    val dependencies = for {
      edge <- graph.outEdges(nodeId).getOrElse(js.Array())
      if edge.v != edge.w
    } yield edge
    if (dependencies.isEmpty) {
      <!-- No dependencies -->
    } else {
      <details id="nodeDetails">
        <summary>Dependencies</summary>
        {
          val _ = new LatestEvent[UIEvent](nodeDetails, "toggle").bind
          if (nodeDetails.asInstanceOf[js.Dynamic].open.asInstanceOf[Boolean]) {
            Constants(dependencies: _*).flatMapBinding { edge =>
              neighborList(graph, clusteringReport, edge.w, draftClusters)
            }
          } else {
            Constants()
          }
        }
      </details>
    }
  }

  sealed trait DependencyExplorerTab {
    @dom
    def navItem(currentTab: Var[DependencyExplorerTab]): Binding[HTMLLIElement] = {
      <li class="nav-item">
        <a
          href=""
          onclick={ event: Event =>
            event.preventDefault()
            currentTab.value = this
          }
          class={
            s"""
              nav-link
              ${ if (currentTab.bind == this) "active" else "" }
            """
          }
        >{ this.toString }</a>
      </li>
    }
  }

  object DependencyExplorerTab {
    case object Root extends DependencyExplorerTab
    case object Leaf extends DependencyExplorerTab
    case object Selection extends DependencyExplorerTab
  }

  @dom
  def render(graph: Graph,
             draftClusters: Vars[DraftCluster],
             clusteringReport: Binding[ClusteringReport],
             rule: Var[ClusteringRule],
             selectedNodeIds: BindingSeq[String]): Binding[Node] =
    <div class="flex-shrink-1 col-4" style:minWidth="0" style:overflowY="auto">{
      val currentTab = Var[DependencyExplorerTab](DependencyExplorerTab.Root)
      <div class="card">
        <ul class="nav nav-tabs sticky-top">
          { DependencyExplorerTab.Root.navItem(currentTab).bind }
          { DependencyExplorerTab.Leaf.navItem(currentTab).bind }
          { DependencyExplorerTab.Selection.navItem(currentTab).bind }
        </ul>
        <div class="card-body">{
          currentTab.bind match {
            case DependencyExplorerTab.Root =>
              Constants(graph.sources(): _*).flatMapBinding { nodeId =>
                neighborList(graph, clusteringReport, nodeId, draftClusters)
              }
            case DependencyExplorerTab.Leaf =>
              Constants(graph.sinks(): _*).flatMapBinding { nodeId =>
                neighborList(graph, clusteringReport, nodeId, draftClusters)
              }
            case DependencyExplorerTab.Selection =>
              selectedNodeIds.flatMapBinding { nodeId =>
                neighborList(graph, clusteringReport, nodeId, draftClusters)
              }
          }
        }</div>
      </div>
    }</div>

}
