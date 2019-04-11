package com.thoughtworks.modularizer
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

import scala.concurrent.ExecutionContext

/**
  * @author 杨博 (Yang Bo)
  */
object Main {

  @dom
  def render() = {
    val gitStorageUrlConfiguration = new GitStorageUrlConfiguration
    val graphOption: Var[Option[Graph]] = Var(None)
    val pageState = Var[PageState](PageState.HomePage)
    val () = new JsonHashRoute(pageState).bind

    val renderHomePage: Component[Unit, Node] = { _ =>
      new HomePage(pageState, graphOption)(stdLib.^.window, gitStorageUrlConfiguration, ExecutionContext.global).view
    }
    val renderWorkBoard: Component[WorkBoardState, Node] = { workBoardState =>
      WorkBoard.render(pageState, workBoardState, graphOption)
    }
    partialUpdate(pageState.map {
      case PageState.HomePage =>
        renderHomePage(())
      case PageState.WorkBoard(graphState) =>
        renderWorkBoard(graphState)
    }).bind
  }

  def main(args: Array[String]): Unit = {
    dom.render(document.body, render())
  }

}
