package com.thoughtworks.modularizer.js.models

import com.thoughtworks.binding.Binding.Var
import com.thoughtworks.binding.JsonHashRoute
import upickle.default._
import com.thoughtworks.binding.Binding

/**
  * @author 杨博 (Yang Bo)
  */
sealed trait PageState

object PageState {

  sealed trait WorkBoardState
  object WorkBoardState {
    case object Summary extends WorkBoardState

    // Workaround for https://github.com/scala/bug/issues/7046
    implicit val readWriter: ReadWriter[WorkBoardState] = ReadWriter.merge(
      macroRW[Summary.type],
    )

  }

  case object HomePage extends PageState
  final case class WorkBoard(graphState: WorkBoardState) extends PageState

  // Workaround for https://github.com/scala/bug/issues/7046
  implicit val readWriter: ReadWriter[PageState] = ReadWriter.merge(
    macroRW[HomePage.type],
    macroRW[WorkBoard],
  )

}
