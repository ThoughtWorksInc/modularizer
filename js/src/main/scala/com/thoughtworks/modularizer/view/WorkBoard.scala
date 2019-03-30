package com.thoughtworks.modularizer.view
import com.thoughtworks.binding.Binding.{Var, Vars}
import com.thoughtworks.binding.{Binding, dom}
import com.thoughtworks.modularizer.model.PageState.WorkBoardState
import com.thoughtworks.modularizer.model.{ClusteringReport, ClusteringRule, DraftCluster, PageState}
import com.thoughtworks.modularizer.view.workboard.{DependencyExplorer, RuleEditor, SummaryDiagram}
import org.scalajs.dom.Event
import typings.graphlibLib.graphlibMod.Graph

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
        val clusteringReport = Binding {
          new ClusteringReport(graph, rule.bind)
        }

        val ruleEditor = new RuleEditor(draftClusters, clusteringReport)
        val summaryDiagram = new SummaryDiagram(graph, rule, clusteringReport)

        <div class="d-flex container-fluid flex-row">
          { DependencyExplorer.render(graph, draftClusters, clusteringReport, rule, ruleEditor.selectedNodeIds).bind }
          { summaryDiagram.view.bind }
          { ruleEditor.view.bind }
        </div>
    }
  }

}
