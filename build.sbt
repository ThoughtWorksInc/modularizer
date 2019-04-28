lazy val js = project

lazy val server = project.settings(
  scalaJSProjects += js,
  npmAssets ++= {
    NpmAssets
      .ofProject(js) { nodeModules =>
        // Runtime CSS files
        (nodeModules / "@fortawesome" / "fontawesome-free").allPaths +++ (nodeModules / "bootstrap").allPaths
      }
      .value
  },
)

ThisBuild / organization := "com.thoughtworks.modularizer"

ThisBuild / dynverSeparator := "-"
