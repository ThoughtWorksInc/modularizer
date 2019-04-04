package com.thoughtworks.modularizer.server

import org.rogach.scallop.ScallopConf

/**
  * @author 杨博 (Yang Bo)
  */
class Configuration(arguments: Seq[String]) extends ScallopConf(arguments) {
  printedName = "Modularizer Server"
  val listeningHost = opt[String](noshort = true, default = Some("localhost"))
  val listeningPort = opt[Int](noshort = true, default = Some(42019))
  val gitUri = opt[String](noshort = true, required = true)
  val gitUsername = opt[String](noshort = true)
  val gitPassword = opt[String](noshort = true, default = Some(""))
  val numberOfTemporaryGitClones = opt[Int](noshort = true, default = Some(3))
  verify()
}
