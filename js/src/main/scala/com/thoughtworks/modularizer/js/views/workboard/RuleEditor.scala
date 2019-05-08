package com.thoughtworks.modularizer.js.views.workboard

import com.thoughtworks.binding.Binding.{BindingSeq, Constants, Var, Vars}
import com.thoughtworks.binding.{Binding, LatestEvent, dom}
import com.thoughtworks.modularizer.js.models.{BuiltInClusterId, ClusteringRule, DraftCluster}
import DraftCluster._
import com.thoughtworks.modularizer.js.services.ClusteringService
import com.thoughtworks.modularizer.js.utilities._
import com.thoughtworks.modularizer.js.views.workboard.ruleeditor._
import org.scalajs.dom._
import org.scalajs.dom.raw.Event

// TODO in-place dependency explorer
class RuleEditor(draftClusters: Vars[DraftCluster],
                 clusteringRule: Var[ClusteringRule],
                 clusteringService: ClusteringService) {
  private val facadeCard = {
    new BuiltInClusterCard(clusteringService.children(BuiltInClusterId.Facade),
                           BuiltInClusterId.Facade,
                           FacadeColorClass)
  }

  private val utilityCard = {
    new BuiltInClusterCard(clusteringService.children(BuiltInClusterId.Utility),
                           BuiltInClusterId.Utility,
                           UtilityColorClass)
  }

  private val unassignedCard = {
    new UnassignedCard(clusteringService.unassignedNodeIds, draftClusters, clusteringRule)
  }

  private val customClusterCards: BindingSeq[CustomClusterCard] = for (draftCluster <- draftClusters) yield {
    new CustomClusterCard(draftClusters, clusteringService, draftCluster)
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
