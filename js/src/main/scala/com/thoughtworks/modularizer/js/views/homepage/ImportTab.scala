package com.thoughtworks.modularizer.js.views.homepage

import com.thoughtworks.binding.{Binding, dom}
import com.thoughtworks.modularizer.js.services.GitStorageUrlConfiguration
import com.thoughtworks.modularizer.js.views.homepage.importtab.{BranchInputGroup, DotFileInputGroup, ImportButtonGroup}
import org.scalajs.dom.raw.HTMLFormElement
import typings.stdLib.GlobalFetch

import scala.concurrent.ExecutionContext

/**
  * @author 杨博 (Yang Bo)
  */
class ImportTab(implicit fetcher: GlobalFetch,
                gitStorageConfiguration: GitStorageUrlConfiguration,
                executionContext: ExecutionContext)
    extends Tab {

  private val branchInputGroup = new BranchInputGroup
  private val dotFileInputGroup = new DotFileInputGroup
  private val importButtonGroup = new ImportButtonGroup(branchInputGroup.branchName, dotFileInputGroup.loadedText)

  def branchName = Binding {
    importButtonGroup.result.bind match {
      case None =>
        None
      case Some(graph) =>
        branchInputGroup.branchName.bind
    }
  }

  @dom
  val view: Binding[HTMLFormElement] = {
    <form>
      { branchInputGroup.view.bind }
      { dotFileInputGroup.view.bind }
      { importButtonGroup.view.bind }
    </form>
  }
}
