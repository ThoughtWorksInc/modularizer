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

import scala.collection.immutable
import scala.scalajs.js
import scala.scalajs.js.UndefOr

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
  def dependencyExplorer(graph: Graph,
                         unassignedNodes: Vars[String],
                         draftClusters: Vars[DraftCluster],
                         rule: Var[ClusteringRule]): Binding[Node] = {
    val currentTab = Var[DependencyExplorerTab](DependencyExplorerTab.Facades)
    <div class="flex-shrink-1 m-2 card">
      <ul class="nav nav-tabs">
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
            unassignedNodes.flatMapBinding { nodeId =>
              neighborList(graph, nodeId, draftClusters)
            }
        }
      }</div>
    </div>
  }

  private val ClusterColors = scala.util.Random.shuffle(
    Seq(
      "var(--blue)",
      "var(--indigo)",
      "var(--purple)",
      "var(--pink)",
      "var(--red)",
      "var(--orange)",
      "var(--yellow)",
      "var(--green)",
      "var(--teal)",
      "var(--cyan)",
    ))

  final case class DraftCluster(name: Var[String], nodeIds: Vars[String], color: Var[String]) {
    def buildCluster: ClusteringRule.Cluster = {
      ClusteringRule.Cluster(name.value, nodeIds.value.to[immutable.Seq])
    }
  }

  @dom
  def ruleEditor(draftClusters: Vars[DraftCluster], clusteringReport: ClusteringReport) =
    <div class="col flex-shrink-1">
      <div class="d-flex flex-column">
        {
          for (draftCluster <- draftClusters) yield {
            <div class="card m-2">
              <div class="input-group">
                <div class="input-group-prepend">
                  <label
                    class="input-group-text"
                    style:color="var(--white)"
                    style:backgroundColor={draftCluster.color.bind}
                  >Cluster</label>
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
                  <button type="button" class="btn btn-secondary" onclick={ _: Event =>
                    draftClusters.value -= draftCluster
                  }>Delete</button>
                </div>
              </div>
              <div class="card-body">{
                if (draftCluster.nodeIds.length.bind != 0) {
                  <details id="nodesDetails">
                    <summary>
                      Locked Nodes
                      <span class="fas fa-lock"></span>
                      <span class="badge badge-info">{
                        draftCluster.nodeIds.length.bind.toString
                      }</span>
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
                          }>Remove Selected</button>
                        } else {
                          <!-- Hidden Remove Button -->
                        }
                      }
                    </summary>
                    <select class="custom-select" selectedIndex={-1} multiple="multiple" id="selectedNodeIds" size={draftCluster.nodeIds.length.bind}>{
                      for (nodeId <- draftCluster.nodeIds) yield <option value={nodeId}>{ nodeId }</option>
                    }</select>
                  </details>
                  <details>{
                    val allChildNodes = UndefOr.any2undefOrA(clusteringReport.clusteringGraph.children(draftCluster.name.value)).getOrElse(js.Array())
                    val unlockedNodes = allChildNodes -- draftCluster.nodeIds.all.bind
                    <summary>
                      Unlocked Nodes
                      <span class="fas fa-unlock"></span>
                      <span class="badge badge-info">{
                        unlockedNodes.length.toString
                      }</span>
                    </summary>
                    <select class="custom-select" selectedIndex={-1} multiple="multiple" id="selectedUnlockedNodeIds">{
                      for (nodeId <- Constants(unlockedNodes: _*)) yield <option value={nodeId}>{ nodeId }</option>
                    }</select>
                  }</details>
                } else {
                  Constants()
                }
              }</div>
            </div>
          }
        }
        <form class="m-2">
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
              <button type="submit" class="btn btn-secondary" onclick={ event: Event =>
                event.preventDefault()
                val clusterColorHistogram =
                  draftClusters.value
                    .groupBy(_.color.value)
                    .mapValues(_.size)
                    .withDefaultValue(0)
                val nextColor = ClusterColors.minBy(clusterColorHistogram)

                draftClusters.value += DraftCluster(Var(clusterName.value), Vars.empty, Var(nextColor))
                clusterName.value = ""
              }>Add</button>
            </div>
          </div>
        </form>
      </div>
    </div>

  @dom
  def visualize(unpatchedGraph: Graph, rule: Binding[ClusteringRule]) = {
    <div>
      {
        <!-- TODO -->
      }
    </div>
  }
  @dom
  def render(pageState: Var[PageState], graphState: Binding[WorkBoardState], graphOption: Binding[Option[Graph]]) = {
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
        val draftClusters = Vars.empty[DraftCluster]
        // TODO: read draftClusters from saved files

//        algNs.dijkstraAll(graph)
        val clusteringReport = ClusteringReport.cluster(graph, rule.bind)

        console.log(clusteringReport.asInstanceOf[js.Any])

//        val stronglyConnectedComponents = algNs.tarjan(graph)
//        console.log(stronglyConnectedComponents)

        val unassignedNodes: Vars[String] = Vars.empty // TODO: fill unassignedNodes from clusteringReport

        <div class="d-flex container-fluid flex-row">
          { dependencyExplorer(graph, unassignedNodes, draftClusters, rule).bind }
          <div class="col">{ visualize(graph, rule).bind }</div>
          { ruleEditor(draftClusters, clusteringReport).bind }
        </div>
    }
  }

}
