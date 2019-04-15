package com.thoughtworks.modularizer.views
import com.thoughtworks.binding.Binding.{Var, Vars}
import com.thoughtworks.binding.Binding.BindingInstances.monadSyntax._
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
import com.thoughtworks.modularizer.views.homepage.OpenTab
import com.thoughtworks.modularizer.views.homepage.Tab

import scala.concurrent.ExecutionContext
import shapeless.:+:

/**
  * @author 杨博 (Yang Bo)
  */
class HomePage(implicit fetcher: GlobalFetch,
               gitStorageConfiguration: GitStorageUrlConfiguration,
               executionContext: ExecutionContext)
    extends Page {

  val importTab = new ImportTab
  val openTab = new OpenTab
  val currentTab = Var[Tab](importTab)

  val result = currentTab.flatMap(_.result)
  val branch = currentTab.flatMap(_.branchName)

  @dom
  val view = {
    <div class="container-fluid">
      <div class="card">
        {
          <ul class="nav nav-tabs">
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
                  currentTab.value = importTab
                }
                class={s"nav-link ${if (currentTab.bind == importTab) "active" else ""}"}
              >Import</a>
            </li>
          </ul>
          <div class="card-body">{ currentTab.bind.view.bind }</div>
        }
      </div>
    </div>
  }

}
