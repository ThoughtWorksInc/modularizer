package com.thoughtworks.modularizer
import com.thoughtworks.binding.Binding.Var
import com.thoughtworks.binding.{Binding, dom}
import com.thoughtworks.modularizer.model.{ClusteringReport, ClusteringRule, PageState}
import com.thoughtworks.modularizer.model.PageState.WorkBoardState
import org.scalajs.dom._
import org.scalajs.dom.raw._
import typings.graphlibLib.graphlibMod.Graph
import typings.graphlibLib.graphlibMod.algNs

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
  def dependencyExplorer(graph: Graph) = {

    <div class="list-group">
      <a href="#" class="list-group-item list-group-item-action active">
        Cras justo odio
      </a>
      <div class="list-group">
        <a href="#" class="list-group-item list-group-item-action active">
          Cras justo odio
        </a>
        <a href="#" class="list-group-item list-group-item-action">Dapibus ac facilisis in</a>
        <a href="#" class="list-group-item list-group-item-action">Morbi leo risus</a>
        <a href="#" class="list-group-item list-group-item-action">Porta ac consectetur ac</a>
        <a href="#" class="list-group-item list-group-item-action disabled">Vestibulum at eros</a>
      </div>
      <a href="#" class="list-group-item list-group-item-action">Dapibus ac facilisis in</a>
      <a href="#" class="list-group-item list-group-item-action">Morbi leo risus</a>
      <a href="#" class="list-group-item list-group-item-action">Porta ac consectetur ac</a>
      <a href="#" class="list-group-item list-group-item-action disabled">Vestibulum at eros</a>
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

//        algNs.dijkstraAll(graph)
        val clusteringReport = ClusteringReport.cluster(graph, rule.bind)

//        val stronglyConnectedComponents = algNs.tarjan(graph)
//        console.log(stronglyConnectedComponents)

        <div class="row">

          <div class="col-md-auto">{ dependencyExplorer(graph).bind }</div>
          <!-- TODO: dependency explorer -->
          <div class="col">{ visualize(graph,rule).bind }</div>
          <!-- TODO: statistics -->
          <!-- TODO: rule editor -->
        </div>
    }
  }</div>

}
