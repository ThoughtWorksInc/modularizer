package com.thoughtworks.modularizer.views.workboard

import com.thoughtworks.binding.Binding.BindingInstances.monadSyntax._
import com.thoughtworks.binding.Binding.{BindingSeq, Constants, Var, Vars}
import com.thoughtworks.binding.{Binding, LatestEvent, dom}
import com.thoughtworks.modularizer.models.{ClusteringReport, ClusteringRule, DraftCluster}
import DraftCluster._
import com.thoughtworks.modularizer.utilities._
import com.thoughtworks.modularizer.views.workboard.ruleeditor._
import org.scalajs.dom._
import org.scalajs.dom.raw.Event

import scala.scalajs.js
import scala.scalajs.js.UndefOr

class RuleEditor(draftClusters: Vars[DraftCluster],
                 clusteringRule: Var[ClusteringRule],
                 clusteringReport: Binding[ClusteringReport]) {
  private val facadeCard: BuiltInClusterCard[Binding[js.Array[String]]] = {
    val facadeNodes = clusteringReport.map { report: ClusteringReport =>
      (report.compoundGraph.children("Facades"): UndefOr[js.Array[String]]).getOrElse(js.Array())
    }
    new BuiltInClusterCard(facadeNodes, "Facades", FacadeColorClass)
  }

  private val utilityCard: BuiltInClusterCard[Binding[js.Array[String]]] = {
    val utilityNodes = clusteringReport.map { report: ClusteringReport =>
      (report.compoundGraph.children("Utilities"): UndefOr[js.Array[String]]).getOrElse(js.Array())
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
  val view: Binding[Node] = <div class="col-3" style:overflowY="auto">
    <form class="my-2">
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
            <button
              type="submit" class="btn btn-secondary"
              disabled={
                locally(LatestEvent.input(clusterName).bind)
                locally(draftClusters.length.bind)
                clusterName.value.isEmpty
              }
              onclick={ event: Event =>
                event.preventDefault()
                val clusterColorHistogram =
                  draftClusters.value
                    .groupBy(_.color.value)
                    .mapValues(_.size)
                    .withDefaultValue(0)
                val initalName = clusterName.value
                clusterName.value = ""
                draftClusters.value.prepend(DraftCluster(Var(initalName), Vars.empty, Var(nextColor.value)))
              }
            >
              <span title="Add" class="fas fa-folder-plus"></span>
            </button>
          </div>
        }
      </div>
    </form>
    { customClusterCards.mapBinding(_.view) }
    { facadeCard.view.bind }
    { utilityCard.view.bind }
    { unassignedCard.view.bind }
  </div>
}
