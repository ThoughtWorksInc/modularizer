package com.thoughtworks.modularizer.views
import com.thoughtworks.binding.bindable._
import com.thoughtworks.binding.Binding.{Constants, Var, Vars}
import com.thoughtworks.binding.{Binding, dom}
import com.thoughtworks.modularizer.models.{ClusteringReport, ClusteringRule, DraftCluster}
import com.thoughtworks.modularizer.services.GitStorageUrlConfiguration
import com.thoughtworks.modularizer.views.workboard.{
  DependencyExplorer,
  GraphJsonLoader,
  RuleEditor,
  RuleJsonLoader,
  SummaryDiagram
}
import org.scalablytyped.runtime.StringDictionary
import org.scalajs.dom.raw.Node
import typings.graphlibLib.graphlibMod.Graph
import typings.stdLib.{GlobalFetch, RequestInit}
import upickle.default._

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Success

/**
  * @author 杨博 (Yang Bo)
  */
class WorkBoard(val branch: String)(implicit fetcher: GlobalFetch,
                                    gitStorageConfiguration: GitStorageUrlConfiguration,
                                    executionContext: ExecutionContext)
    extends Page {

  val graphJsonLoader = new GraphJsonLoader(branch)
  val ruleJsonLoader = new RuleJsonLoader(branch)

  @dom
  def board(graph: Graph, initialRule: ClusteringRule, initialETag: Option[String]): Binding[Constants[Node]] = {
    val rule = Var(initialRule)
    val eTag = Var(initialETag)
    val ruleChanged = Var(false)
    val draftClusters = Vars((for ((cluster, i) <- initialRule.clusters.zipWithIndex) yield {
      DraftCluster.loadFrom(cluster, DraftCluster.CustomClusterColors(i % DraftCluster.CustomClusterColors.length))
    }): _*)
    val clusteringReport = Binding {
      new ClusteringReport(graph, rule.bind)
    }

    val ruleEditor = new RuleEditor(draftClusters, rule, clusteringReport)
    val summaryDiagram = new SummaryDiagram(graph, draftClusters, rule, ruleChanged, clusteringReport)

    Constants(
      autoSave(rule, ruleChanged, eTag).bind,
      <div class="d-flex flex-row flex-grow-1" style:minHeight="0">
        { DependencyExplorer.render(graph, draftClusters, clusteringReport, rule, ruleEditor.selectedNodeIds).bind }
        { summaryDiagram.view.bind }
        { ruleEditor.view.bind }
      </div>
    )
  }

  @dom
  def autoSave(rule: Binding[ClusteringRule],
               ruleChanged: Binding[Boolean],
               eTag: Var[Option[String]]): Binding[Node] = {
    val isSaving = Var(false)
    if (ruleChanged.bind && !isSaving.bind) {
      Future { isSaving.value = true }.bind match {
        case Some(Success(())) =>
          val changedRule = rule.bind
          fetcher
            .fetch(
              gitStorageConfiguration.ruleJsonUrl(branch),
              RequestInit(method = "PUT",
                          body = write(rule.bind),
                          headers = StringDictionary(eTag.bind.map("ETag" -> _).toSeq: _*))
            )
            .bind match {
            case None =>
              <div class="alert alert-info" data:role="alert">
                Save to git repository...
              </div>
            case Some(Right(response)) =>
              if (response.ok) {
                response.headers.get("ETag").asInstanceOf[String] match {
                  case null =>
                    <div class="alert alert-danger" data:role="alert">
                      ETag is not found
                    </div>
                  case nextETag: String =>
                    val _ = Future {
                      eTag.value = Some(nextETag)
                      isSaving.value = false
                    }.bind
                    <!-- Save successful -->
                }
              } else {
                <div class="alert alert-danger" data:role="alert">
                  {
                    response.statusText
                  }
                </div>
              }
            case Some(Left(e)) =>
              <div class="alert alert-danger" data:role="alert">
                {
                  e.toString
                }
              </div>
          }
        case _ =>
          <!-- Waiting for future execution -->
      }
    } else {
      <!-- Rule is unchanged -->
    }
  }
  @dom
  val view: Binding[Node] = {
    <div class="container-fluid d-flex flex-column" style:height="100%">
      { ruleJsonLoader.view.bindSeq }
      { graphJsonLoader.view.bindSeq }
      {
        (graphJsonLoader.result.bind, ruleJsonLoader.result.bind) match {
          case (Some(graph), Some(rule)) =>
            board(graph, rule, ruleJsonLoader.eTag.bind).bindSeq
          case _ =>
            Constants.empty
        }
      }
    </div>
  }

}
