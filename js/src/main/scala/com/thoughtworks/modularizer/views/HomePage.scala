package com.thoughtworks.modularizer.views
import com.thoughtworks.binding.Binding.BindingInstances.monadSyntax._
import com.thoughtworks.binding.Binding.Var
import com.thoughtworks.binding.{Binding, dom}
import com.thoughtworks.modularizer.services.GitStorageUrlConfiguration
import com.thoughtworks.modularizer.views.homepage.{ImportTab, OpenTab, Tab}
import org.scalajs.dom.Event
import org.scalajs.dom.raw.Node
import typings.stdLib.GlobalFetch

import scala.concurrent.ExecutionContext

/**
  * @author 杨博 (Yang Bo)
  */
class HomePage(implicit fetcher: GlobalFetch,
               gitStorageConfiguration: GitStorageUrlConfiguration,
               executionContext: ExecutionContext)
    extends Page {

  val importTab = new ImportTab
  val openTab = new OpenTab
  val currentTab: Var[Tab] = Var[Tab](importTab)

  val branch: Binding[Option[String]] = currentTab.flatMap(_.branchName)

  @dom
  val view: Binding[Node] = {
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
