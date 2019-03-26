package com.thoughtworks.modularizer

import org.scalajs.dom.raw.HTMLElement

import scala.language.implicitConversions
import scala.scalajs.js.{UndefOr, UndefOrOps, |}

/**
  * @author 杨博 (Yang Bo)
  */
package object util {

  implicit final def unitOrOps[A](unitOr: Unit | A): UndefOrOps[A] = {
    new UndefOrOps(unitOr.asInstanceOf[UndefOr[A]])
  }

  implicit final def orUnitOps[A](unitOr: A | Unit): UndefOrOps[A] = {
    new UndefOrOps(unitOr.asInstanceOf[UndefOr[A]])
  }

  implicit final class ClassMapOps @inline()(node: HTMLElement) {
    @inline def classMap: Map[String, Boolean] = {
      node.className.split(' ').view.map(_ -> true).toMap
    }

    @inline def classMap_=(enabledClasses: Iterable[(String, Boolean)]): Unit = {
      node.className = enabledClasses.view.withFilter(_._2).map(_._1).mkString(" ")
    }

  }
}
