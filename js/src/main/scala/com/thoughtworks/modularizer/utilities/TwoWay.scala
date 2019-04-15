package com.thoughtworks.modularizer.utilities

import com.thoughtworks.binding.Binding.{SingleMountPoint, Var}
import org.scalajs.dom.{window, _}
import scala.util.Try

import scala.scalajs.js
import scala.scalajs.js.URIUtils.decodeURIComponent
import scala.util.control.NonFatal
import com.thoughtworks.binding.LatestEvent
import com.thoughtworks.binding.Binding.BindingInstances.monadSyntax._
import scala.util.Success

import scala.concurrent.ExecutionContext
import com.thoughtworks.dsl.Dsl, Dsl.Keyword
import com.thoughtworks.binding.Binding
import com.thoughtworks.binding.FutureBinding
import scala.concurrent.Future

case class TwoWay[A](v: Var[A]) extends AnyVal with Keyword[TwoWay[A], A]

object TwoWay {

  implicit def twoWayDsl[A](implicit executionContext: ExecutionContext): Dsl[TwoWay[A], Binding[A], A] = {
    (keyword, modification) =>
      val TwoWay(v) = keyword
      Binding {
        val currentValue = v.bind
        val nextValue = modification(currentValue).bind
        FutureBinding(
          Future {
            v.value = nextValue
          }
        ).bind match {
          case Some(Success(())) =>
            nextValue
          case _ =>
            currentValue
        }
      }
  }

}
