package com.thoughtworks.modularizer.js.views.workboard.ruleeditor

import com.thoughtworks.modularizer.js.utilities._
import com.thoughtworks.binding.Binding.BindingSeq
import com.thoughtworks.binding.bindable._
import com.thoughtworks.binding.{Binding, LatestEvent, dom}
import org.scalajs.dom.raw.{HTMLOptionElement, HTMLSelectElement}

class MultipleSelect[Items: BindableSeq.Lt[?, String]](val items: Items) {

  @dom
  protected def option(text: String): Binding[HTMLOptionElement] = <option title={text} value={text}>{ text }</option>

  private val options: BindingSeq[HTMLOptionElement] = items.bindSeq.mapBinding(option)

  @dom
  val view: Binding[HTMLSelectElement] = {
    <select
      class={
        s"""
          custom-select
          ${if (items.bindSeq.isEmpty.bind) "d-none" else ""}
        """
      }
      style:direction="rtl"
      selectedIndex={-1}
      multiple="multiple"
      size={items.bindSeq.length.bind}
    >{ options.bind }</select>
  }

  val selectedNodeIds: BindingSeq[String] = for {
    option <- options
    if {
      val _ = LatestEvent.change(view.bind).bind
      option.selected
    }
  } yield option.value

}
