package com.thoughtworks.modularizer.js.views.homepage
import com.thoughtworks.binding.Binding
import org.scalajs.dom.raw.Node

trait Tab {
  def branchName: Binding[Option[String]]
  def view: Binding[Node]
}
