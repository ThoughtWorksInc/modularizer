package com.thoughtworks.modularizer.views.homepage

import com.thoughtworks.binding.{Binding, dom}
import com.thoughtworks.modularizer.services.GitStorageUrlConfiguration
import com.thoughtworks.modularizer.views.homepage.opentab.{BranchInputGroup, OpenButtonGroup}
import org.scalajs.dom.raw.HTMLFormElement
import typings.stdLib.GlobalFetch
import scala.concurrent.ExecutionContext

class OpenTab(implicit fetcher: GlobalFetch,
              gitStorageConfiguration: GitStorageUrlConfiguration,
              executionContext: ExecutionContext)
    extends Tab {
  private val branchInputGroup = new BranchInputGroup
  private val openButtonGroup = new OpenButtonGroup(branchInputGroup.branchName)

  def branchName: Binding[Option[String]] = Binding {
    if (openButtonGroup.isClicked.bind) {
      branchInputGroup.branchName.bind
    } else {
      None
    }
  }

  @dom
  val view: Binding[HTMLFormElement] = {
    <form> 
    { branchInputGroup.view.bind }
    { openButtonGroup.view.bind }
    </form>
  }
}
