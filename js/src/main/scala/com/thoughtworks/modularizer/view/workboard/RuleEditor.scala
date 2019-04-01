package com.thoughtworks.modularizer.view.workboard

import com.thoughtworks.binding.Binding.BindingInstances.monadSyntax._
import com.thoughtworks.binding.Binding.{BindingSeq, Constants, Var, Vars}
import com.thoughtworks.binding.{Binding, dom}
import com.thoughtworks.modularizer.model.{ClusteringReport, ClusteringRule, DraftCluster}
import DraftCluster._
import com.thoughtworks.modularizer.util._
import com.thoughtworks.modularizer.view.workboard.ruleeditor._
import org.scalajs.dom._
import org.scalajs.dom.raw.Event

import scala.scalajs.js
import scala.scalajs.js.UndefOr

class RuleEditor(draftClusters: Vars[DraftCluster],
                 clusteringRule: Var[ClusteringRule],
                 clusteringReport: Binding[ClusteringReport]) {
  private val facadeCard: BuiltInClusterCard[Binding[js.Array[String]]] = {
    val facadeNodes = clusteringReport.map { report: ClusteringReport =>
      (report.compoundGraph.children("source"): UndefOr[js.Array[String]]).getOrElse(js.Array())
    }
    new BuiltInClusterCard(facadeNodes, "Facades", FacadeColorClass)
  }

  private val utilityCard: BuiltInClusterCard[Binding[js.Array[String]]] = {
    val utilityNodes = clusteringReport.map { report: ClusteringReport =>
      (report.compoundGraph.children("sink"): UndefOr[js.Array[String]]).getOrElse(js.Array())
    }
    new BuiltInClusterCard(utilityNodes, "Utilities", UtilityColorClass)
  }

  private val unassignedCard: UnassignedCard[Binding[js.Array[String]]] = {
    val unassignedNodes = clusteringReport.map { report: ClusteringReport =>
      def isUnassigned(nodeId: String) = {
        (report.compoundGraph.children(nodeId): UndefOr[js.Array[String]])
          .fold(true)(_.isEmpty) && report.compoundGraph.parent(nodeId).isEmpty
      }
      for {
        nodeId <- report.compoundGraph.nodes()
        if isUnassigned(nodeId)
      } yield nodeId
    }
    new UnassignedCard(unassignedNodes, draftClusters, clusteringRule)
  }

  private val customClusterCards: BindingSeq[CustomClusterCard] = for (draftCluster <- draftClusters) yield {
    new CustomClusterCard(draftClusters, clusteringReport, draftCluster)
  }

  val selectedNodeIds: BindingSeq[String] =
    Constants(
      facadeCard.selectedUnlockedNodeIds,
      utilityCard.selectedUnlockedNodeIds,
      unassignedCard.selectedUnlockedNodeIds,
      customClusterCards.flatMap { card: CustomClusterCard =>
        Constants(card.selectedLockedNodeIds, card.selectedUnlockedNodeIds).flatMap(identity)
      }
    ).flatMap(identity)

  @dom
  val view: Binding[Node] = <div class="flex-shrink-1 col-auto">
    { facadeCard.view.bind }
    { utilityCard.view.bind }
    { customClusterCards.mapBinding(_.view) }
    <form class="m-2">
      <div class="input-group">
        {
          val nextColor = Binding {
            val clusterColorHistogram =
              draftClusters.all.bind
                .groupBy(_.color.value)
                .mapValues(_.size)
                .withDefaultValue(0)
             DraftCluster.CustomClusterColors.minBy(clusterColorHistogram)
          }
          <div class="input-group-prepend">
            <label
              class="input-group-text"
              style:color={nextColor.bind.textColor}
              style:backgroundColor={nextColor.bind.backgroundColor}
            >New Cluster</label>
          </div>
          <input
            id="clusterName"
            type="text"
            class="form-control"
          />
          <div class="input-group-append">
            <button type="submit" class="btn btn-secondary" onclick={ event: Event =>
              event.preventDefault()
              val clusterColorHistogram =
                draftClusters.value
                  .groupBy(_.color.value)
                  .mapValues(_.size)
                  .withDefaultValue(0)
              draftClusters.value += DraftCluster(Var(clusterName.value), Vars.empty, Var(nextColor.value))
              clusterName.value = ""
            }>
              <span title="Add" class="fas fa-folder-plus"></span>
            </button>
          </div>
        }
      </div>
    </form>
    { unassignedCard.view.bind }
  </div>
}
