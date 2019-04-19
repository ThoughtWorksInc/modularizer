package com.thoughtworks.modularizer.views.workboard

import com.thoughtworks.binding.bindable._
import com.thoughtworks.binding.Binding.Constants
import com.thoughtworks.binding.{Binding, dom}
import com.thoughtworks.modularizer.services.GitStorageUrlConfiguration
import org.scalajs.dom.raw._
import org.scalajs.dom._
import typings.stdLib.GlobalFetch
import scala.concurrent.ExecutionContext
import com.thoughtworks.modularizer.models.ClusteringRule
import typings.stdLib.RequestInit
import ujson.WebJson
import upickle.default.reader

import scala.scalajs.js
import scala.scalajs.js.Thenable

/**
  * @author 杨博 (Yang Bo)
  */
class RuleJsonLoader(branch: String)(implicit fetcher: GlobalFetch,
                                     gitStorageConfiguration: GitStorageUrlConfiguration,
                                     executionContext: ExecutionContext) {

  private val ruleResponse =
    fetcher.fetch(
      gitStorageConfiguration.ruleJsonUrl(branch),
      RequestInit(method = "GET")
    )

  val eTag: Binding[Option[String]] = Binding {
    ruleResponse.bind match {
      case Some(Right(response)) if response.ok =>
        Option(response.headers.get("ETag").asInstanceOf[String])
      case _ =>
        None
    }
  }

  private val ruleBody: Binding[Option[Thenable[ClusteringRule]]] = Binding {
    ruleResponse.bind match {
      case Some(Right(response)) =>
        if (response.ok) {
          val ruleJsonPromise = response.json()
          Some(ruleJsonPromise.`then`[ClusteringRule] { ruleJson =>
            val initialRule: ClusteringRule =
              WebJson.transform(ruleJson.asInstanceOf[js.Any], reader[ClusteringRule])
            initialRule
          })
        } else {
          None
        }
      case _ =>
        None
    }
  }

  val result: Binding[Option[ClusteringRule]] = Binding {
    ruleResponse.bind match {
      case Some(Right(response)) if response.status == 404 =>
        Some(ClusteringRule.empty)
      case _ =>
        ruleBody.bind match {
          case Some(thenable) =>
            thenable.bind match {
              case Some(Right(initialRule)) =>
                Some(initialRule)
              case _ =>
                None
            }
          case _ =>
            None
        }
    }
  }

  @dom
  val ruleResponseStatus: Binding[Node] = {
    ruleResponse.bind match {
      case None =>
        <div class="alert alert-info" data:role="alert">
          Downloading rule.json header...
        </div>
      case Some(Right(response)) =>
        <!-- rule.json header loaded -->
      case Some(Left(e)) =>
        <div class="alert a lert-danger" data:role="alert">
          {
            e.toString
          }
        </div>
    }
  }

  @dom
  val ruleBodyStatus: Binding[Node] = {
    ruleBody.bind match {
      case None =>
        <!-- rule.json content is not available -->
      case Some(thenable) =>
        thenable.bind match {
          case None =>
            <div class="alert alert-info" data:role="alert">
              Downloading rule.json...
            </div>
          case Some(Right(initialRule)) =>
            <!-- rule.json downloaded -->
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
    Constants(ruleResponseStatus.bind, ruleBodyStatus.bind)
  }
}
