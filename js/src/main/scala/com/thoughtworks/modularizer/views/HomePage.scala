package com.thoughtworks.modularizer.views
import com.thoughtworks.binding.Binding.{Var, Vars}
import com.thoughtworks.binding.{Binding, LatestEvent, dom}
import com.thoughtworks.modularizer.models.PageState.WorkBoardState
import com.thoughtworks.modularizer.utilities._
import com.thoughtworks.modularizer.models.{JdepsGraph, PageState}
import com.thoughtworks.modularizer.services.GitStorageUrlConfiguration
import com.thoughtworks.modularizer.views.homepage.ImportTab
import org.scalajs.dom.{Event, FileList, FileReader, UIEvent}
import org.scalajs.dom.raw.{FileReader, HTMLInputElement, Node}
import typings.graphlibDashDotLib.graphlibDashDotMod
import typings.graphlibLib.graphlibMod.Graph
import typings.stdLib.GlobalFetch

import scala.concurrent.ExecutionContext

/**
  * @author 杨博 (Yang Bo)
  */
class HomePage(pageState: Var[PageState], outputGraph: Var[Option[Graph]])(
    implicit fetcher: GlobalFetch,
    gitStorageConfiguration: GitStorageUrlConfiguration,
    executionContext: ExecutionContext) {

  @dom
  val view = {
    val readerOption: Var[Option[FileReader]] = Var(None)

    <div class="container-fluid">
      <div class="card">
        {
          val openTab: Node = {
            <div>TODO</div>
          }
          val importTab = new ImportTab
          val currentTab = Var[Node](importTab.view.bind)
          <ul class="nav nav-tabs sticky-top">
            <li class="nav-item">
              <a
                href=""
                onclick={ event: Event =>
                  event.preventDefault()
                  currentTab.value = openTab
                }
                class={s"nav-link ${if (currentTab.bind == openTab) "active" else ""}"}
              >Open</a>
            </li>
            <li class="nav-item">
              <a
                href=""
                onclick={ event: Event =>
                  event.preventDefault()
                  currentTab.value = importTab.view.value
                }
                class={s"nav-link ${if (currentTab.bind == importTab.view.bind) "active" else ""}"}
              >Import</a>
            </li>
          </ul>
          <div class="card-body">{ currentTab.bind }</div>
        }
      </div>
    </div>
  }

}
