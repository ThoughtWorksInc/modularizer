package com.thoughtworks.modularizer
import com.thoughtworks.binding.Binding.{BindingSeq, Constants, Var, Vars}
import com.thoughtworks.binding.Binding.BindingInstances.monadSyntax._
import com.thoughtworks.binding.{Binding, LatestEvent, dom}
import com.thoughtworks.Extractor._
import com.thoughtworks.modularizer.model.{ClusteringReport, ClusteringRule, DraftCluster, PageState}
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
        val clusteringReport = rule.map(ClusteringReport(graph, _))
        <div class="d-flex container-fluid flex-row">
          { DependencyExplorer.render(graph, draftClusters, clusteringReport, rule).bind }
          { SummaryDiagram.render(graph, clusteringReport).bind }
          { RuleEditor.render(draftClusters, clusteringReport).bind }
        </div>
    }
  }

}
