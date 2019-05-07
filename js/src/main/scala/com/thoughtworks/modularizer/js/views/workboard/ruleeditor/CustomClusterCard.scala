package com.thoughtworks.modularizer.js.views.workboard.ruleeditor

import com.thoughtworks.binding.Binding.{BindingSeq, Vars}
import com.thoughtworks.binding.{Binding, dom}
import com.thoughtworks.modularizer.js.models._
import com.thoughtworks.modularizer.js.services.ClusteringService
import org.scalajs.dom._
import org.scalajs.dom.raw.{DragEffect, Event}

/**
  * @author 杨博 (Yang Bo)
  */
class CustomClusterCard(draftClusters: Vars[DraftCluster],
                        clusteringService: ClusteringService,
                        draftCluster: DraftCluster) {

  private val selectLocked = new MultipleSelect(draftCluster.nodeIds)
  private val selectUnlocked = new MultipleSelect(Binding {
    clusteringService
      .children(CustomClusterId(draftCluster.name.bind))
      .all
      .bind
      .filterNot(draftCluster.nodeIds.all.bind.toSet)
  })

  def selectedLockedNodeIds: BindingSeq[String] = selectLocked.selectedNodeIds

  def selectedUnlockedNodeIds: BindingSeq[String] = selectUnlocked.selectedNodeIds

  @dom
  val view: Binding[Node] = {
    <div
      class="card my-2"
      draggable="true"
      ondragstart={
        val clusterId = draftCluster.name.bind;
        { event: DragEvent =>
          event.dataTransfer.setData("cluster id", clusterId)
        }
      }
      ondragover={ event: DragEvent =>
        event.preventDefault()
        event.dataTransfer.dropEffect = DragEffect.Move
      }
      ondrop={ event: DragEvent =>
        val fromClusterId = event.dataTransfer.getData("cluster id")
        val sourceIndex = draftClusters.value.indexWhere(_.name.value == fromClusterId)
        val targetIndex = draftClusters.value.indexOf(draftCluster)
        val sourceCluster = draftClusters.value.remove(sourceIndex)
        draftClusters.value.insert(targetIndex, sourceCluster)
      }
    >
      <div class="input-group sticky-top">
        <div class="input-group-prepend">
          <label
            class="input-group-text"
            style:backgroundColor={draftCluster.color.bind.backgroundColor}
            style:color={draftCluster.color.bind.textColor}
            style:cursor="move"
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
          }>
            <span title="Delete" class="fas fa-folder-minus"></span>
          </button>
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
                <button type="button" class="float-right badge badge-secondary" onclick={ _: Event =>
                  draftCluster.nodeIds.value --= selectLocked.selectedNodeIds.value
                }>
                  <span title="Remove Selected" class="fas fa-trash"></span>
                </button>
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
