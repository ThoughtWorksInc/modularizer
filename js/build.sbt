enablePlugins(ScalaJSBundlerPlugin)

enablePlugins(Example)

// Don't enable ScalaJSWeb due to https://github.com/scalacenter/scalajs-bundler/pull/288
// enablePlugins(ScalaJSWeb)

libraryDependencies += "com.thoughtworks.binding" %%% "dom" % "11.6.0"

libraryDependencies += "com.thoughtworks.binding" %%% "jsonhashroute" % "0.1.0"

libraryDependencies += "com.thoughtworks.binding" %%% "latestevent" % "0.1.0"

libraryDependencies += "com.thoughtworks.binding" %%% "component" % "0.1.1"

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
