package com.thoughtworks.modularizer
import com.thoughtworks.binding.Binding.BindingInstances.monadSyntax._
import com.thoughtworks.binding.Binding.Var
import com.thoughtworks.binding.Component.partialUpdate
import com.thoughtworks.binding._
import com.thoughtworks.modularizer.view._
import com.thoughtworks.modularizer.model.PageState
import com.thoughtworks.modularizer.model.PageState.WorkBoardState
import com.thoughtworks.modularizer.view.{ImportJdepsDotFile, WorkBoard}
import org.scalajs.dom._
import org.scalajs.dom.raw.Node
import typings.graphlibLib.graphlibMod.Graph

/**
  * @author 杨博 (Yang Bo)
  */
object Main {

  @dom
  def render() = {
    val graphOption: Var[Option[Graph]] = Var(None)
    val pageState = Var[PageState](PageState.ImportJdepsDotFile)
    val () = new JsonHashRoute(pageState).bind

    val renderImportJdepsDotFile: Component[Unit, Node] = { _ =>
      ImportJdepsDotFile.render(pageState, graphOption)
    }
    val renderWorkBoard: Component[WorkBoardState, Node] = { workBoardState =>
      WorkBoard.render(pageState, workBoardState, graphOption)
    }
    partialUpdate(pageState.map {
      case PageState.ImportJdepsDotFile =>
        renderImportJdepsDotFile(())
      case PageState.WorkBoard(graphState) =>
        renderWorkBoard(graphState)
    }).bind
  }

  def main(args: Array[String]): Unit = {
    dom.render(document.body, render())
  }

}
