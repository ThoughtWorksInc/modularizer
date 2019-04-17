package com.thoughtworks.modularizer
import com.thoughtworks.modularizer.utilities._
import com.thoughtworks.binding.Binding.BindingInstances.monadSyntax._
import com.thoughtworks.binding.Binding.Var
import com.thoughtworks.binding.Component.partialUpdate
import com.thoughtworks.binding._
import com.thoughtworks.modularizer.views._
import com.thoughtworks.modularizer.models.PageState
import com.thoughtworks.modularizer.models.PageState.WorkBoardState
import com.thoughtworks.modularizer.services.GitStorageUrlConfiguration
import com.thoughtworks.modularizer.views.{HomePage, WorkBoard}
import org.scalajs.dom._
import org.scalajs.dom.raw.Node
import typings.graphlibLib.graphlibMod.Graph
import typings.stdLib
import typings.stdLib.Window
import io.lemonlabs.uri.Url
import scala.concurrent.ExecutionContext
import scala.util.Failure
import scala.util.Success
import shapeless._
import io.lemonlabs.uri.PathParts
import io.lemonlabs.uri.Path
import scala.concurrent.Future
import typings.stdLib.GlobalFetch

/**
  * @author 杨博 (Yang Bo)
  */
object Main {

  class RootView(implicit fetcher: GlobalFetch,
                 gitStorageConfiguration: GitStorageUrlConfiguration,
                 executionContext: ExecutionContext) {

    val route = new com.thoughtworks.modularizer.utilities.HashRoute

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

  def main(args: Array[String]): Unit = {
    implicit val gitStorageUrlConfiguration = new GitStorageUrlConfiguration
    implicit val fetcher = stdLib.^.window
    import ExecutionContext.Implicits.global

    val rootView = new RootView
    dom.render(document.body, rootView.view)
  }

}
