package com.thoughtworks.modularizer.model

import typings.graphlibLib.graphlibMod.{Graph, GraphOptions}

import scala.util.matching.Regex

object JdepsGraph {
  private val BeforeSpace: Regex = """^(\S+) .*$""".r
}

import com.thoughtworks.modularizer.model.JdepsGraph._

/** Contains utilities on graph produced by `jdeps`
  *
  * @author 杨博 (Yang Bo)
  */
final case class JdepsGraph(jdepsGraph: Graph) extends AnyVal {

  /** Returns a new graph, which contains only internal dependencies.
    *
    * @param jdepsGraph The graph produced by `jdeps`
    * @example Given a graph parsed from a DOT file,
    *
    *          {{{
    *                    import typings.graphlibDashDotLib.graphlibDashDotMod
    *                    import com.thoughtworks.modularizer.model.JdepsGraph
    *                    val jdepsGraph = JdepsGraph(graphlibDashDotMod.^.read("""
    *                      digraph "your.jar" {
    *                          // Path: your/target/your.jar
    *                          "com.foo.bar"     -> "java.io (java.base)";
    *                          "com.foo.bar"     -> "com.foo.bar.baz (your.jar)";
    *                          "com.foo.bar.baz" -> "com.notfound.mypackage (找不到)";
    *                      }
    *                    """))
    *          }}}
    *
    *          when calculating internal dependencies,
    *
    *          {{{
    *                    val internalDependencies = jdepsGraph.internalDependencies
    *          }}}
    *
    *          then the returned graph should only contain internal dependencies,
    *          and package names in parentheses should be removed.
    *
    *          {{{
    *                    import scala.scalajs.js
    *
    *                    new js.WrappedArray(internalDependencies.nodes) should contain only ("com.foo.bar", "com.foo.bar.baz")
    *
    *                    import org.scalatest.Inside._
    *                    import typings.graphlibLib.graphlibMod.Edge
    *                    inside(new js.WrappedArray(internalDependencies.edges)) {
    *                      case js.WrappedArray(edge) =>
    *                        edge.v should be("com.foo.bar")
    *                        edge.w should be("com.foo.bar.baz")
    *                    }
    *          }}}
    */
  def internalDependencies: Graph = {
    val internalGraph = new Graph(new GraphOptions {
      compound = false
      directed = true
      multigraph = false
    })
    for (edge <- jdepsGraph.edges) {
      internalGraph.setNode(edge.v)
    }
    for (edge <- jdepsGraph.edges) {
      val BeforeSpace(target) = edge.w
      if (internalGraph.hasNode(target)) {
        internalGraph.setEdge(edge.v, target)
      }
    }
    internalGraph
  }

}
