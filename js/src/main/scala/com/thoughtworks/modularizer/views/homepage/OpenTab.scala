package com.thoughtworks.modularizer.views.homepage

import com.thoughtworks.binding.FutureBinding
import com.thoughtworks.binding.Binding.Var
import com.thoughtworks.binding.{Binding, JsPromiseBinding, LatestEvent, dom}
import com.thoughtworks.modularizer.models.{JdepsGraph, PageState}
import com.thoughtworks.modularizer.models.PageState.WorkBoardState
import com.thoughtworks.modularizer.services.GitStorageUrlConfiguration
import com.thoughtworks.modularizer.views.homepage.opentab.{BranchInputGroup, OpenButtonGroup}
import org.scalajs.dom.{Event, FileReader}
import org.scalajs.dom.raw.{HTMLFormElement, HTMLInputElement}
import typings.graphlibDashDotLib.graphlibDashDotMod
import typings.graphlibLib.graphlibMod
import typings.stdLib.{GlobalFetch, ReadableStream, RequestInit, Response, Uint8Array, _BodyInit}
import typings.graphlibLib.graphlibMod.Graph

import scala.concurrent.Future
import scala.concurrent.ExecutionContext
import scala.scalajs.js
import scala.scalajs.js.{JSON, |}

class OpenTab(implicit fetcher: GlobalFetch,
              gitStorageConfiguration: GitStorageUrlConfiguration,
              executionContext: ExecutionContext)
    extends Tab {
  private val branchInputGroup = new BranchInputGroup
  private val openButtonGroup = new OpenButtonGroup(branchInputGroup.branchName)

  def result = openButtonGroup.result
  def branchName = branchInputGroup.branchName

  @dom
  val view: Binding[HTMLFormElement] = {
    <form> 
    { branchInputGroup.view.bind }
    { openButtonGroup.view.bind }
    </form>
  }
}
