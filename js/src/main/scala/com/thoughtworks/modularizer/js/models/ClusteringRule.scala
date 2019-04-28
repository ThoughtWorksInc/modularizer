package com.thoughtworks.modularizer.js.models
import scala.collection.immutable
import upickle.default._

/**
  * @author 杨博 (Yang Bo)
  */
final case class ClusteringRule(breakingEdges: Iterable[(String, String)], clusters: immutable.Seq[ClusteringRule.Cluster]) {}
object ClusteringRule {
  def empty = ClusteringRule(Iterable.empty, immutable.Seq.empty)

  implicit val rw: ReadWriter[ClusteringRule] = macroRW

  final case class Cluster(parent: String, children: immutable.Seq[String])
  object Cluster {
    implicit val rw: ReadWriter[Cluster] = macroRW
  }

}
