package com.thoughtworks.modularizer.js

import com.thoughtworks.modularizer.js.models.opaqueDefinition

import scala.scalajs.js

/**
  * @author 杨博 (Yang Bo)
  */
package object models {
  private[models] trait OpaqueDefinition {
    type NodeId <: String
    type ClusterId <: String
    type BuiltInClusterId <: ClusterId
    type CustomClusterId <: ClusterId
    def NodeId(id: String): NodeId
    def NodeIdArray(ids: js.Array[String]): js.Array[NodeId]
    def CustomClusterId(id: String): CustomClusterId
    def ClusterId(id: String): ClusterId

    val Conflict: BuiltInClusterId
    val Facade: BuiltInClusterId
    val Utility: BuiltInClusterId
    val Isolated: BuiltInClusterId
  }

  private[models] val opaqueDefinition: OpaqueDefinition = new OpaqueDefinition {
    type NodeId = String
    type ClusterId = String
    type BuiltInClusterId = String
    type CustomClusterId = String

    def NodeId(id: String): String = id
    def NodeIdArray(ids: js.Array[String]): js.Array[String] = ids

    def CustomClusterId(id: String): String = id
    def ClusterId(id: String): String = id

    val Conflict = "Conflict"
    val Facade = "Facade"
    val Utility = "Utility"
    val Isolated = "Isolated"

  }

  type NodeId = opaqueDefinition.NodeId
  type ClusterId = opaqueDefinition.ClusterId
  type BuiltInClusterId = opaqueDefinition.BuiltInClusterId
  type CustomClusterId = opaqueDefinition.CustomClusterId

  def NodeId(id: String): NodeId = opaqueDefinition.NodeId(id)
  def NodeIdArray(ids: js.Array[String]): js.Array[NodeId] = opaqueDefinition.NodeIdArray(ids)
  def CustomClusterId(id: String): CustomClusterId = opaqueDefinition.CustomClusterId(id)
  def ClusterId(id: String): ClusterId = opaqueDefinition.ClusterId(id)
  object BuiltInClusterId {
    val Conflict: opaqueDefinition.Conflict.type = opaqueDefinition.Conflict
    val Facade: opaqueDefinition.Facade.type = opaqueDefinition.Facade
    val Utility: opaqueDefinition.Utility.type = opaqueDefinition.Utility
    val Isolated: opaqueDefinition.Isolated.type = opaqueDefinition.Isolated
  }
}
