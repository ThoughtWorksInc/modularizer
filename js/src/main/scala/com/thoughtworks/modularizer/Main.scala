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

    val homePage = new HomePage

    val route = new com.thoughtworks.modularizer.utilities.HashRoute

    val currentState = route { previousState =>
      Binding {
        page.bind match {
          case homePage: HomePage =>
            (homePage.result.bind, homePage.branch.bind) match {
              case (Some(graph), Some(branch)) =>
                "work-board/"
              case _ =>
                ""
            }
          case workBoard: WorkBoard =>
            "work-board/" + workBoard.nextState.bind
        }
      }
    }

    val page: Binding[Page] = Binding {
      val hash = Url.parse(currentState.bind) // TODO: 实现一套 UrlBinding
      hash match {
        case Url(PathParts("work-board", rest @ _*), queryString, None) =>
          (homePage.result.bind, homePage.branch.bind) match {
            case (Some(graph), Some(branch)) =>
              new WorkBoard(graph, branch)
            case _ =>
              // <div class="alert alert-warning" data:role="alert">
              //   <p>
              //     No graph found. You may want to import a <kbd>jdeps</kbd> report first.
              //   </p>
              //   <hr/>
              //   <button type="button" class="btn btn-primary" onclick={ _: Event =>
              //     pageState.value = PageState.HomePage
              //   }>Import</button>
              // </div>
              homePage
          }
        case _ =>
          homePage
      }
    }

    val view = {
      page.flatMap(_.view)
    }

  }

  // @dom
  // def render() = {
  //   val gitStorageUrlConfiguration = new GitStorageUrlConfiguration
  //   val graphOption: Var[Option[Graph]] = Var(None)
  //   val pageState = Var[PageState](PageState.HomePage)
  //   val () = new JsonHashRoute(pageState).bind

  //   val renderHomePage: Component[Unit, Node] = { _ =>
  //     new HomePage()(stdLib.^.window, gitStorageUrlConfiguration, ExecutionContext.global, pageState).view
  //   }
  //   val renderWorkBoard: Component[WorkBoardState, Node] = { workBoardState =>
  //     WorkBoard.render(pageState, workBoardState, graphOption)
  //   }
  //   partialUpdate(pageState.map {
  //     case PageState.HomePage =>
  //       renderHomePage(())
  //     case PageState.WorkBoard(graphState) =>
  //       renderWorkBoard(graphState)
  //   }).bind
  // }

  def main(args: Array[String]): Unit = {
    implicit val gitStorageUrlConfiguration = new GitStorageUrlConfiguration
    implicit val fetcher = stdLib.^.window
    import ExecutionContext.Implicits.global

    val rootView = new RootView
    dom.render(document.body, rootView.view)
  }

}
