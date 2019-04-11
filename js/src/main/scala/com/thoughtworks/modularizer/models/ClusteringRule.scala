package com.thoughtworks.modularizer.models
import scala.collection.immutable

/**
  * @author 杨博 (Yang Bo)
  */
final case class ClusteringRule(breakingEdges: Set[(String, String)], clusters: immutable.Seq[ClusteringRule.Cluster]) {}
object ClusteringRule {
  case class Cluster(parent: String, children: immutable.Seq[String])

}
