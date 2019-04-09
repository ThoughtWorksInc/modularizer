package com.thoughtworks.modularizer

import scala.language.implicitConversions
import scala.scalajs.js.{UndefOr, UndefOrOps, |}

/**
  * @author 杨博 (Yang Bo)
  */
package object util {

  implicit final def unitOrOps[A, B](unitOr: Unit | A)(implicit ev: |.Evidence[A, B]): UndefOrOps[B] = {
    new UndefOrOps(unitOr.asInstanceOf[UndefOr[B]])
  }

  implicit final def orUnitOps[A, B](unitOr: A | Unit)(implicit ev: |.Evidence[A, B]): UndefOrOps[B] = {
    new UndefOrOps(unitOr.asInstanceOf[UndefOr[B]])
  }

}
