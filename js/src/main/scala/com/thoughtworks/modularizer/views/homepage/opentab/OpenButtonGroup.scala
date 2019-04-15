package com.thoughtworks.modularizer.views.homepage.opentab

import com.thoughtworks.binding.{Binding, FutureBinding, JsPromiseBinding, dom}
import com.thoughtworks.binding.Binding.Var
import com.thoughtworks.modularizer.models.JdepsGraph
import com.thoughtworks.modularizer.services.GitStorageUrlConfiguration
import org.scalajs.dom.raw._
import org.scalajs.dom._
import typings.graphlibDashDotLib.graphlibDashDotMod
import typings.graphlibLib.graphlibMod
import typings.stdLib.{GlobalFetch, RequestInit, Response}
import typings.graphlibLib.graphlibMod.{Graph, GraphOptions}

import scala.concurrent.{ExecutionContext, Future}
import scala.scalajs.js
import scala.scalajs.js.{JSON, |}
import scala.util.{Failure, Success}

/**
  * @author 杨博 (Yang Bo)
  */
class OpenButtonGroup(branchName: Binding[Option[String]])(implicit fetcher: GlobalFetch,
                                                           gitStorageConfiguration: GitStorageUrlConfiguration,
                                                           executionContext: ExecutionContext) {

  private val downloading: Var[Option[js.Thenable[Response]]] = Var(None)
  private val parsing: Binding[Option[js.Thenable[Graph]]] = Binding {
    downloading.bind match {
      case Some(dowloadingStarted) =>
        JsPromiseBinding(dowloadingStarted).bind match {
          case Some(Right(response)) =>
            val jsonPromise = response.json().asInstanceOf[js.Promise[js.Object]]
            Some(jsonPromise.`then`[Graph](graphlibMod.jsonNs.read(_)))
          case _ =>
            None
        }
      case None =>
        None
    }
  }

  /** The result graph after successful downloading */
  val result: Binding[Option[Graph]] = Binding {
    parsing.bind match {
      case Some(thenable) => 
        JsPromiseBinding(thenable).bind match {
          case Some(Right(graph)) => 
            Some(graph)
          case _ =>
            None
        }
      case _ =>
        None
    }
  }

  private val isPendingDownload = Binding {
    downloading.bind match {
      case None =>
        false
      case Some(dowloadingStarted) =>
        JsPromiseBinding(dowloadingStarted).bind.isEmpty
    }
  }

  private val isPendingParse = Binding {
    parsing.bind match {
      case None =>
        false
      case Some(parsingStarted) =>
        JsPromiseBinding(parsingStarted).bind.isEmpty
    }
  }

  @dom
  val view: Binding[Node] = {
    branchName.bind match {
      case None =>
        <!-- No branch selected -->
      case Some(branch) =>
        <div class="form-group">
          <button
            type="submit"
            class="btn btn-primary"
            disabled={ isPendingDownload.bind || isPendingParse.bind }
            onclick={ event: Event =>
              event.preventDefault()
              downloading.value = Some(fetcher.fetch(gitStorageConfiguration.graphJsonUrl(branch), RequestInit(method = "GET")))
            }
          >Import</button>
          {
              downloading.bind match {
                case None =>
                  <!-- Downloading is not started yet -->
                case Some(dowloadingStarted) =>
                  JsPromiseBinding(dowloadingStarted).bind match {
                    case None =>
                      <div class="alert alert-info" data:role="alert">
                        Connecting to git repository...
                      </div>
                    case Some(Left(e)) =>
                      <div class="alert alert-danger" data:role="alert">
                        {
                          e.toString
                        }
                      </div>
                    case Some(Right(response)) =>
                      if (response.ok) {
                        parsing.bind match {
                          case None => 
                            <div class="alert alert-success" data:role="alert">
                              Connected to git repository.
                            </div>
                          case Some(parsingStarted) =>
                            JsPromiseBinding(parsingStarted).bind match {
                              case None => 
                                <div class="alert alert-info" data:role="alert">
                                  Downloading the graph from git repository...
                                </div>
                              case Some(Right(graph)) =>
                                <div class="alert alert-success" data:role="alert">
                                  The dependency graph is downloaded successfully.
                                </div>
                              case Some(Left(e)) =>
                                <div class="alert alert-danger" data:role="alert">
                                  {
                                    e.toString
                                  }
                                </div>
                            }
                        }
                      } else {
                        <div class="alert alert-danger" data:role="alert">
                          {
                            response.statusText
                          }
                        </div>
                      }
                  }
              }
            }
        </div>
    }

  }

}
