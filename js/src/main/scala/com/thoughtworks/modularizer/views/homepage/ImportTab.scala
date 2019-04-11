package com.thoughtworks.modularizer.views.homepage

import com.thoughtworks.binding.Binding.Var
import com.thoughtworks.binding.{Binding, JsPromiseBinding, LatestEvent, dom}
import com.thoughtworks.modularizer.models.{JdepsGraph, PageState}
import com.thoughtworks.modularizer.models.PageState.WorkBoardState
import com.thoughtworks.modularizer.services.GitStorageUrlConfiguration
import com.thoughtworks.modularizer.views.homepage.importtab.{BranchInputGroup, DotFileInputGroup, ImportButtonGroup}
import org.scalajs.dom.{Event, FileReader}
import org.scalajs.dom.raw.{HTMLFormElement, HTMLInputElement}
import typings.graphlibDashDotLib.graphlibDashDotMod
import typings.graphlibLib.graphlibMod
import typings.stdLib.{GlobalFetch, ReadableStream, RequestInit, Response, Uint8Array, _BodyInit}

import scala.concurrent.ExecutionContext
import scala.scalajs.js
import scala.scalajs.js.{JSON, |}

/**
  * @author 杨博 (Yang Bo)
  */
class ImportTab(implicit fetcher: GlobalFetch,
                gitStorageConfiguration: GitStorageUrlConfiguration,
                executionContext: ExecutionContext) {

  private val branchInputGroup = new BranchInputGroup
  private val dotFileInputGroup = new DotFileInputGroup
  private val importButtonGroup = new ImportButtonGroup(branchInputGroup.branchName, dotFileInputGroup.loadedText)

  @dom
  val view: Binding[HTMLFormElement] = {

    <form>
      { branchInputGroup.view.bind }
      { dotFileInputGroup.view.bind }
      { importButtonGroup.view.bind }
    </form>
  }
}
