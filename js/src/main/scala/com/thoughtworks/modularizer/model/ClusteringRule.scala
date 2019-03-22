package com.thoughtworks.modularizer.model

/**
  * @author 杨博 (Yang Bo)
  */
final case class ClusteringRule(breakingEdges: Set[(String, String)], clusters: Seq[ClusteringRule.Cluster]) {}
object ClusteringRule {
  case class Cluster(parent: String, children: Seq[String])

}
