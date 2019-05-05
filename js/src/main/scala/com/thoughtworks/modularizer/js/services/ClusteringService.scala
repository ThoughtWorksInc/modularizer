package com.thoughtworks.modularizer.js.services

import com.thoughtworks.binding.Binding
import com.thoughtworks.modularizer.js.models.{ClusterId, NodeId}

/**
  * @author 杨博 (Yang Bo)
  */
class ClusteringService {

  def getParent(nodeId: NodeId): Binding[Option[ClusterId]] = ???

  def getDependencies(nodeId: NodeId): Binding[Seq[ClusterId]] = ???

  def getDependents(nodeId: NodeId): Binding[Seq[ClusterId]] = ???

}
