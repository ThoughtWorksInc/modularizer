package com.thoughtworks.modularizer
import util._
import com.thoughtworks.binding.Binding.{BindingSeq, Constants, Var, Vars}
import com.thoughtworks.binding.Binding.BindingInstances.monadSyntax._
import com.thoughtworks.binding.bindable._
import com.thoughtworks.binding.{Binding, LatestEvent, dom}
import com.thoughtworks.modularizer.model.{ClusteringReport, DraftCluster}
import org.scalajs.dom._
import raw.{Event, HTMLOptionElement, HTMLSelectElement}
import typings.graphlibLib.graphlibMod.Graph

import scala.scalajs.js
import scala.scalajs.js.UndefOr

/**
  * @author 杨博 (Yang Bo)
  */
object RuleEditor {

  def multipleSelect[Items: BindableSeq.Lt[?, String]](
      items: Items
  ): (Binding[HTMLSelectElement], BindingSeq[String]) = {
    @dom
    def option(text: String): Binding[HTMLOptionElement] = <option value={text}>{ text }</option>

    val options: BindingSeq[HTMLOptionElement] = items.bindSeq.mapBinding(option)

    @dom
    val view: Binding[HTMLSelectElement] =
      <select
        classMap={Map(
          "custom-select" -> true,
          "d-none" -> items.bindSeq.isEmpty.bind
        )}
        selectedIndex={-1}
        multiple="multiple"
        size={items.bindSeq.length.bind}
      >{ options.bind }</select>

    val selectedItems = for {
      option <- options
      if {
        val _ = LatestEvent.change(view.bind).bind
        option.selected
      }
    } yield option.value

    view -> selectedItems
  }

  def builtInCluster[Items: BindableSeq.Lt[?, String]](items: Items,
                                                       clusterName: String,
                                                       labelBgClass: String,
                                                       labelTextClass: String): (Binding[Node], BindingSeq[String]) = {
    val unlockedNodes = items.bindSeq
    val (selectElement, selectedItems) = multipleSelect(unlockedNodes)

    @dom def card =
      <div class="card m-2">
        <div class="input-group">
          <div class="input-group-prepend">
            <label class={s"input-group-text $labelBgClass $labelTextClass"}>Built-in Cluster</label>
          </div>
          <input
            type="text"
            readOnly="readOnly"
            class="form-control"
            value={ clusterName }
          />
        </div>
        <div class="card-body">
          <details>
            <summary>
              Unlocked Nodes
              <span class="fas fa-unlock"></span>
              <span class="badge badge-info">{ unlockedNodes.length.bind.toString }</span>
            </summary>
            { selectElement.bind }
          </details>
        </div>
      </div>
    card -> selectedItems
  }

  @dom
  def render(draftClusters: Vars[DraftCluster], clusteringReport: Binding[ClusteringReport]) = {
    val facadeNodes = clusteringReport.map { report =>
      (report.clusteringGraph.children("source"): UndefOr[js.Array[String]]).getOrElse(js.Array())
    }
    val utilityNodes = clusteringReport.map { report =>
      (report.clusteringGraph.children("sink"): UndefOr[js.Array[String]]).getOrElse(js.Array())
    }
    val unassignedNodes = clusteringReport.map { report =>
      def isUnassigned(nodeId: String) = {
        (report.clusteringGraph.children(nodeId): UndefOr[js.Array[String]])
          .fold(true)(_.isEmpty) && report.clusteringGraph.parent(nodeId).isEmpty
      }
      for {
        nodeId <- report.clusteringGraph.nodes()
        if isUnassigned(nodeId)
      } yield nodeId
    }
    val (facadeCard, selectedFacades) = builtInCluster(facadeNodes, "Facades", "bg-dark", "text-light")
    val (utilityCard, selectedUtilities) = builtInCluster(utilityNodes, "Utilities", "bg-light", "text-dark")
    val (unassignedCard, selectedUnassignedNodes) =
      builtInCluster(unassignedNodes, "Unassigned", "bg-gray", "text-dark")

    <div class="flex-shrink-1 col-auto">
      { facadeCard.bind }
      { utilityCard.bind }
      { unassignedCard.bind }
      {
        for (draftCluster <- draftClusters) yield {
          <div class="card m-2">
            <div class="input-group">
              <div class="input-group-prepend">
                <label
                  class="input-group-text text-light"
                  style:backgroundColor={draftCluster.color.bind}
                >Cluster</label>
              </div>
              <input
                id="clusterName"
                type="text"
                class="form-control"
                value={ draftCluster.name.bind }
                onchange={_: Event =>
                  draftCluster.name.value = clusterName.value
                }
              />
              <div class="input-group-append">
                <button type="button" class="btn btn-secondary" onclick={ _: Event =>
                  draftClusters.value -= draftCluster
                }>Delete</button>
              </div>
            </div>
            <div class="card-body">
              <details id="nodesDetails">
                <summary>
                  Locked Nodes
                  <span class="fas fa-lock"></span>
                  <span class="badge badge-info">{
                    draftCluster.nodeIds.length.bind.toString
                  }</span>
                  {
                    if (Binding {
                      val latestToggleEvent = new LatestEvent[Event](nodesDetails, "toggle").bind
                      val latestChangeEvent = new LatestEvent[Event](selectedNodeIds, "change").bind
                      val numberOfNodes = draftCluster.nodeIds.length.bind
                      nodesDetails.asInstanceOf[js.Dynamic].open.asInstanceOf[Boolean] && selectedNodeIds.selectedIndex != -1
                    }.bind) {
                      <button type="button" class="float-right badge badge-danger" onclick={ _: Event =>
                        for (option <- selectedNodeIds.options.toArray) {
                          if (option.selected) {
                            draftCluster.nodeIds.value -= option.value
                          }
                        }
                      }>Remove Selected</button>
                    } else {
                      <!-- Hidden Remove Button -->
                    }
                  }
                </summary>
                <select
                  id="selectedNodeIds"
                  classMap={
                    Map(
                      "custom-select" -> true,
                      "d-none" -> draftCluster.nodeIds.isEmpty.bind
                    )
                  }
                  selectedIndex={-1}
                  multiple="multiple"
                  size={draftCluster.nodeIds.length.bind}
                >{
                  for (nodeId <- draftCluster.nodeIds) yield <option value={nodeId}>{ nodeId }</option>
                }</select>
              </details>
              <details>{
                val allChildNodes = UndefOr.any2undefOrA(clusteringReport.bind.clusteringGraph.children(draftCluster.name.value)).getOrElse(js.Array())
                val unlockedNodes = allChildNodes -- draftCluster.nodeIds.all.bind
                <summary>
                  Unlocked Nodes
                  <span class="fas fa-unlock"></span>
                  <span class="badge badge-info">{
                    unlockedNodes.length.toString
                  }</span>
                </summary>
                <select
                  id="selectedUnlockedNodeIds"
                  classMap={Map(
                    "custom-select" -> true,
                    "d-none" -> unlockedNodes.isEmpty
                  )}
                  selectedIndex={-1}
                  multiple="multiple"
                  size={unlockedNodes.length}
                >{
                  for (nodeId <- Constants(unlockedNodes: _*)) yield <option value={nodeId}>{ nodeId }</option>
                }</select>
              }</details>
            </div>
          </div>
        }
      }
      <form class="m-2">
        <div class="input-group">
          <div class="input-group-prepend">
            <label class="input-group-text">New Cluster</label>
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
              val nextColor = DraftCluster.ClusterColors.minBy(clusterColorHistogram)

              draftClusters.value += DraftCluster(Var(clusterName.value), Vars.empty, Var(nextColor))
              clusterName.value = ""
            }>Add</button>
          </div>
        </div>
      </form>
    </div>

  }

}
