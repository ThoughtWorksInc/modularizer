package com.thoughtworks.modularizer
import com.thoughtworks.binding.Binding.{BindingSeq, Constants, Var, Vars}
import com.thoughtworks.binding.Binding.BindingInstances.monadSyntax._
import com.thoughtworks.binding.{Binding, LatestEvent, dom}
import com.thoughtworks.Extractor._
import com.thoughtworks.modularizer.WorkBoard.DependencyExplorerTab
import com.thoughtworks.modularizer.model.{ClusteringReport, ClusteringRule, PageState}
import com.thoughtworks.modularizer.model.PageState.WorkBoardState
import org.scalajs.dom._
import org.scalajs.dom.raw._
import typings.graphlibLib.graphlibMod.Graph
import typings.graphlibLib.graphlibMod.algNs
import com.thoughtworks.modularizer.util._

import scala.scalajs.js

/**
  * @author 杨博 (Yang Bo)
  */
object WorkBoard {

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
    Constants(
      <div class="d-flex flex-row">
        <span title={ nodeId } style="direction:rtl" class="mr-auto flex-shrink-1 text-right text-truncate">{
          nodeId
        }</span>
        {
          val currentClusterSeq = for {
            draftCluster <- draftClusters
            clusterNodeId <- draftCluster.nodeIds
            if clusterNodeId == nodeId
          } yield draftCluster.name.bind
          currentClusterSeq.length.bind match {
            case 0 =>
              Constants(<div class="dropdown">
                <button class="badge dropdown-toggle" type="button" data:data-toggle="dropdown" >
                  Assign to
                </button>
                <div class="dropdown-menu">{
                  for (draftCluster <- draftClusters) yield {
                    <button
                      class="dropdown-item"
                      type="button"
                      onclick={ _: Event =>
                        val _ = draftCluster.nodeIds.value += nodeId
                      }
                    >{ draftCluster.name.bind }</button>
                  }
                }</div>
              </div>)
            case _ =>
              for (currentCluster <- currentClusterSeq) yield {
                <span class="badge badge-info">{ currentCluster }</span>
              }
          }
        }
      </div>,
      dependencyList(graph, nodeId, draftClusters).bind,
      dependentList(graph, nodeId, draftClusters).bind
    )
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
            <div class="pl-2 border-left">{
              Constants(dependents: _*).flatMapBinding { edge =>
                neighborList(graph, edge.v, draftClusters)
              }
            }</div>
          } else {
            <!-- hidden details -->
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
            <div class="pl-2 border-left">{
              Constants(dependencies: _*).flatMapBinding { edge =>
                neighborList(graph, edge.w, draftClusters)
              }
            }</div>
          } else {
            <!-- hidden details -->
          }
        }
      </details>
    }
  }

  sealed trait DependencyExplorerTab {

    @dom
    def navItem(currentTab: Var[DependencyExplorerTab]): Binding[HTMLLIElement] = {
      <li class="nav-item">
        <button
          type="button"
          onclick={ event: Event =>
            event.preventDefault()
            currentTab.value = this
          }
          classMap={
            Map(
              "active" -> (currentTab.bind == this),
              "nav-link" -> true,
              "btn-link" -> true
            )
          }
        >{ this.toString }</button>
      </li>
    }
  }
  object DependencyExplorerTab {
    case object Facades extends DependencyExplorerTab
    case object Utilities extends DependencyExplorerTab
    case object Unassigned extends DependencyExplorerTab
  }

  @dom
  def dependencyExplorer(graph: Graph,
                         unassignedNodes: Vars[String],
                         draftClusters: Vars[DraftCluster]): Binding[BindingSeq[Node]] = {
    val currentTab = Var[DependencyExplorerTab](DependencyExplorerTab.Facades)
    <ul class="nav nav-tabs">
      { DependencyExplorerTab.Facades.navItem(currentTab).bind }
      { DependencyExplorerTab.Utilities.navItem(currentTab).bind }
      { DependencyExplorerTab.Unassigned.navItem(currentTab).bind }
    </ul>
    <div>{
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
          unassignedNodes.flatMapBinding { nodeId =>
            neighborList(graph, nodeId, draftClusters)
          }
      }
    }</div>

  }
  final case class DraftCluster(name: Var[String], nodeIds: Vars[String])

  @dom
  def ruleEditor(draftClusters: Vars[DraftCluster]) = {
    <div class="d-flex flex-column">
      {
        for (draftCluster <- draftClusters) yield {
          <div class="card">
            <div class="input-group">
              <div class="input-group-prepend">
                <label class="input-group-text">Cluster</label>
              </div>
              <input
                id="clusterName"
                type="text"
                class="form-control"
                value={ draftCluster.name.bind }
                onchange={_: Event =>
                  draftCluster.name.value = clusterName.value
                }
              />
              <div class="input-group-append">
                <button type="button" class="btn btn-danger" onclick={ _: Event =>
                  draftClusters.value -= draftCluster
                }>Delete</button>
              </div>
            </div>
            <details
              id="nodesDetails"
              classMap={
                Map(
                  "card-body" -> true,
                  "d-none" -> (draftCluster.nodeIds.length.bind == 0)
                )
              }
            >
              <summary>
                Nodes
                <span class="badge badge-primary badge-pill">{ draftCluster.nodeIds.length.bind.toString }</span>
                {
                  val isDetailOpen =
                    new LatestEvent[Event](nodesDetails, "toggle")
                      .map { _ =>
                        nodesDetails.asInstanceOf[js.Dynamic].open.asInstanceOf[Boolean]
                      }
                      .bind
                  val hasSelectedOption =
                    new LatestEvent[Event](selectedNodeIds, "change")
                      .tuple(draftCluster.nodeIds.length)
                      .map { _ =>
                        selectedNodeIds.selectedIndex != -1
                      }
                      .bind
                  if (isDetailOpen && hasSelectedOption) {
                    <button type="button" class="float-right badge badge-danger" onclick={ _: Event =>
                      for (option <- selectedNodeIds.options.toArray) {
                        if (option.selected) {
                          draftCluster.nodeIds.value -= option.value
                        }
                      }
                    }>Remove</button>
                  } else {
                    <!-- Hidden Remove Button -->
                  }
                }
              </summary>
              <select class="custom-select" selectedIndex={-1} multiple="multiple" id="selectedNodeIds" size={draftCluster.nodeIds.length.bind}>{
                for (nodeId <- draftCluster.nodeIds) yield <option value={nodeId}>{ nodeId }</option>
              }</select>
            </details>
          </div>
        }
      }
      <form>
        <div class="input-group">
          <div class="input-group-prepend">
            <label class="input-group-text">New Cluster</label>
          </div>
          <input
            id="clusterName"
            type="text"
            class="form-control"
          />
          <div class="input-group-append">
            <button type="submit" class="btn btn-primary" onclick={ event: Event =>
              draftClusters.value += DraftCluster(Var(clusterName.value), Vars.empty)
              clusterName.value = ""
              event.preventDefault()
            }>Add</button>
          </div>
        </div>
      </form>
    </div>
  }
  @dom
  def visualize(unpatchedGraph: Graph, rule: Binding[ClusteringRule]) = {
    <div>
      {
        <!-- TODO -->
      }
    </div>
  }
  @dom
  def render(pageState: Var[PageState], graphState: Binding[WorkBoardState], graphOption: Binding[Option[Graph]]) =
    <div class="container-fluid">{
    graphOption.bind match {
      case None =>
        <div class="alert alert-warning" data:role="alert">
          No graph found. You may want to import a <kbd>jdeps</kbd> report first.
          <button type="button" class="btn btn-primary" onclick={ _: Event =>
            pageState.value = PageState.ImportJdepsDotFile
          }>Import</button>
        </div>
      case Some(graph) =>
        val rule = Var(ClusteringRule(Set.empty, Nil))
        val draftClusters = Vars(
          DraftCluster(Var("module-1"), Vars("com.ebao.ciitc.insurance.dto.trans", "com.ebao.life.agent.agency", "com.ebao.life.batch.calccv")),
          DraftCluster(Var("module-2"), Vars("com.aratek.finger", "com.ebao.ciitc.insurance.businessmodule", "com.ebao.health.insurance.datachange.asyclaim"))
        )
        // TODO: read draftClusters from saved files


//        algNs.dijkstraAll(graph)
        val clusteringReport = ClusteringReport.cluster(graph, rule.bind)

//        val stronglyConnectedComponents = algNs.tarjan(graph)
//        console.log(stronglyConnectedComponents)

        val unassignedNodes: Vars[String] = Vars.empty // TODO: fill unassignedNodes from clusteringReport

        <div class="d-flex flex-row">

          <div class="col-sm-auto">{ dependencyExplorer(graph, unassignedNodes, draftClusters).bind }</div>
          <!-- TODO: dependency explorer -->
          <div class="col">{ visualize(graph,rule).bind }</div>
          <!-- TODO: statistics -->
          <!-- TODO: rule editor -->
          { ruleEditor(draftClusters).bind }
        </div>
    }
  }</div>

}
