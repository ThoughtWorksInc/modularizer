enablePlugins(ScalaJSBundlerPlugin)

enablePlugins(Example)

enablePlugins(ScalaJSWeb)

// ScalaJSWeb only works with ScalaJSBundlerPlugin when bundling mode is library-only.
webpackBundlingMode := BundlingMode.LibraryOnly()

Compile / fastOptJS / relativeSourceMaps := false

libraryDependencies += "com.thoughtworks.binding" %%% "dom" % "11.7.0-127-a9f1e4dd"

libraryDependencies += "com.thoughtworks.binding" %%% "jsonhashroute" % "0.2.0"

libraryDependencies += "com.thoughtworks.binding" %%% "latestevent" % "0.1.0-5-8a619f31"

libraryDependencies += "com.thoughtworks.binding" %%% "component" % "0.1.1"

libraryDependencies += "com.thoughtworks.binding" %%% "bindable" % "1.0.1-64-5a774591"

addCompilerPlugin("org.spire-math" %% "kind-projector" % "0.9.9")

addCompilerPlugin("org.scalamacros" % "paradise" % "2.1.1" cross CrossVersion.full)

resolvers += Resolver.bintrayRepo("oyvindberg", "ScalablyTyped")

libraryDependencies += ScalablyTyped.D.d3
npmDependencies in Compile += "d3" -> "5.9.2"

libraryDependencies += ScalablyTyped.D.`dagre-d3`
npmDependencies in Compile += "dagre-d3" -> "0.6.3"

libraryDependencies += ScalablyTyped.G.`graphlib-dot`
npmDependencies in Compile += "graphlib-dot" -> "0.6.2"

scalaJSUseMainModuleInitializer := true

libraryDependencies += "org.scalatest" %%% "scalatest" % "3.0.7" % Test

scalacOptions += "-P:scalajs:sjsDefinedByDefault"
