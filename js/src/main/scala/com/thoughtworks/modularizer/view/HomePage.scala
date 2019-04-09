package com.thoughtworks.modularizer.view
import com.thoughtworks.binding.Binding.{Var, Vars}
import com.thoughtworks.binding.{Binding, LatestEvent, dom}
import com.thoughtworks.modularizer.model.PageState.WorkBoardState
import com.thoughtworks.modularizer.util._
import com.thoughtworks.modularizer.model.{JdepsGraph, PageState}
import com.thoughtworks.modularizer.view.homepage.ImportTab
import org.scalajs.dom.{Event, FileList, FileReader, UIEvent}
import org.scalajs.dom.raw.{FileReader, HTMLInputElement, Node}
import typings.graphlibDashDotLib.graphlibDashDotMod
import typings.graphlibLib.graphlibMod.Graph

/**
  * @author 杨博 (Yang Bo)
  */
object HomePage {

  @dom
  def render(pageState: Var[PageState], outputGraph: Var[Option[Graph]]) = {
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
