package com.thoughtworks.modularizer.js.views.workboard

import com.thoughtworks.binding.Binding.Constants
import com.thoughtworks.binding.bindable._
import com.thoughtworks.binding.{Binding, dom}
import com.thoughtworks.modularizer.js.services.GitStorageUrlConfiguration
import org.scalajs.dom.raw.Node
import typings.graphlibLib.graphlibMod
import typings.graphlibLib.graphlibMod.Graph
import typings.stdLib.stdLibStrings.`no-cache`
import typings.stdLib.{GlobalFetch, RequestInit, Response}

import scala.concurrent.ExecutionContext
import scala.scalajs.js
import scala.scalajs.js.{Promise, Thenable}

/**
  * @author 杨博 (Yang Bo)
  */
class GraphJsonLoader(branch: String)(implicit fetcher: GlobalFetch,
                                      gitStorageConfiguration: GitStorageUrlConfiguration,
                                      executionContext: ExecutionContext) {

  private val graphResponse: Promise[Response] =
    fetcher.fetch(
      gitStorageConfiguration.graphJsonUrl(branch),
      RequestInit(cache = `no-cache`,method = "GET")
    )
  private val graphBody: Binding[Option[Thenable[Graph]]] = Binding {
    graphResponse.bind match {
      case Some(Right(response)) if response.ok =>
        val graphJsonPromise = response.json().asInstanceOf[js.Promise[js.Object]]
        Some(graphJsonPromise.`then`[Graph](graphlibMod.jsonNs.read(_)))
      case _ =>
        None
    }
  }
  val result: Binding[Option[Graph]] = Binding {
    graphBody.bind match {
      case Some(thenable) =>
        thenable.bind match {
          case Some(Right(graph)) =>
            Some(graph)
          case _ =>
            None
        }
      case _ =>
        None
    }
  }
  @dom
  val graphResponseStatus: Binding[Node] = {
    graphResponse.bind match {
      case None =>
        <div class="alert alert-info" data:role="alert">
          Downloading graph.json header...
        </div>
      case Some(Right(response)) =>
        if (response.ok) {
          <!-- graph.json header loaded -->
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
  }
  @dom
  val graphBodyStatus: Binding[Node] = {
    graphBody.bind match {
      case None =>
        <!-- graph.json content is not available -->
      case Some(thenable) =>
        thenable.bind match {

          case None =>
            <div class="alert alert-info" data:role="alert">
              Downloading graph.json...
            </div>
          case Some(Right(initialRule)) =>
            <!-- graph.json downloaded -->
          case Some(Left(e)) =>
            <div class="alert alert-danger" data:role="alert">
              {
                e.toString
              }
            </div>
        }
    }
  }
  val view: Binding[Constants[Node]] = Binding {
    Constants(graphResponseStatus.bind, graphBodyStatus.bind)
  }
}
