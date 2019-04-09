package com.thoughtworks.modularizer.view.homepage

import com.thoughtworks.binding.{Binding, LatestEvent, dom}
import com.thoughtworks.modularizer.model.{JdepsGraph, PageState}
import com.thoughtworks.modularizer.model.PageState.WorkBoardState
import com.thoughtworks.modularizer.view.homepage.importtab.{BranchInputGroup, DotFileInputGroup}
import org.scalajs.dom.{Event, FileReader}
import org.scalajs.dom.raw.{HTMLFormElement, HTMLInputElement}
import typings.graphlibDashDotLib.graphlibDashDotMod

/**
  * @author 杨博 (Yang Bo)
  */
class ImportTab {

  private val dotFileInputGroup = new DotFileInputGroup
  private val branchInputGroup = new BranchInputGroup



  @dom
  val view: Binding[HTMLFormElement] = {

    <form>
      { branchInputGroup.view.bind }
      { dotFileInputGroup.view.bind }
      {
        dotFileInputGroup.loadedText.bind match {
          case None =>
            <!-- No DOT file loaded -->
          case Some(dotFileContent) =>
            <button type="submit" class="btn btn-primary" onclick={ event: Event =>
              event.preventDefault()
              val jdepsGraph = JdepsGraph(graphlibDashDotMod.^.read(dotFileContent))

              val graph = jdepsGraph.internalDependencies

              // TODO: upload to git
//              outputGraph.value = Some(graph)
//              pageState.value = PageState.WorkBoard(WorkBoardState.Summary)
            }>Import</button>
        }
      }
    </form>
  }
}
