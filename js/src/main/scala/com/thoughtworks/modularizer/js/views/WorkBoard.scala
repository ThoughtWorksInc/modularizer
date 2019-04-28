package com.thoughtworks.modularizer.js.views
import com.thoughtworks.binding.bindable._
import com.thoughtworks.binding.Binding.{Constants, Var, Vars}
import com.thoughtworks.binding.{Binding, dom}
import com.thoughtworks.modularizer.js.models.{ClusteringReport, ClusteringRule, DraftCluster}
import com.thoughtworks.modularizer.js.services.GitStorageUrlConfiguration
import com.thoughtworks.modularizer.js.views.workboard.{
  BreakingEdgeList,
  DependencyExplorer,
  GraphJsonLoader,
  RuleEditor,
  RuleJsonLoader,
  SummaryDiagram
}
import org.scalablytyped.runtime.StringDictionary
import org.scalajs.dom.raw.{Event, Node}
import typings.graphlibLib.graphlibMod
import typings.graphlibLib.graphlibMod.Graph
import typings.stdLib.{Blob, BlobPropertyBag, GlobalFetch, RequestInit, Response}
import upickle.default._

import scala.concurrent.ExecutionContext
import scala.scalajs.js.{JSON, Thenable, |}
import scala.collection.immutable
import typings.stdLib.stdLibStrings.`no-cache`

import scala.scalajs.js

private object WorkBoard {
  private final val WeakETagRegex = """W/(.*)""".r
}

/**
  * @author 杨博 (Yang Bo)
  */
class WorkBoard(val branch: String)(implicit fetcher: GlobalFetch,
                                    gitStorageConfiguration: GitStorageUrlConfiguration,
                                    executionContext: ExecutionContext)
    extends Page {
  import WorkBoard._

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
          <div class="position-sticky" style:bottom="0">
            <div class="btn-toolbar m-3" data:role="toolbar" style="justify-content:flex-end">
              <div class="btn-group" data:role="group">
                <button
                  type="button"
                  class="btn btn-primary"
                  onclick={ _: Event=>
                    rule.value = ClusteringRule(breakingEdges.value.to[immutable.Seq], draftClusters.value.view.map(_.buildCluster).to[immutable.Seq])
                  }
                >
                  <span class="fas fa-save"></span>
                  Save &amp; Refresh
                </button>
                <button
                  type="button"
                  class="btn btn-secondary"
                  onclick={
                    val compoundGraph = clusteringReport.bind.compoundGraph
                    locally { _: Event =>
                      typings.fileDashSaverLib.fileDashSaverMod.^.saveAs(
                        Blob.newInstance2(
                          js.Array(JSON.stringify(graphlibMod.jsonNs.write(compoundGraph))),
                          BlobPropertyBag(`type` = "text/json")
                        ),
                        filename = "compoundGraph.json"
                      )
                    }
                  }
                >
                  <span class="fas fa-download"></span>
                  Download result
                </button>
              </div>
            </div>
          </div>
        </div>
        { ruleEditor.view.bind }
      </div>
    )
  }

  /** Returns a `If-Match` HTTP request header.
    *
    * @note This method will convert weak ETags to strong ETags,
    *       because `If-Match` MUST use strong comparison according to RFC7232.
    *
    * @see https://tools.ietf.org/html/rfc7232#section-3.1
    *
    */
  private def ifMatch(eTag: String): (String, String) = {
    eTag match {
      case WeakETagRegex(eTag) =>
        "If-Match" -> eTag
      case strongETag =>
        "If-Match" -> strongETag
    }
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
                      headers = StringDictionary(eTag.bind.map(ifMatch).toSeq: _*))
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
