package com.thoughtworks.modularizer.model

import com.thoughtworks.binding.Binding.{Var, Vars}

import scala.collection.immutable
final case class DraftCluster(name: Var[String], nodeIds: Vars[String], color: Var[String]) {
  def buildCluster: ClusteringRule.Cluster = {
    ClusteringRule.Cluster(name.value, nodeIds.value.to[immutable.Seq])
  }
}
object DraftCluster {

  val ClusterColors = scala.util.Random.shuffle(
    Seq(
      "var(--blue)",
      "var(--indigo)",
      "var(--purple)",
      "var(--pink)",
      "var(--red)",
      "var(--orange)",
      "var(--yellow)",
      "var(--green)",
      "var(--teal)",
      "var(--cyan)",
    ))
}
