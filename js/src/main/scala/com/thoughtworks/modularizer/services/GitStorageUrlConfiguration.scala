package com.thoughtworks.modularizer.services

import scala.scalajs.js.URIUtils.encodeURIComponent

/**
  * @author 杨博 (Yang Bo)
  */
class GitStorageUrlConfiguration {
  def graphJsonUrl(branch: String) = s"api/git/branches/${encodeURIComponent(branch)}/files/graph.json"
}
