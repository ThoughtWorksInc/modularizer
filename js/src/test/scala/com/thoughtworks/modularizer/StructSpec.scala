package com.thoughtworks.modularizer
import org.scalatest.{FreeSpec, Matchers}
import scala.language.experimental.macros
import scala.language.implicitConversions
import scala.reflect.macros.whitebox
import scala.scalajs.js
import scala.scalajs.js.annotation.ScalaJSDefined
import scala.language.dynamics

/**
  * @author 杨博 (Yang Bo)
  */
class StructSpec extends FreeSpec with Matchers {
  import StructSpec._
  "xx" in {
    val myStruct: Struct[{ def jsField(): Int }] = (new MyJsClass).asInstanceOf[Struct[{ def jsField(): Int }]]

    myStruct.jsField() should be(2)

  }
}
object StructSpec {
//
//  class Macros(val c: whitebox.Context) {
//    import c.universe._
//    def applyDynamic(methodName: Tree)(args: Tree*): Tree = q"1"
//  }

//  trait StructOps[+Self] extends Dynamic {
////    def applyDynamic(methodName: String)(args: Any*): Int = macro Macros.applyDynamic
//  }

  type Struct[+Self] = js.Object with DynamicStruct[Self]
  trait DynamicStruct[+Self] extends Dynamic

  object DynamicStruct {
    implicit final class StructOps[A](struct: Struct[A]) {
      def applyDynamic(methodName: String)(args: Any*): Int = 2
    }
  }

//  type Struct[+Self] = js.Object with StructTag[Self]

//  object Struct {
//    implicit def toDynamic[A](struct: Struct[A]): js.Dynamic = {
//      struct.asInstanceOf[js.Dynamic]
//    }
//  }

  class MyJsClass extends js.Object {
    def jsField() = 1
  }

}
