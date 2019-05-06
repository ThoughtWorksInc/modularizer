package com.thoughtworks.modularizer.js.views.homepage.opentab

import com.thoughtworks.binding.{Binding, dom}
import com.thoughtworks.binding.bindable._
import com.thoughtworks.binding.Binding.Var
import com.thoughtworks.modularizer.js.models.JdepsGraph
import com.thoughtworks.modularizer.js.services.GitStorageUrlConfiguration
import org.scalajs.dom.raw._
import org.scalajs.dom._
import typings.graphlibDashDotLib.graphlibDashDotMod
import typings.graphlibLib.graphlibMod
import typings.stdLib.{GlobalFetch, RequestInit, Response}
import typings.graphlibLib.graphlibMod.{Graph, GraphOptions}

import scala.concurrent.{ExecutionContext, Future}
import scala.scalajs.js
import scala.scalajs.js.{JSON, |}
import scala.util.{Failure, Success}

/**
  * @author 杨博 (Yang Bo)
  */
class OpenButtonGroup(branchName: Binding[Option[String]])(implicit fetcher: GlobalFetch,
                                                           gitStorageConfiguration: GitStorageUrlConfiguration,
                                                           executionContext: ExecutionContext) {

  val isClicked = Var(false)

  @dom
  val view: Binding[Node] = {
    branchName.bind match {
      case None =>
        <!-- No branch selected -->
      case Some(branch) =>
        <div class="form-group">
          <button
            type="submit"
            class="btn btn-primary"
            disabled={ branchName.bind.isEmpty || isClicked.bind }
            onclick={ event: Event =>
              event.preventDefault()
              isClicked.value = true
            }
          >Open</button>
        </div>
    }

  }

}
