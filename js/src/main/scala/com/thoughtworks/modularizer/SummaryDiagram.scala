package com.thoughtworks.modularizer
import com.thoughtworks.binding.{Binding, dom}
import com.thoughtworks.modularizer.model.ClusteringReport
import typings.graphlibLib.graphlibMod.Graph

/**
 * @author 杨博 (Yang Bo)
 */
object SummaryDiagram {

  @dom
  def render(unpatchedGraph: Graph, rule: Binding[ClusteringReport]) = {
    <div class="col-auto flex-fill">
      {
        <!-- TODO -->
      }
    </div>
  }
}
