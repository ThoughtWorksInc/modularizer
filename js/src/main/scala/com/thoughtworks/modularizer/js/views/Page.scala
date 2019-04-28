package com.thoughtworks.modularizer.js.views

import com.thoughtworks.binding.Binding
import org.scalajs.dom.raw.Node

trait Page {
  def view: Binding[Node]
}
