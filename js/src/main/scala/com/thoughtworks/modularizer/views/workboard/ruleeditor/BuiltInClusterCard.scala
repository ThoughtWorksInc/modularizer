package com.thoughtworks.modularizer.views.workboard.ruleeditor

import com.thoughtworks.binding.bindable._
import com.thoughtworks.binding.{Binding, dom}
import com.thoughtworks.modularizer.models.DraftCluster.ClusterColor
import org.scalajs.dom._

/**
  * @author 杨博 (Yang Bo)
  */
class BuiltInClusterCard[Items: BindableSeq.Lt[?, String]](items: Items,
                                                           clusterName: String,
                                                           clusterColor: ClusterColor) {

  private val selectUnlockedNodes = new MultipleSelect(items.bindSeq)

  def selectedUnlockedNodeIds: Binding.BindingSeq[String] = selectUnlockedNodes.selectedNodeIds

  @dom val view: Binding[Node] = {
    <div class="card my-2">
      <div class="input-group sticky-top">
        <div class="input-group-prepend">
          <label
            class="input-group-text"
            style:backgroundColor={clusterColor.backgroundColor}
            style:color={ clusterColor.textColor }
          >Built-in Cluster</label>
        </div>
        <input type="text" readOnly="readOnly" class="form-control" value={ clusterName }/>
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
