package com.thoughtworks.modularizer.views
import com.thoughtworks.binding.bindable._
import com.thoughtworks.binding.Binding.{Constants, Var, Vars}
import com.thoughtworks.binding.{Binding, dom}
import com.thoughtworks.modularizer.models.{ClusteringReport, ClusteringRule, DraftCluster}
import com.thoughtworks.modularizer.services.GitStorageUrlConfiguration
import com.thoughtworks.modularizer.views.workboard.{
  BreakingEdgeList,
  DependencyExplorer,
  GraphJsonLoader,
  RuleEditor,
  RuleJsonLoader,
  SummaryDiagram
}
import org.scalablytyped.runtime.StringDictionary
import org.scalajs.dom.raw.Node
import typings.graphlibLib.graphlibMod.Graph
import typings.stdLib.{GlobalFetch, RequestInit, Response}
import upickle.default._

import scala.concurrent.{ExecutionContext, Future}
import scala.scalajs.js.{Thenable, |}
import scala.util.Success
import scala.collection.immutable
import org.scalajs.dom.raw.Event
import typings.stdLib.stdLibStrings.`no-cache`

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
    val savedRule = Var(initialRule)
    val breakingEdges = Vars(initialRule.breakingEdges.toSeq: _*)
    val draftClusters = Vars((for ((cluster, i) <- initialRule.clusters.zipWithIndex) yield {
      DraftCluster.loadFrom(cluster, DraftCluster.CustomClusterColors(i % DraftCluster.CustomClusterColors.length))
    }): _*)
    val clusteringReport = Binding {
      new ClusteringReport(graph, rule.bind)
    }

    val ruleEditor = new RuleEditor(draftClusters, rule, clusteringReport)
    val summaryDiagram = new SummaryDiagram(graph, draftClusters, breakingEdges, rule, clusteringReport)
    val breakingEdgeList = new BreakingEdgeList(breakingEdges)

    Constants(
      autoSave(rule, savedRule, eTag).bind,
      <div class="d-flex flex-row flex-grow-1" style:minHeight="0">
        { DependencyExplorer.render(graph, draftClusters, clusteringReport, rule, ruleEditor.selectedNodeIds).bind }
        <div class="col-5" style:overflowY="auto">
          {
            breakingEdgeList.view.bindSeq
          }
          {
            summaryDiagram.view.bind
          }
          <div class="position-sticky flex-row" style:bottom="0">
            <button
              type="button"
              class="btn btn-primary position-sticky ml-auto d-block m-3"
              onclick={ _: Event=>
                rule.value = ClusteringRule(breakingEdges.value.to[immutable.Seq], draftClusters.value.view.map(_.buildCluster).to[immutable.Seq])
              }
            ><span class="fas fa-save"></span></button>
          </div>
        </div>
        { ruleEditor.view.bind }
      </div>
    )
  }

  @dom
  def autoSave(rule: Binding[ClusteringRule],
               savedRule: Var[ClusteringRule],
               eTag: Var[Option[String]]): Binding[Node] = {
    val changedRule = rule.bind
    if (savedRule.bind ne changedRule) {
      val responsePromise = fetcher
        .fetch(
          gitStorageConfiguration.ruleJsonUrl(branch),
          RequestInit(cache = `no-cache`,
                      method = "PUT",
                      body = write(rule.bind),
                      headers = StringDictionary(eTag.bind.map("If-Match" -> _).toSeq: _*))
        )
      responsePromise
        .`then`[Response] { response: Response =>
          if (response.ok) {
            val nullableETag = response.headers.get("ETag").asInstanceOf[String]
            if (nullableETag != null) {
              savedRule.value = changedRule
              eTag.value = Some(nullableETag)
            }
          }
          response: Response | Thenable[Response]
        }
        .bind match {
        case None =>
          <div class="alert alert-info" data:role="alert">
            Save to git repository...
          </div>
        case Some(Right(response)) =>
          if (response.ok) {
            response.headers.get("ETag").asInstanceOf[String] match {
              case null =>
                <div class="alert alert-danger" data:role="alert">ETag is not found</div>
              case nextETag: String =>
                <!-- Save successful -->
            }
          } else {
            <div class="alert alert-danger" data:role="alert">{ response.statusText }</div>
          }
        case Some(Left(e)) =>
          <div class="alert alert-danger" data:role="alert">{ e.toString }</div>
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
