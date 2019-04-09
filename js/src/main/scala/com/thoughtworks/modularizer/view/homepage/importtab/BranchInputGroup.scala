package com.thoughtworks.modularizer.view.homepage.importtab

import com.thoughtworks
import com.thoughtworks.binding
import com.thoughtworks.binding.{Binding, FutureBinding, LatestEvent, dom}
import org.scalajs.dom.ext.{Ajax, AjaxException}
import org.scalajs.dom.raw._

import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala.util.{Failure, Success, Try}

/**
  * @author 杨博 (Yang Bo)
  */
class BranchInputGroup {

  @dom
  private val input: Binding[HTMLInputElement] = <input
    id="branch"
    type="text"
    class="form-control"
    placeholder="Branch name"
    required="required"
    value=""
  />

  private val uncheckedBranchName: Binding[Option[String]] = Binding {
    val element = input.bind
    val _ = LatestEvent.change(element).bind
    element.value match {
      case ""     => None
      case branch => Some(branch)
    }
  }

  private val checkAvailibility: Binding[Option[Try[XMLHttpRequest]]] = Binding {
    uncheckedBranchName.bind match {
      case None => None
      case Some(branch) =>
        FutureBinding(
          Ajax(method = "HEAD",
               url = s"api/git-storages/$branch/graph.json",
               data = null,
               timeout = 0,
               headers = Map.empty,
               withCredentials = false,
               responseType = "")).bind
    }
  }

  val branchName = Binding {
    checkAvailibility.bind match {
      case Some(Failure(AjaxException(xhr))) if xhr.status == 404 =>
        uncheckedBranchName.bind
      case _ =>
        None
    }
  }

  // TODO: override the branch if the an existing graph.json in the branch is found.

  @dom
  val view: Binding[HTMLDivElement] =
    <div class="form-group">
      <label for="branch">Branch name</label>
      {
        val element = input.bind
        element.className = raw"""
          form-control
          ${
            checkAvailibility.bind match {
              case None =>
                ""
              case Some(Success(xhr)) =>
                "is-invalid"
              case Some(Failure(AjaxException(xhr)))  =>
                xhr.status match {
                  case 404 =>
                    "is-valid"
                  case _ =>
                    "is-invalid"
                }
              case Some(Failure(e)) =>
                "is-invalid"
            }
          }
        """
        element
      }
      <small class="form-text text-muted">
        The git branch name where this project will store to.
      </small>
      {
        checkAvailibility.bind match {
          case None =>
            <!-- Loading header of graph.json -->
          case Some(Success(xhr)) =>
            <div class="invalid-feedback">{xhr.getResponseHeader("ETag")}</div>
          case Some(Failure(AjaxException(xhr)))  =>
            xhr.status match {
              case 404 =>
                <div class="valid-feedback">Branch {input.bind.value} is avaible.</div>
              case _ =>
                <div class="invalid-feedback">{xhr.statusText}</div>
            }
          case Some(Failure(e)) =>
            <div class="invalid-feedback">{e.getMessage}</div>
        }
      }
    </div>
}
