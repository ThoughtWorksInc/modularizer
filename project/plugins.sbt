addSbtPlugin("com.thoughtworks.sbt-best-practice" % "sbt-best-practice" % "7.0.1")

addSbtPlugin("org.xerial.sbt" % "sbt-sonatype" % "2.5")

addSbtPlugin("com.dwijnand" % "sbt-dynver" % "3.3.0+14-76cb6848")

addSbtPlugin("com.jsuereth" % "sbt-pgp" % "1.1.2")

addSbtPlugin("org.lyranthe.sbt" % "partial-unification" % "1.1.2")

addSbtPlugin("com.thoughtworks.example" % "sbt-example" % "6.0.1")

addSbtPlugin("com.vmunier" % "sbt-web-scalajs" % "1.0.8-0.6")

addSbtPlugin("org.scala-js" % "sbt-scalajs" % "0.6.26")

addSbtPlugin("org.irundaia.sbt" % "sbt-sassify" % "1.4.12")

resolvers += Resolver.bintrayRepo("oyvindberg", "ScalablyTyped")
addSbtPlugin("org.scalablytyped" % "sbt-scalablytyped" % "201903180536")

addSbtPlugin("ch.epfl.scala" % "sbt-web-scalajs-bundler" % "0.14.0")
