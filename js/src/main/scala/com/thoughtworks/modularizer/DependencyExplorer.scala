package com.thoughtworks.modularizer
import com.thoughtworks.binding.Binding.BindingInstances.monadSyntax._
import com.thoughtworks.binding.Binding.{BindingSeq, Constants, Var, Vars}
import com.thoughtworks.binding.{Binding, LatestEvent, dom}
import com.thoughtworks.modularizer.model.{ClusteringReport, ClusteringRule, DraftCluster}
import org.scalajs.dom.raw.{Event, HTMLLIElement, Node, UIEvent}
import typings.graphlibLib.graphlibMod.Graph
import util._

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
  def neighborList(graph: Graph, nodeId: String, draftClusters: Vars[DraftCluster]): Binding[BindingSeq[Node]] = {
    <div class="d-flex flex-row align-items-baseline">
      <span title={ nodeId } style:direction="rtl" class="mr-auto flex-shrink-1 text-right text-truncate">{
        nodeId
      }</span>
      {
        if (draftClusters.length.map(_ == 0).bind) {
          Constants()
        } else {
          val currentClusterSeq = for {
            draftCluster <- draftClusters
            clusterNodeId <- draftCluster.nodeIds
            if clusterNodeId == nodeId
          } yield draftCluster
          currentClusterSeq.length.bind match {
            case 0 =>
              Constants(<div class="dropdown">
                <button
                  type="button"
                  class="badge badge-secondary dropdown-toggle"
                  data:data-toggle="dropdown"
                >Unassigned</button>
                <div class="dropdown-menu">{
                  for (draftCluster <- draftClusters) yield {
                    <button
                      class="dropdown-item"
                      type="button"
                      onclick={ _: Event =>
                        val _ = draftCluster.nodeIds.value += nodeId
                      }
                    >
                      Assign to
                      <span class="badge badge-secondary" style:backgroundColor={ draftCluster.color.bind }>{
                        draftCluster.name.bind
                      }</span>
                    </button>
                  }
                }</div>
              </div>)
            case _ =>
              for (currentCluster <- currentClusterSeq) yield <div class="dropdown">
                <button
                  type="button"
                  class="badge badge-secondary dropdown-toggle"
                  data:data-toggle="dropdown"
                  style:color="var(--white)"
                  style:backgroundColor={ currentCluster.color.bind }
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
                          val _ = currentCluster.nodeIds.value -= nodeId
                        }
                      >
                        Unassign from
                        <span class="badge badge-secondary" style:backgroundColor={ draftCluster.color.bind }>{
                          draftCluster.name.bind
                        }</span>
                      </button>
                    } else {
                      <button
                        class="dropdown-item"
                        type="button"
                        onclick={ _: Event =>
                          currentCluster.nodeIds.value -= nodeId
                          val _ = draftCluster.nodeIds.value += nodeId
                        }
                      >
                        Assign to
                        <span class="badge badge-secondary" style:backgroundColor={ draftCluster.color.bind }>{
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
      { dependencyList(graph, nodeId, draftClusters).bind }
      { dependentList(graph, nodeId, draftClusters).bind }
    </div>
  }

  @dom
  def dependentList(graph: Graph, nodeId: String, draftClusters: Vars[DraftCluster]): Binding[Node] = {
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
          val isOpen =
            new LatestEvent[Event](nodeDetails, "toggle")
              .map { _ =>
                nodeDetails.asInstanceOf[js.Dynamic].open.asInstanceOf[Boolean]
              }
              .bind
          if (isOpen) {
            Constants(dependents: _*).flatMapBinding { edge =>
              neighborList(graph, edge.v, draftClusters)
            }
          } else {
            Constants()
          }
        }
      </details>
    }
  }

  @dom
  def dependencyList(graph: Graph, nodeId: String, draftClusters: Vars[DraftCluster]): Binding[Node] = {
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
              neighborList(graph, edge.w, draftClusters)
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
          classMap={
            Map(
              "active" -> (currentTab.bind == this),
              "nav-link" -> true
            )
          }
        >{ this.toString }</a>
      </li>
    }
  }

  object DependencyExplorerTab {
    case object Facades extends DependencyExplorerTab
    case object Utilities extends DependencyExplorerTab
    case object Unassigned extends DependencyExplorerTab
  }

  @dom
  def render(graph: Graph,
             draftClusters: Vars[DraftCluster],
             clusteringReport: Binding[ClusteringReport],
             rule: Var[ClusteringRule]): Binding[Node] =
    <div class="flex-shrink-1 col-auto" style:minWidth="0">{
    val currentTab = Var[DependencyExplorerTab](DependencyExplorerTab.Facades)
    <div class="card">
      <ul class="nav nav-tabs bg-light sticky-top">
        { DependencyExplorerTab.Facades.navItem(currentTab).bind }
        { DependencyExplorerTab.Utilities.navItem(currentTab).bind }

        <li class="nav-item">
          <a
            href=""
            onclick={ event: Event =>
              event.preventDefault()
              currentTab.value = DependencyExplorerTab.Unassigned
            }
            classMap={
              Map(
                "active" -> (currentTab.bind == DependencyExplorerTab.Unassigned),
                "nav-link" -> true
              )
            }
          >
            { DependencyExplorerTab.Unassigned.toString }
            <button
              type="button"
              class="badge badge-secondary"
              onclick={ _: Event=>
                rule.value = ClusteringRule(Set.empty, draftClusters.value.view.map(_.buildCluster).to[immutable.Seq])
              }
            ><span class="fas fa-sync"></span></button>
          </a>
        </li>
      </ul>
      <div class="card-body">{
        currentTab.bind match {
          case DependencyExplorerTab.Facades =>
            Constants(graph.sources(): _*).flatMapBinding { nodeId =>
              neighborList(graph, nodeId, draftClusters)
            }
          case DependencyExplorerTab.Utilities =>
            Constants(graph.sinks(): _*).flatMapBinding { nodeId =>
              neighborList(graph, nodeId, draftClusters)
            }
          case DependencyExplorerTab.Unassigned =>
            val unassignedNodes = Constants.empty[String] // TODO:
            unassignedNodes.flatMapBinding { nodeId =>
              neighborList(graph, nodeId, draftClusters)
            }
        }
      }</div>
    </div>
  }</div>

}
