package com.thoughtworks.modularizer.views.workboard
import com.thoughtworks.binding.Binding._
import com.thoughtworks.binding.dom.Runtime.TagsAndTags2
import com.thoughtworks.binding.{Binding, dom}
import com.thoughtworks.modularizer.models.{ClusteringReport, ClusteringRule, DraftCluster}
import org.scalablytyped.runtime.StringDictionary
import org.scalajs.dom.raw.{Node, SVGPreserveAspectRatio}
import org.scalajs.dom.{Event, window}
import typings.d3DashSelectionLib.d3DashSelectionMod.{BaseType, Selection}
import typings.d3Lib.d3Mod
import typings.dagreDashD3Lib.dagreDashD3Mod
import typings.dagreLib.Anon_Compound
import typings.dagreLib.dagreMod.graphlibNs.{Graph => GraphD3}
import typings.dagreLib.dagreMod.{GraphLabel, Label}
import typings.graphlibLib.graphlibMod.{Graph, algNs}
import typings.graphlibLib.graphlibMod.Path
import com.thoughtworks.modularizer.utilities._
import scalatags.JsDom

import scala.annotation.tailrec
import scala.collection.immutable

/**
  * @author 杨博 (Yang Bo)
  */
class SummaryDiagram(simpleGraph: Graph,
                     draftClusters: Vars[DraftCluster],
                     clusteringRule: Var[ClusteringRule],
                     clusteringReport: Binding[ClusteringReport]) {

  import org.scalajs.dom.svg
  @dom
  val view: Binding[Node] = {
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
      {
        val svg = <svg
          style:width="100%" style:height="100%"
          preserveAspectRatio:baseVal:align={SVGPreserveAspectRatio.SVG_PRESERVEASPECTRATIO_XMIDYMID}
          preserveAspectRatio:baseVal:meetOrSlice={SVGPreserveAspectRatio.SVG_MEETORSLICE_MEET}
        ></svg>
        val render = dagreDashD3Mod.^.render.newInstance0()
        render(d3Mod.^.select(svg).asInstanceOf[Selection[_, _, BaseType, _]], buildGraphD3.bind)
        val boundBox = svg.getBBox()
        svg.viewBox.baseVal.x = boundBox.x
        svg.viewBox.baseVal.y = boundBox.y
        svg.viewBox.baseVal.width = boundBox.width
        svg.viewBox.baseVal.height = boundBox.height
        svg
      }
    </div>
  }

  def buildGraphD3: Binding[GraphD3] = Binding {
    val report = clusteringReport.bind
    val clusters = clusteringRule.bind.clusters

    val g =
      new GraphD3(new Anon_Compound {
        compound = true
        directed = true
        multigraph = false
      })

    g.setGraph(new GraphLabel {
      compound = true
    })

    g.setNode(
      "Utilities",
      Label(
        StringDictionary(
          "label" -> "",
          "style" -> "fill: #ffd47f" // TODO: add color property on ClusteringRule
        )
      )
    )
    g.setNode(
      "label_Utilities",
      Label(
        StringDictionary(
          "rank" -> "max",
          "label" -> "Utilities",
//            "clusterLabelPos" -> "top",
          "style" -> "stroke: none; fill-opacity: 0"
        )
      )
    )
    g.setParent("label_Utilities", "Utilities")

    g.setNode(
      "Facades",
      Label(
        StringDictionary(
          "label" -> "",
          "style" -> "fill: #ffd47f" // TODO: add color property on ClusteringRule
        )
      )
    )

    g.setNode(
      "label_Facades",
      Label(
        StringDictionary(
          "rank" -> "max",
          "label" -> "Facades",
//            "clusterLabelPos" -> "top",
          "style" -> "stroke: none; fill-opacity: 0"
        )
      )
    )
    g.setParent("label_Facades", "Facades")
    for (cluster <- clusters) {
      val id = cluster.parent
      g.setNode(
        id,
        Label(
          StringDictionary(
            "label" -> "",
//            "clusterLabelPos" -> "top",
            "style" -> "fill: #ffd47f" // TODO: add color property on ClusteringRule
          )
        )
      )

      val labelId = s"label_$id"
      g.setNode(
        labelId,
        Label(
          StringDictionary(
            "rank" -> "max",
            "label" -> id,
//            "clusterLabelPos" -> "top",
            "style" -> "stroke: none; fill-opacity: 0"
          )
        )
      )
      g.setParent(labelId, id)

//      for (child <- cluster.children) {
//        g.setNode(child, Label(StringDictionary("label" -> child)))
//        g.setParent(child, cluster.parent)
//      }
    }
//
//    g.setNode("xxx",
//              Label(
//                StringDictionary(
//                  "label" -> "xxx"
//                )
//              ))
//
//    g.setParent("xxx", "a")
//
    g.setDefaultEdgeLabel { edge =>
      Label(
        StringDictionary(
          "curve" -> d3Mod.^.curveBasis
        )
        // TODO:
      )
    }

    def addKeyPath(paths: StringDictionary[StringDictionary[Path]],
                   from: String,
                   clusterId: String,
                   setEdge: (String, String) => Unit): Unit = {
      val distancesToCluster = paths(clusterId)

      @tailrec
      def loop(from: String): Unit = {
        if (from != clusterId) {
          val predecessor = distancesToCluster(from).predecessor
          val currentParent = report.compoundGraph.parent(from)
          if (currentParent.isDefined) {
            g.setNode(from,
                      Label(
                        StringDictionary(
                          "label" -> from,
                          //            "clusterLabelPos" -> "top",
                          //            "style" -> "fill: #ffd47f",
                        )
                      ))
            g.setParent(from, currentParent.get)
            if (report.compoundGraph.parent(predecessor).isDefined) {
              setEdge(predecessor, from)
            }
          }
          loop(predecessor)
        }
      }

      loop(from)
    }

    for (clusterIdUsedByFacades <- ClusteringReport
           .findNearestClusters(report.dependentPaths, report.clusterIds :+ "Utilities", "Facades")) {
      addKeyPath(report.dependentPaths, "Facades", clusterIdUsedByFacades, { (from, to) =>
        g.setEdge(from, to)
      })
    }

    for (clusterIdThatDependsOnUtilities <- ClusteringReport
           .findNearestClusters(report.dependencyPaths, report.clusterIds, "Utilities")) {
      addKeyPath(report.dependencyPaths, "Utilities", clusterIdThatDependsOnUtilities, { (to, from) =>
        g.setEdge(from, to)
      })
    }

    for (from <- clusters) {
      val dependencyClusterIds = ClusteringReport.findNearestClusters(report.dependentPaths, clusters.collect {
        case to if to.parent != from.parent =>
          to.parent
      }, from.parent)

      for (dependencyClusterId <- dependencyClusterIds) {
        addKeyPath(report.dependentPaths, from.parent, dependencyClusterId, { (from, to) =>
          g.setEdge(from, to)
        })
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
