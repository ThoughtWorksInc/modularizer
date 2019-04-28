package com.thoughtworks.modularizer.js.views.homepage.importtab

import com.thoughtworks.binding.{Binding, LatestEvent, dom}
import com.thoughtworks.binding.bindable._
import com.thoughtworks.modularizer.js.services.GitStorageUrlConfiguration
import org.scalajs.dom.raw._
import typings.stdLib.GlobalFetch
import typings.stdLib.Response
import typings.stdLib.RequestInit
import typings.stdLib.stdLibStrings.`no-cache`

/**
  * @author 杨博 (Yang Bo)
  */
class BranchInputGroup(implicit fetcher: GlobalFetch, gitStorageConfiguration: GitStorageUrlConfiguration) {

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

  private val checkAvailability: Binding[Option[Either[Any, Response]]] = Binding {
    uncheckedBranchName.bind match {
      case None => None
      case Some(branch) =>
        fetcher
          .fetch(gitStorageConfiguration.graphJsonUrl(branch), RequestInit(cache = `no-cache`, method = "HEAD"))
          .bind
    }
  }

  val branchName: Binding[Option[String]] = Binding {
    checkAvailability.bind match {
      case Some(Right(response)) if response.status == 404 =>
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
            checkAvailability.bind match {
              case None =>
                "" // Loading HTTP header of graph.json
              case Some(Right(response)) if response.status == 404 =>
                "is-valid"
              case _ =>
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
        checkAvailability.bind match {
          case None =>
            <!-- Loading HTTP header of graph.json -->
          case Some(Right(response)) =>
            if (response.ok) {
              <div class="invalid-feedback">The branch {input.bind.value} has been used.</div>
            } else {
              response.status match {
                case 404 =>
                  <div class="valid-feedback">The branch {input.bind.value} is avaible.</div>
                case otherStatus =>
                  <div class="invalid-feedback">{response.statusText}</div>
              }
            }
          case Some(Left(e)) =>
            <div class="invalid-feedback">{e.toString}</div>
        }
      }
    </div>
}
