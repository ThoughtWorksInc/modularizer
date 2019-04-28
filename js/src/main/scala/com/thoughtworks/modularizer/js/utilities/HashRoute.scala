package com.thoughtworks.modularizer.js.utilities

import com.thoughtworks.binding.{Binding, FutureBinding, LatestEvent}
import com.thoughtworks.binding.Binding.BindingInstances.monadSyntax._
import com.thoughtworks.dsl.Dsl
import com.thoughtworks.dsl.Dsl.Keyword
import org.scalajs.dom.{window, _}

import scala.concurrent.{ExecutionContext, Future}
import scala.scalajs.js.URIUtils.decodeURIComponent
import scala.util.Success

/** Bidirectional data-binding between the page state built from UI events and the page state parsed from URL hash. */
final case class HashRoute(window: Window = window) extends AnyVal with Keyword[HashRoute, Unit]
object HashRoute {

  private def trimHash(): String = {
    decodeURIComponent(window.location.hash match {
      case hashText if hashText.startsWith("#") =>
        hashText.substring(1)
      case hashText =>
        hashText
    })
  }

  implicit def hashRouteDsl(implicit executionContext: ExecutionContext): Dsl[HashRoute, Binding[String], Unit] = {
    (keyword, viewState) => // viewState is the page state triggered by UI events
      val HashRoute(window) = keyword
      Binding {
        FutureBinding(Future {}).bind match {
          case Some(Success(())) =>
            val currentState = viewState(()).bind
            val () = FutureBinding(Future {
              window.location.hash = currentState
            }).map(Function.const(())).bind
            LatestEvent.hashchange(window).bind match {
              case None =>
                currentState
              case Some(event) =>
                trimHash()
            }
          case _ =>
            trimHash()
        }
      }

  }
}
//class HashRoute(hashInput: => Binding[String], window: Window = window)(implicit executionContext: ExecutionContext) {
//
//  /** The page state triggered by URL hash change. */
//  final def hashOutput = Binding {
//    FutureBinding(Future {}).bind match {
//      case Some(Success(())) =>
//        val currentState = hashInput.bind
//        window.location.hash = currentState
//        new LatestEvent[Event](window, "hashchange").bind match {
//          case None =>
//            currentState
//          case Some(event) =>
//            trimHash()
//        }
//      case _ =>
//        trimHash()
//    }
//  }
//
//  private def trimHash() = {
//    decodeURIComponent(window.location.hash match {
//      case hashText if hashText.startsWith("#") =>
//        hashText.substring(1)
//      case hashText =>
//        hashText
//    })
//  }
//
//}
