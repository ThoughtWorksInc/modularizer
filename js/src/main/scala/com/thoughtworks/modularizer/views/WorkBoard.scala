package com.thoughtworks.modularizer.views
import com.thoughtworks.binding.Binding.{Var, Vars}
import com.thoughtworks.binding.{Binding, dom}
import com.thoughtworks.modularizer.utilities._
import com.thoughtworks.modularizer.models.PageState.WorkBoardState
import com.thoughtworks.modularizer.models.{ClusteringReport, ClusteringRule, DraftCluster, PageState}
import com.thoughtworks.modularizer.views.workboard.{DependencyExplorer, RuleEditor, SummaryDiagram}
import org.scalajs.dom.Event
import typings.graphlibLib.graphlibMod.Graph
import upickle.default._
import upickle.implicits._
import scala.scalajs.js.|
import scala.scalajs.js
import scala.scalajs.js.Thenable
import scala.concurrent.ExecutionContext
import typings.stdLib.GlobalFetch
import com.thoughtworks.modularizer.services.GitStorageUrlConfiguration
import ujson.WebJson
import typings.stdLib.RequestInit
import org.scalablytyped.runtime.StringDictionary
import com.thoughtworks.binding.JsPromiseBinding
import typings.stdLib.Response
import org.scalajs.dom.raw.Node
import scala.concurrent.Future

/**
  * @author 杨博 (Yang Bo)
  */
class WorkBoard(graph: Graph, branch: String)(implicit fetcher: GlobalFetch,
                                              gitStorageConfiguration: GitStorageUrlConfiguration,
                                              executionContext: ExecutionContext)
    extends Page {

  val nextState: Binding[String] = Binding {
    "" // TODO
  }

  @dom
  def board(initialRule: ClusteringRule, initialETag: Option[String]) = {
    val rule = Var(initialRule)
    val eTag = Var(initialETag)
    val ruleChanged = Var(false)

    val draftClusters = Vars.empty[DraftCluster]
    // TODO: read draftClusters from saved files
    val clusteringReport = Binding {
      new ClusteringReport(graph, rule.bind)
    }

    val ruleEditor = new RuleEditor(draftClusters, rule, clusteringReport)
    val summaryDiagram = new SummaryDiagram(graph, draftClusters, rule, ruleChanged, clusteringReport)

    <div class="container-fluid">
      {
        autoSave(rule, ruleChanged, eTag).bind
      }
      <div class="d-flex flex-row" style:height="100%">
        { DependencyExplorer.render(graph, draftClusters, clusteringReport, rule, ruleEditor.selectedNodeIds).bind }
        { summaryDiagram.view.bind }
        { ruleEditor.view.bind }
      </div>
    </div>
  }

  @dom
  def autoSave(rule: Binding[ClusteringRule], ruleChanged: Binding[Boolean], eTag: Var[Option[String]]): Binding[Node] =
    if (ruleChanged.bind) {
      val changedRule = rule.bind

      import com.thoughtworks.binding.FutureBinding
      JsPromiseBinding(
        fetcher.fetch(
          gitStorageConfiguration.ruleJsonUrl(branch),
          RequestInit(method = "PUT",
                      body = write(rule.bind),
                      headers = StringDictionary(eTag.bind.map("ETag" -> _).toSeq: _*))
        )
      ).bind match {
        case None =>
          <div class="alert alert-info" data:role="alert">
            Save to git repository...
          </div>
        case Some(Right(response)) =>
          if (response.ok) {
            (response.headers.get("ETag").asInstanceOf[String]) match {
              case null =>
                <div class="alert alert-danger" data:role="alert">
                  ETag is not found
                </div>
              case nextETag: String =>
                val _ = FutureBinding(Future {
                  eTag.value = Some(nextETag)
                }).bind
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
    } else {
      <!-- Rule is unchanged -->
    }

  @dom
  val view = {
    JsPromiseBinding(
      fetcher.fetch(
        gitStorageConfiguration.ruleJsonUrl(branch),
        RequestInit(method = "GET")
      )
    ).bind match {
      case None =>
        <div class="alert alert-info" data:role="alert">
          Connecting to git repository...
        </div>
      case Some(Right(response)) =>
        response.status match {
          case 404 =>
            board(ClusteringRule(Set.empty, collection.immutable.Seq.empty), None).bind
          case _ if response.ok =>
            (response.headers.get("ETag").asInstanceOf[String]) match {
              case null =>
                <div class="alert alert-danger" data:role="alert">
                ETag is not found
              </div>
              case initialETag: String =>
                val ruleJsonPromise = response.json()
                JsPromiseBinding(ruleJsonPromise.`then`[ClusteringRule] { ruleJson =>
                  val initialRule: ClusteringRule =
                    WebJson.transform(ruleJson.asInstanceOf[js.Any], reader[ClusteringRule])
                  initialRule
                }).bind match {
                  case None =>
                    <div class="alert alert-info" data:role="alert">
                    Downloading to rule.json...
                  </div>
                  case Some(Right(initialRule)) =>
                    board(initialRule, Some(initialETag)).bind
                  case Some(Left(e)) =>
                    <div class="alert alert-danger" data:role="alert">
                      {
                        e.toString
                      }
                    </div>
                }
            }
          case _ =>
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

  }

}
