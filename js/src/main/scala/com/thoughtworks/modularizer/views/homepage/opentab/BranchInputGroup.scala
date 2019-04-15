package com.thoughtworks.modularizer.views.homepage.opentab

import com.thoughtworks.binding.{Binding, JsPromiseBinding, LatestEvent, dom}
import com.thoughtworks.modularizer.services.GitStorageUrlConfiguration
import org.scalajs.dom.raw._
import typings.stdLib.GlobalFetch
import typings.stdLib.Response
import typings.stdLib.RequestInit

import scala.scalajs.js.URIUtils._
import scala.scalajs.js

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

  val branchName: Binding[Option[String]] = Binding {
    val element = input.bind
    val _ = LatestEvent.change(element).bind
    element.value match {
      case ""     => None
      case branch => Some(branch)
    }
  }

  @dom
  val view: Binding[HTMLDivElement] =
    <div class="form-group">
      <label for="branch">Branch name</label>
      { input.bind }
      <small class="form-text text-muted">
        The git branch name where this project will open from.
      </small>
    </div>
}
