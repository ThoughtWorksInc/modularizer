enablePlugins(SbtWeb)

enablePlugins(SbtSassify)

enablePlugins(WebScalaJSBundlerPlugin)

lazy val js = project

scalaJSProjects += js

pipelineStages in Assets += scalaJSPipeline

libraryDependencies += "org.webjars" % "bootstrap" % "3.4.1"

libraryDependencies += "org.webjars" % "jquery" % "3.3.1-2"

ThisBuild / organization := "com.thoughtworks.modularizer"
