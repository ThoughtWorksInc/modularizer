package com.thoughtworks.modularizer.js.views.workboard.ruleeditor

import com.thoughtworks.binding.Binding.{BindingSeq, Var}
import com.thoughtworks.binding.bindable._
import com.thoughtworks.binding.{Binding, dom}
import com.thoughtworks.modularizer.js.models.{ClusteringRule, DraftCluster, NodeId}
import com.thoughtworks.modularizer.js.models.DraftCluster.ClusterColor
import org.scalajs.dom._

import scala.collection.immutable

/**
  * @author 杨博 (Yang Bo)
  */
class UnassignedCard(items: BindingSeq[NodeId], draftClusters: BindingSeq[DraftCluster], rule: Var[ClusteringRule]) {

  private val selectUnlockedNodes = new MultipleSelect(items.bindSeq)

  def selectedUnlockedNodeIds: Binding.BindingSeq[String] = selectUnlockedNodes.selectedNodeIds

  @dom val view: Binding[Node] = {
    <div class="card my-2">
      <div class="input-group sticky-top">
        <div class="input-group-prepend">
          <label
            class="input-group-text"
            style:backgroundColor={ DraftCluster.UnassignedColorClass.backgroundColor}
            style:color={ DraftCluster.UnassignedColorClass.textColor }
          >Built-in Cluster</label>
        </div>
        <input type="text" readOnly="readOnly" class="form-control" value="Unassigned"/>
      </div>
      <div class="card-body">
        <details id="unlockedDetails">
          <summary class="bg-white position-sticky" style:top="2em">
            Unlocked Nodes
            <span class="fas fa-unlock"></span>
            <span class="badge badge-info">{ selectUnlockedNodes.items.length.bind.toString }</span>
          </summary>
          { selectUnlockedNodes.view.bind }
        </details>
      </div>
    </div>
  }

}
