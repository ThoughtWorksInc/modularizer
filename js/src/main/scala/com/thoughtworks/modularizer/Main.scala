package com.thoughtworks.modularizer
import com.thoughtworks.binding.Binding.BindingInstances.monadSyntax._
import com.thoughtworks.binding.Binding.Var
import com.thoughtworks.binding.Component.partialUpdate
import com.thoughtworks.binding._
import com.thoughtworks.modularizer.model.PageState
import com.thoughtworks.modularizer.model.PageState.WorkBoardState
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
    dom.render(document.body, render()) //pageState.flatMap(_.render))
  }
//    val graph: Graph = new Graph(new Anon_Compound {
//      compound = true
//    })
//
//    graph
//      .setGraph(new GraphLabel {
//        ranker = "tight-tree"
//      })
//      .setDefaultEdgeLabel { edge: Edge =>
//        Label(StringDictionary())
//      }
//      .setNode("node-1",
//               Label(
//                 StringDictionary(
//                   "label" -> "Node 1"
//                 )
//               ))
//      .setNode("node-2",
//               Label(
//                 StringDictionary(
//                   "label" -> "Node 2"
//                 )
//               ))
//      .setNode("node-3",
//               Label(
//                 StringDictionary(
//                   "label" -> "Node 3"
//                 )
//               ))
//      .setEdge("node-1", "node-2")
//      .setEdge("node-1", "node-3")
//
//
//
//  @dom
//  def htmlTemplate(graph: Graph) = {
//    val render = dagreDashD3Mod.^.render.newInstance0()
//    val svgContainer = <div></div>
//    val svgSelection = d3Mod.^.select(svgContainer).append("svg").asInstanceOf[Selection[_, _, BaseType, _]]
//    render(svgSelection, graph)
//
//    <div>
//      {svgContainer}
//      <button type="button" onclick={ event: Any =>
//        graph
//        .setNode("node-4",
//                 Label(
//                   StringDictionary(
//                     "label" -> "Node 4"
//                   )
//                 ))
//        .setEdge("node-4", "node-1")
//
//        render(svgSelection, graph)
//      }>Add Node</button>
//    </div>
//  }

}
