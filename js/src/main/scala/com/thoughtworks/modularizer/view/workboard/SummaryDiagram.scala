package com.thoughtworks.modularizer.view.workboard
import com.thoughtworks.binding.Binding._
import com.thoughtworks.binding.{Binding, dom}
import com.thoughtworks.modularizer.model.{ClusteringReport, ClusteringRule}
import org.scalablytyped.runtime.StringDictionary
import org.scalajs.dom.raw.Node
import org.scalajs.dom.window
import typings.d3DashSelectionLib.d3DashSelectionMod.{BaseType, Selection}
import typings.d3Lib.d3Mod
import typings.dagreDashD3Lib.dagreDashD3Mod
import typings.dagreLib.Anon_Compound
import typings.dagreLib.dagreMod.graphlibNs.{Graph => GraphD3}
import typings.dagreLib.dagreMod.{GraphLabel, Label}
import typings.graphlibLib.graphlibMod.{Graph, algNs}

private object SummaryDiagram {
  final class ClusterMountPoint(graphD3: GraphD3, cluster: Binding[String]) extends SingleMountPoint[String](cluster) {

    private var clusterNameOption: Option[String] = None

    override def unmount() = {
      clusterNameOption.foreach(graphD3.removeNode)
      clusterNameOption = None
      super.unmount()
    }

    def set(clusterName: String): Unit = {
      clusterNameOption.foreach(graphD3.removeNode)
      graphD3.setNode(clusterName,
                      Label(
                        // TODO:
                      ))
      this.clusterNameOption = Some(clusterName)
    }
  }
}

/**
  * @author 杨博 (Yang Bo)
  */
class SummaryDiagram(simpleGraph: Graph,
                     clusteringRule: Var[ClusteringRule],
                     clusteringReport: Binding[ClusteringReport]) {

  @dom
  val view: Binding[Node] = {
    val render = dagreDashD3Mod.^.render.newInstance0()
    val svgContainer = <div class="svg-container"></div>
    val svgSelection = d3Mod.^.select(svgContainer)
      .append("svg")
      .attr("preserveAspectRatio", "xMinYMin meet")
      .attr("viewBox", "0 0 400 600")
      .classed("svg-content-responsive", true)
      .asInstanceOf[Selection[_, _, BaseType, _]]
    val g = buildGraphD3.bind
    window.requestAnimationFrame { _ =>
      render(svgSelection, g)
    }

    <div class="flex-grow-1 col-auto">
      {svgContainer}
    </div>
  }

  def buildGraphD3: Binding[GraphD3] = Binding {
    val report = clusteringReport.bind
    val clusters = clusteringRule.bind.clusters

    val g =
      new GraphD3(new Anon_Compound {
//        compound = true
        directed = true
        multigraph = false
      })

    g.setGraph(new GraphLabel {
//      compound = true
    })

    g.setNode(
      "sink",
      Label(
        StringDictionary(
          "label" -> "Utilities"
        )
      )
    )
    g.setNode(
      "source",
      Label(
        StringDictionary(
          "label" -> "Facades"
        )
      )
    )
    for (cluster <- clusters) {
      g.setNode(
        cluster.parent,
        Label(
          StringDictionary(
            "label" -> cluster.parent,
//            "clusterLabelPos" -> "top",
//            "style" -> "fill: #ffd47f",
          )
        )
      )
//      for (child <- cluster.children) {
//        g.setNode(child, Label(StringDictionary("label" -> child)))
//        g.setParent(child, cluster.parent)
//      }
    }
    g.setDefaultEdgeLabel { edge =>
      Label(
        StringDictionary(
          "curve" -> d3Mod.^.curveBasis
        )
        // TODO:
      )
    }
    ClusteringReport
      .findNearestClusters(report.dependencyPaths, report.clusterIds, "source")
      .foreach(g.setEdge("source", _))
    ClusteringReport
      .findNearestClusters(report.dependentPaths, report.clusterIds, "sink")
      .foreach(g.setEdge(_, "sink"))

    for (from <- clusters) {
      val dependencyClusterIds = ClusteringReport.findNearestClusters(report.dependencyPaths, clusters.collect {
        case to if to.parent != from.parent =>
          to.parent
      }, from.parent)

      for (dependencyClusterId <- dependencyClusterIds) {
        g.setEdge(from.parent, dependencyClusterId)
      }
    }

//
//    for (draftCluster <- draftClusters) {
//      new ClusterMountPoint(g, draftCluster.name).bind
//      val originalName = draftCluster.name.value // FIXME: avoid .value
//
//      val dependencies = report.dependencyPaths(originalName)
//
//      ClusteringReport.findNearestClusters(report.dependencyPaths, draftClusters.all.bind.collect {
//        case cluster if cluster.name.value != originalName =>
//          cluster.name.value
//      }, originalName)
////      ClusteringReport.findNearestCluster()
////      for (otherCluster <- draftClusters) {
////        if (otherCluster.name.value != originalName && !dependencies(otherCluster.name.value).distance.isInfinite) {
////
////        }
////      }
//
//    }

//    draftClusters

    //.setNode("", Label())

    g
  }

}
