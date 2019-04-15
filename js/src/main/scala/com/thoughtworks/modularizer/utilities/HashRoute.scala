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
import com.thoughtworks.binding.Binding
import com.thoughtworks.binding.FutureBinding
import scala.concurrent.Future
import com.thoughtworks.dsl.Dsl, Dsl.Keyword

final case class HashRoute(window: Window = window) extends AnyVal with Keyword[HashRoute, String]

object HashRoute {
  implicit def hashRouteDsl(implicit executionContext: ExecutionContext): Dsl[HashRoute, Binding[String], String] = { (keyword, handler) =>
    val HashRoute(window) = keyword
    Binding {
      val _ = new LatestEvent[Event](window, "hashchange").bind 
      val currentValue = {
        decodeURIComponent(window.location.hash match {
          case hashText if hashText.startsWith("#") =>
            hashText.substring(1)
          case hashText =>
            hashText
        })
      }
      FutureBinding(Future(())).bind match {
        case None => 
          currentValue
        case _ =>
          val nextValue = handler(currentValue).bind
          FutureBinding(
            Future {
              window.location.hash = nextValue
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

}

// /** Bidirectional data-binding between the page state built from UI events and the page state parsed from URL hash.
//   *
//   * @param nextState The page state triggered by UI events
//   */
// class HashRoute(nextState: => Binding[String], window: Window = window)(implicit executionContext: ExecutionContext) {
//   private def hashState() = {
//     decodeURIComponent(window.location.hash match {
//       case hashText if hashText.startsWith("#") =>
//         hashText.substring(1)
//       case hashText =>
//         hashText
//     })
//   }

//   /** The page state tiggered by URL hash change. */
//   final def currentState = Binding {
//     FutureBinding(Future {}).bind match {
//       case Some(Success(())) =>
//         val currentState = nextState.bind
//         window.location.hash = currentState
//         new LatestEvent[Event](window, "hashchange").bind match {
//           case None =>
//             currentState
//           case Some(event) =>
//             hashState()
//         }
//       case _ =>
//         hashState()
//     }
//   }

// }
