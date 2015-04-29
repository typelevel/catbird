import ScoverageSbtPlugin.ScoverageKeys.coverageExcludedPackages

val bijectionVersion = "0.7.2"
val utilVersion = "6.24.0"
val finagleVersion = "6.25.0"

lazy val buildSettings = Seq(
  organization := "io.catbird",
  version := "0.1.0-SNAPSHOT",
  scalaVersion := "2.11.6",
  crossScalaVersions := Seq("2.11.6")
)

lazy val compilerOptions = Seq(
  "-deprecation",
  "-encoding", "UTF-8",
  "-feature",
  "-language:existentials",
  "-language:higherKinds",
  "-unchecked",
  "-Yno-adapted-args",
  "-Ywarn-dead-code",
  "-Ywarn-numeric-widen",
  "-Xfuture"
)

lazy val baseSettings = Seq(
  scalacOptions ++= compilerOptions ++ (
    CrossVersion.partialVersion(scalaVersion.value) match {
      case Some((2, 11)) => Seq("-Ywarn-unused-import")
      case _ => Nil
    }
  ),
  wartremoverWarnings in (Compile, compile) ++= Warts.allBut(
    Wart.NoNeedForMonad
  )
)

lazy val allSettings = buildSettings ++ baseSettings ++ unidocSettings

lazy val root = project.in(file("."))
  .settings(allSettings ++ noPublish)
  .settings(unidocSettings ++ site.settings ++ ghpages.settings)
  .settings(
    site.addMappingsToSiteDir(mappings in (ScalaUnidoc, packageDoc), "api"),
    git.remoteRepo := "git@github.com:travisbrown/catbird.git"
  )
  .settings(scalacOptions in (Compile, console) := compilerOptions)
  .aggregate(util, finagle)
  .dependsOn(util, finagle)
  .dependsOn(
    ProjectRef(uri("git://github.com/travisbrown/cats.git#demo"), "std")
  )

lazy val test = project
  .settings(buildSettings ++ baseSettings)
  .settings(
    libraryDependencies ++= Seq(
      "com.twitter" %% "bijection-core" % bijectionVersion,
      "com.twitter" %% "util-core" % utilVersion,
      "org.scalacheck" %% "scalacheck" % "1.12.2",
      "org.scalatest" %% "scalatest" % "2.2.4",
      "org.typelevel" %% "discipline" % "0.2.1"
    ),
    coverageExcludedPackages := "io\\.catbird\\.test\\..*"
  )
  .dependsOn(
    ProjectRef(uri("git://github.com/travisbrown/cats.git#demo"), "core"),
    ProjectRef(uri("git://github.com/travisbrown/cats.git#demo"), "laws"),
    ProjectRef(uri("git://github.com/travisbrown/cats.git#demo"), "std")
  )
  .disablePlugins(CoverallsPlugin)

lazy val util = project
  .settings(buildSettings ++ baseSettings)
  .settings(
    libraryDependencies ++= Seq(
      "com.twitter" %% "bijection-core" % bijectionVersion,
      "com.twitter" %% "util-core" % utilVersion
    )
  )
  .dependsOn(
    ProjectRef(uri("git://github.com/travisbrown/cats.git#demo"), "core"),
    test % "test"
  )
  .disablePlugins(CoverallsPlugin)

lazy val finagle = project
  .settings(allSettings)
  .settings(
    libraryDependencies ++= Seq(
      "com.twitter" %% "finagle-core" % finagleVersion
    )
  )
  .dependsOn(util, test % "test")
  .disablePlugins(CoverallsPlugin)

lazy val publishSettings = Seq(
  publishMavenStyle := true,
  publishArtifact := true,
  publishTo := {
    val nexus = "https://oss.sonatype.org/"
    if (isSnapshot.value)
      Some("snapshots" at nexus + "content/repositories/snapshots")
    else
      Some("releases"  at nexus + "service/local/staging/deploy/maven2")
  },
  publishArtifact in Test := false,
  licenses := Seq("Apache 2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0")),
  homepage := Some(url("https://github.com/travisbrown/catbird")),
  autoAPIMappings := true,
  apiURL := Some(url("https://travisbrown.github.io/catbird/api/")),
  pomExtra := (
    <scm>
      <url>git://github.com/travisbrown/catbird.git</url>
      <connection>scm:git://github.com/travisbrown/catbird.git</connection>
    </scm>
    <developers>
      <developer>
        <id>travisbrown</id>
        <name>Travis Brown</name>
        <url>https://twitter.com/travisbrown</url>
      </developer>
    </developers>
  )
)

lazy val noPublish = Seq(
  publish := {},
  publishLocal := {}
)
