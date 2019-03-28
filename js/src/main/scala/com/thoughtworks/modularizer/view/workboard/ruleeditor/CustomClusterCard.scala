package com.thoughtworks.modularizer.view.workboard.ruleeditor

import com.thoughtworks.binding.Binding.{BindingSeq, Vars}
import com.thoughtworks.binding.Binding.BindingInstances.monadSyntax._
import com.thoughtworks.binding.{Binding, LatestEvent, dom}
import com.thoughtworks.modularizer.model.{ClusteringReport, DraftCluster}
import org.scalajs.dom._
import org.scalajs.dom.raw.Event

import scala.scalajs.js
import scala.scalajs.js.UndefOr

/**
  * @author 杨博 (Yang Bo)
  */
class CustomClusterCard(draftClusters: Vars[DraftCluster],
                        clusteringReport: Binding[ClusteringReport],
                        draftCluster: DraftCluster) {

  private val selectLocked = new MultipleSelect(draftCluster.nodeIds)
  private val selectUnlocked = new MultipleSelect(Binding {
    val allChildNodes = UndefOr
      .any2undefOrA(clusteringReport.bind.clusteringGraph.children(draftCluster.name.value))
      .getOrElse(js.Array())
    allChildNodes -- draftCluster.nodeIds.all.bind
  })

  def selectedLockedNodeIds: BindingSeq[String] = selectLocked.selectedNodeIds

  def selectedUnlockedNodeIds: BindingSeq[String] = selectUnlocked.selectedNodeIds

  @dom
  val view: Binding[Node] = {
    <div class="card m-2">
      <div class="input-group sticky-top">
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
        <details>
          <summary class="bg-white position-sticky" style:top="2em">
            Locked Nodes
            <span class="fas fa-lock"></span>
            <span class="badge badge-info">{
              draftCluster.nodeIds.length.bind.toString
            }</span>
            {
              if (selectLocked.selectedNodeIds.nonEmpty.bind) {
                <button type="button" class="float-right badge badge-danger" onclick={ _: Event =>
                  draftCluster.nodeIds.value --= selectLocked.selectedNodeIds.value
                }>Remove Selected</button>
              } else {
                <!-- Hidden Remove Button -->
              }
            }
          </summary>
          { selectLocked.view.bind }
        </details>
        <details id="unlockedDetails">
          <summary class="bg-white position-sticky" style:top="2em">
            Unlocked Nodes
            <span class="fas fa-unlock"></span>
            <span class="badge badge-info">{
              selectUnlocked.items.bind.length.toString
            }</span>
          </summary>
          { selectUnlocked.view.bind }
        </details>
      </div>
    </div>
  }
}
