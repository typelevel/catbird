addSbtPlugin("com.github.sbt" % "sbt-unidoc" % "0.5.0")
addSbtPlugin("org.typelevel" % "sbt-typelevel" % "0.7.7")
addSbtPlugin("com.github.sbt" % "sbt-ghpages" % "0.8.0")
addSbtPlugin("org.scalastyle" %% "scalastyle-sbt-plugin" % "1.0.0")
addSbtPlugin("org.scoverage" % "sbt-scoverage" % "2.3.1")
addSbtPlugin("pl.project13.scala" % "sbt-jmh" % "0.4.3")
addSbtPlugin("ch.epfl.scala" % "sbt-scalafix" % "0.14.2")
addSbtPlugin("org.typelevel" % "sbt-typelevel-mergify" % "0.7.7")
addSbtPlugin("org.scala-native" % "sbt-scala-native" % "0.4.16")

ThisBuild / libraryDependencySchemes ++= Seq(
  "org.scala-lang.modules" %% "scala-xml" % VersionScheme.Always
)
