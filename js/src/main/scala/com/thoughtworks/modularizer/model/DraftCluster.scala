package com.thoughtworks.modularizer.model

import com.thoughtworks.binding.Binding.{Var, Vars}
import com.thoughtworks.modularizer.model.DraftCluster.ClusterColor

import scala.collection.immutable
import scala.util.Random.shuffle

final case class DraftCluster(name: Var[String], nodeIds: Vars[String], color: Var[ClusterColor]) {
  def buildCluster: ClusteringRule.Cluster = {
    ClusteringRule.Cluster(name.value, nodeIds.value.to[immutable.Seq])
  }
}
object DraftCluster {

  case class ClusterColor(textColorName: String, backgroundColorName: String) {
    val textColor = s"var(--$textColorName)"
    val backgroundColor = s"var(--$backgroundColorName)"
  }

  final val CustomClusterColors = shuffle(
    Seq(
      ClusterColor("light", "blue"),
      ClusterColor("light", "indigo"),
      ClusterColor("light", "purple"),
      ClusterColor("light", "pink"),
      ClusterColor("light", "red"),
      ClusterColor("light", "orange"),
      ClusterColor("light", "yellow"),
      ClusterColor("light", "green"),
      ClusterColor("light", "teal"),
      ClusterColor("light", "cyan"),
    ))

  final val UtilityColorClass = ClusterColor("dark", "light")
  final val FacadeColorClass = ClusterColor("light", "dark")
  final val UnassignedColorClass = ClusterColor("light", "gray")

}
