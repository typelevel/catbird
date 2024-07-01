addSbtPlugin("com.github.sbt" % "sbt-unidoc" % "0.5.0")
addSbtPlugin("org.typelevel" % "sbt-typelevel-ci-release" % "0.7.1")
addSbtPlugin("org.typelevel" % "sbt-typelevel-mergify" % "0.7.1")
addSbtPlugin("org.typelevel" % "sbt-typelevel-settings" % "0.7.1")
addSbtPlugin("com.github.sbt" % "sbt-ghpages" % "0.8.0")
addSbtPlugin("org.scalastyle" %% "scalastyle-sbt-plugin" % "1.0.0")
addSbtPlugin("org.scoverage" % "sbt-scoverage" % "2.0.6")
addSbtPlugin("pl.project13.scala" % "sbt-jmh" % "0.4.3")
addSbtPlugin("ch.epfl.scala" % "sbt-scalafix" % "0.12.0")
addSbtPlugin("com.eed3si9n" % "sbt-projectmatrix" % "0.10.0")

ThisBuild / libraryDependencySchemes ++= Seq(
  "org.scala-lang.modules" %% "scala-xml" % VersionScheme.Always
)
