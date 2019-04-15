package com.thoughtworks.modularizer.views.homepage
import com.thoughtworks.binding.Binding
import typings.graphlibLib.graphlibMod.Graph
import org.scalajs.dom.raw.Node

trait Tab {
  def result: Binding[Option[Graph]]
  def branchName: Binding[Option[String]]
  def view: Binding[Node]
}
