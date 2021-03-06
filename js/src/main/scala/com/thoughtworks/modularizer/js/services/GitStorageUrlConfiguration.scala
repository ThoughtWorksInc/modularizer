package com.thoughtworks.modularizer.js.services

import scala.scalajs.js.URIUtils.encodeURIComponent

/**
  * @author 杨博 (Yang Bo)
  */
class GitStorageUrlConfiguration {
  def graphJsonUrl(branch: String) = s"api/git/branches/${encodeURIComponent(branch)}/files/graph.json"
  def ruleJsonUrl(branch: String) = s"api/git/branches/${encodeURIComponent(branch)}/files/rule.json"
}
