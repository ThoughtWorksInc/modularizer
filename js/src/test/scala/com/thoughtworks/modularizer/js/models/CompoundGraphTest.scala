package com.thoughtworks.modularizer.js.models

import org.scalatest.{FreeSpec, Matchers}
import typings.graphlibDashDotLib.graphlibDashDotMod

/**
  * @author 邓何均 (Deng,HeJun)
  * */
class CompoundGraphTest extends FreeSpec with Matchers {
  "Test CompoundGraph Class" - {
    "CompoundGraph.unassignedNodeIds and clusterIds" in {
      this.info("given: read a compound graph ")
      val graph = graphlibDashDotMod.^.read("""
             digraph {
                 F -> A -> B -> C
                 A -> X -> Y -> E
                 G -> X -> C
                 D -> Y 
              } """)

      graph.setParent("C", "A")
      graph.setParent("D", "A")
      graph.setParent("E", "B")
      graph.setParent("F", "B")

      val compoundGraph = new CompoundGraph(graph, IndexedSeq(CustomClusterId("A"), CustomClusterId("B")))

      this.info("when: get clusterIds and unassignedNodeIds")
      val clusterIds = compoundGraph.clusterIds
      val unassignedNodeIds = compoundGraph.unassignedNodeIds.toSeq

      this.info("then: clusterIds and unassignedNodeIds should contains correct ids")
      clusterIds should contain only ("A", "B")
      unassignedNodeIds should contain only ("X", "Y", "G")
    }
  }
}
