package com.thoughtworks.modularizer.js.views.workboard

import com.thoughtworks.binding.Binding.Vars
import com.thoughtworks.binding.{Binding, LatestEvent, dom}
import org.scalajs.dom.raw.Event
import org.scalajs.dom.raw.Node

/**
  * @author 杨博 (Yang Bo)
  */
class BreakingEdgeList(breakingEdges: Vars[(String, String)]) {
  @dom
  def view: Binding[Node] = {
    <div class="card my-2">
      <div class="card-body">
        <details>
          <summary>
            Breaking Edges
          </summary>
          <form class="form-inline flex-nowrap my-1">
            <input type="text" class="m-1 form-control flex-shrink-1 flex-grow-1" style:width="100%" placeholder="Dependent" id="dependentInput"/>
            <span class="m-1 fas fa-arrow-right"></span>
            <input type="text" class="m-1 form-control flex-shrink-1 flex-grow-1" style:width="100%" placeholder="Dependency" id="dependencyInput"/>
            <button type="submit" class="m-1 btn btn-secondary" disabled={
              locally(LatestEvent.input(dependentInput).bind)
              locally(LatestEvent.input(dependencyInput).bind)
              locally(breakingEdges.length.bind)
              dependentInput.value.isEmpty || dependencyInput.value.isEmpty
            }
              onclick={ event: Event =>
                event.preventDefault()
                val from = dependentInput.value
                val to = dependencyInput.value
                dependentInput.value = ""
                dependencyInput.value = ""
                breakingEdges.value.prepend(from -> to)
              }
            ><span class="fas fa-unlink"></span></button>
          </form>
          {
            for ((from, to) <- breakingEdges) yield {
              <form class="form-inline flex-nowrap my-1">
                <span title={ from } style:direction="rtl" class="m-1 ml-auto flex-shrink-1 flex-shrink-1 text-right text-truncate">{
                  from
                }</span>
                <span class="m-1 fas fa-arrow-right"></span>
                <span title={ to } style:direction="rtl" class="m-1 mr-auto flex-shrink-1 flex-shrink-1 text-right text-truncate">{
                  to
                }</span>
                <button type="submit" class="m-1 btn btn-secondary"
                  onclick={ event: Event =>
                    event.preventDefault()
                    breakingEdges.value -= from -> to
                  }
                ><span class="fas fa-link"></span></button>
              </form>
            }
          }
        </details>
      </div>
    </div>
  }
}
