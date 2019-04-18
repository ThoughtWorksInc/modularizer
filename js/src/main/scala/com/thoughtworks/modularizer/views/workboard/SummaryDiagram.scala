package com.thoughtworks.modularizer.views.workboard
import com.thoughtworks.binding.Binding._
import com.thoughtworks.binding.{Binding, dom}
import com.thoughtworks.modularizer.models.{ClusteringReport, ClusteringRule, DraftCluster}
import org.scalablytyped.runtime.StringDictionary
import org.scalajs.dom.raw.Node
import org.scalajs.dom.{Event, window}
import typings.d3DashSelectionLib.d3DashSelectionMod.{BaseType, Selection}
import typings.d3Lib.d3Mod
import typings.dagreDashD3Lib.dagreDashD3Mod
import typings.dagreLib.Anon_Compound
import typings.dagreLib.dagreMod.graphlibNs.{Graph => GraphD3}
import typings.dagreLib.dagreMod.{GraphLabel, Label}
import typings.graphlibLib.graphlibMod.{Graph, algNs}

import scala.collection.immutable

private object SummaryDiagram {
  final class ClusterMountPoint(graphD3: GraphD3, cluster: Binding[String]) extends SingleMountPoint[String](cluster) {

    private var clusterNameOption: Option[String] = None

    override def unmount(): Unit = {
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
                     draftClusters: Vars[DraftCluster],
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
      <button
        type="button"
        class="btn btn-primary position-absolute"
        style:bottom="2em"
        style:right="2em"
        onclick={ _: Event=>
          clusteringRule.value = ClusteringRule(Set.empty, draftClusters.value.view.map(_.buildCluster).to[immutable.Seq])
        }
      ><span class="fas fa-save"></span></button>
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
      "Utilities",
      Label(
        StringDictionary(
          "label" -> "Utilities"
        )
      )
    )
    g.setNode(
      "Facades",
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
      .findNearestClusters(report.dependentPaths, report.clusterIds :+ "Utilities", "Facades")
      .foreach(g.setEdge("Facades", _))
    ClusteringReport
      .findNearestClusters(report.dependencyPaths, report.clusterIds, "Utilities")
      .foreach(g.setEdge(_, "Utilities"))

    for (from <- clusters) {
      val dependencyClusterIds = ClusteringReport.findNearestClusters(report.dependentPaths, clusters.collect {
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
