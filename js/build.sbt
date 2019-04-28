enablePlugins(ScalaJSBundlerPlugin)

enablePlugins(Example)

enablePlugins(ScalaJSWeb)

enablePlugins(BuildInfoPlugin)

// ScalaJSWeb only works with ScalaJSBundlerPlugin when bundling mode is library-only.
webpackBundlingMode := BundlingMode.LibraryOnly()

Compile / fastOptJS / relativeSourceMaps := false

libraryDependencies += "com.thoughtworks.binding" %%% "jspromisebinding" % "11.7.0+144-c34de6d5"

libraryDependencies += "com.thoughtworks.binding" %%% "futurebinding" % "11.7.0+144-c34de6d5"

libraryDependencies += "com.thoughtworks.binding" %%% "dom" % "11.7.0+144-c34de6d5"

libraryDependencies += "com.thoughtworks.binding" %%% "jsonhashroute" % "0.2.0"

libraryDependencies += "com.thoughtworks.binding" %%% "latestevent" % "0.2.0+12-59d56a67"

libraryDependencies += "com.thoughtworks.binding" %%% "latestjqueryevent" % "0.2.0"

libraryDependencies += "com.thoughtworks.binding" %%% "component" % "0.1.1"

libraryDependencies += "com.thoughtworks.binding" %%% "bindable" % "1.1.0"

libraryDependencies += "com.thoughtworks.dsl" %%% "dsl" % "1.1.1+18-0516ac07"

libraryDependencies += "io.lemonlabs" %%% "scala-uri" % "1.4.4"

addCompilerPlugin("org.spire-math" %% "kind-projector" % "0.9.9")

addCompilerPlugin("org.scalamacros" % "paradise" % "2.1.1" cross CrossVersion.full)

resolvers += Resolver.bintrayRepo("oyvindberg", "ScalablyTyped")

libraryDependencies += ScalablyTyped.D.`dagre-d3`
npmDependencies in Compile += "dagre-d3" -> "0.6.3"

libraryDependencies += ScalablyTyped.D.`d3-shape`
npmDependencies in Compile += "d3-shape" -> "1.3.5"

libraryDependencies += ScalablyTyped.G.`graphlib-dot`
npmDependencies in Compile += "graphlib-dot" -> "0.6.2"

libraryDependencies += ScalablyTyped.F.`file-saver`
npmDependencies in Compile += "file-saver" -> "2.0.1"

libraryDependencies += ScalablyTyped.B.bootstrap
npmDependencies in Compile += "bootstrap" -> "4.3.1"
npmDependencies in Compile += "jquery" -> "3.3.1"
npmDependencies in Compile += "popper.js" -> "1.14.7"

npmDependencies in Compile += "@fortawesome/fontawesome-free" -> "5.8.1"

scalaJSUseMainModuleInitializer := true

libraryDependencies += "org.scalatest" %%% "scalatest" % "3.0.7" % Test

scalacOptions += "-P:scalajs:sjsDefinedByDefault"

publish / skip := true

buildInfoPackage := "com.thoughtworks.modularizer.js"
