package com.thoughtworks.modularizer.js
import com.thoughtworks.binding.Binding.BindingInstances.monadSyntax._
import com.thoughtworks.binding._
import com.thoughtworks.modularizer.js.services.GitStorageUrlConfiguration
import com.thoughtworks.modularizer.js.views.{HomePage, WorkBoard, _}
import io.lemonlabs.uri.{PathParts, Url}
import org.scalajs.dom._
import typings.stdLib
import typings.stdLib.GlobalFetch

import scala.concurrent.ExecutionContext

final class Main(implicit fetcher: GlobalFetch,
                 gitStorageConfiguration: GitStorageUrlConfiguration,
                 executionContext: ExecutionContext) {

  val route = new com.thoughtworks.modularizer.js.utilities.HashRoute

  val currentState = route {
    case () =>
      Binding {
        page.bind match {
          case homePage: HomePage =>
            homePage.branch.bind match {
              case Some(branch) =>
                s"work-board/$branch"
              case _ =>
                ""
            }
          case workBoard: WorkBoard =>
            s"work-board/${workBoard.branch}"
        }
      }
  }

  val page: Binding[Page] = Binding {
    val hash = Url.parse(currentState.bind) // TODO: 实现一套 UrlBinding
    hash match {
      case Url(PathParts("work-board", branch, rest @ _*), queryString, None) =>
        new WorkBoard(branch)
      case _ =>
        new HomePage
    }
  }

  val view = {
    page.flatMap(_.view)
  }

}

/**
  * @author 杨博 (Yang Bo)
  */
object Main {

  def main(args: Array[String]): Unit = {
    typings.bootstrapLib.bootstrapLibRequire
    document.title = s"Modularizer ${BuildInfo.version}"
    implicit val gitStorageUrlConfiguration = new GitStorageUrlConfiguration
    implicit val fetcher = stdLib.^.window
    import ExecutionContext.Implicits.global

    val rootView = new Main
    dom.render(document.body, rootView.view)
  }

}
