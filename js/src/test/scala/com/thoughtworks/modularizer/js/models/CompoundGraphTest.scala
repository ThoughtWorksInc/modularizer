package com.thoughtworks.modularizer.js.models

import org.scalatest.{FreeSpec, Matchers}

class CompoundGraphTest extends FreeSpec with Matchers {
  "Test CompoundGraph Class" - {

    "CompoundGraph.unassignedNodeIds and clusterIds" in {
      // given
      import typings.graphlibDashDotLib.graphlibDashDotMod
      val graph = graphlibDashDotMod.^.read("""
             digraph "your.jar" {
                 F -> A -> B -> C
                 A -> X -> Y -> E
                 G -> X -> C
                 D -> Y
             }
           """)

      graph.setParent("B", "cluster B")
      graph.setParent("F", "cluster F")
      graph.setParent("A", "cluster F")
      graph.setParent("D", "cluster D")
      graph.setParent("G", "cluster G")
      graph.setParent("E", "cluster E")

      // when
      val compoundGraph = new CompoundGraph(graph)

      // then
      compoundGraph.clusterIds.toList should contain("cluster B")
      compoundGraph.clusterIds.toList should contain("cluster F")
      compoundGraph.clusterIds.toList should contain("cluster D")
      compoundGraph.clusterIds.toList should contain("cluster G")
      compoundGraph.clusterIds.toList should contain("cluster E")
      compoundGraph.unassignedNodeIds.toList should contain("C")
      compoundGraph.unassignedNodeIds.toList should contain("X")
      compoundGraph.unassignedNodeIds.toList should contain("Y")
    }
  }
}
