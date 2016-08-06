import ReleaseTransformations._

val bijectionVersion = "0.9.2"
val catsVersion = "0.7.0-SNAPSHOT"
val utilVersion = "6.35.0"
val finagleVersion = "6.36.0"

lazy val buildSettings = Seq(
  organization := "io.catbird",
  scalaVersion := "2.11.8"
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
  "-Xfuture",
  "-Ywarn-unused-import",
  "-Yno-imports",
  "-Yno-predef"
)

lazy val baseSettings = Seq(
  scalacOptions ++= compilerOptions,
  scalacOptions in (Compile, console) ~= {
    _.filterNot(Set("-Ywarn-unused-import", "-Yno-imports", "-Yno-predef"))
  },
  scalacOptions in (Test, console) ~= {
    _.filterNot(Set("-Ywarn-unused-import", "-Yno-imports", "-Yno-predef"))
  },
  libraryDependencies ++= Seq(
    "org.typelevel" %% "cats-core" % catsVersion,
    compilerPlugin("org.spire-math" %% "kind-projector" % "0.8.0")
  ),
  resolvers += Resolver.sonatypeRepo("snapshots"),
  wartremoverWarnings in (Compile, compile) ++= Warts.allBut(
    Wart.NoNeedForMonad
  ),
  ScoverageSbtPlugin.ScoverageKeys.coverageHighlighting := true
)

lazy val allSettings = buildSettings ++ baseSettings ++ publishSettings

lazy val root = project.in(file("."))
  .settings(allSettings ++ noPublishSettings)
  .settings(unidocSettings ++ site.settings ++ ghpages.settings)
  .settings(
    site.addMappingsToSiteDir(mappings in (ScalaUnidoc, packageDoc), "api"),
    git.remoteRepo := "git@github.com:travisbrown/catbird.git"
  )
  .settings(
    initialCommands in console :=
      """
        |import com.twitter.finagle._
        |import com.twitter.util._
        |import io.catbird.finagle._
        |import io.catbird.util._
      """.stripMargin
  )
  .aggregate(util, finagle, bijections, tests, benchmark)
  .dependsOn(util, finagle, bijections)

lazy val tests = project
  .settings(allSettings ++ noPublishSettings)
  .settings(
    libraryDependencies ++= Seq(
      "org.scalacheck" %% "scalacheck" % "1.12.5",
      "org.scalatest" %% "scalatest" % "3.0.0-M9",
      "org.typelevel" %% "cats-laws" % catsVersion,
      "org.typelevel" %% "discipline" % "0.4"
    ),
    scalacOptions ~= {
      _.filterNot(Set("-Yno-imports", "-Yno-predef"))
    },
    ScoverageSbtPlugin.ScoverageKeys.coverageExcludedPackages := "io\\.catbird\\.tests\\..*"
  )
  .dependsOn(util, finagle, bijections)

lazy val util = project
  .settings(moduleName := "catbird-util")
  .settings(allSettings)
  .settings(
    libraryDependencies ++= Seq(
      "com.twitter" %% "util-core" % utilVersion
    )
  )

lazy val finagle = project
  .settings(moduleName := "catbird-finagle")
  .settings(allSettings)
  .settings(
    libraryDependencies ++= Seq(
      "com.twitter" %% "finagle-core" % finagleVersion
    )
  )
  .dependsOn(util)

lazy val bijections = project
  .settings(moduleName := "catbird-bijections")
  .settings(allSettings)
  .settings(
    libraryDependencies ++= Seq(
      "com.twitter" %% "bijection-core" % bijectionVersion,
      "com.twitter" %% "finagle-core" % finagleVersion
    )
  )
  .dependsOn(util)

lazy val benchmark = project
  .settings(moduleName := "catbird-benchmark")
  .settings(allSettings)
  .settings(noPublishSettings)
  .settings(
    libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.0-M9",
    scalacOptions ~= {
      _.filterNot(Set("-Yno-imports", "-Yno-predef"))
    }
  )
  .enablePlugins(JmhPlugin)
  .dependsOn(util)

lazy val publishSettings = Seq(
  releaseCrossBuild := true,
  releasePublishArtifactsAction := PgpKeys.publishSigned.value,
  homepage := Some(url("https://github.com/travisbrown/catbird")),
  licenses := Seq("Apache 2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0")),
  publishMavenStyle := true,
  publishArtifact in Test := false,
  pomIncludeRepository := { _ => false },
  publishTo := {
    val nexus = "https://oss.sonatype.org/"
    if (isSnapshot.value)
      Some("snapshots" at nexus + "content/repositories/snapshots")
    else
      Some("releases"  at nexus + "service/local/staging/deploy/maven2")
  },
  autoAPIMappings := true,
  apiURL := Some(url("https://travisbrown.github.io/catbird/api/")),
  scmInfo := Some(
    ScmInfo(
      url("https://github.com/travisbrown/catbird"),
      "scm:git:git@github.com:travisbrown/catbird.git"
    )
  ),
  pomExtra := (
    <developers>
      <developer>
        <id>travisbrown</id>
        <name>Travis Brown</name>
        <url>https://twitter.com/travisbrown</url>
      </developer>
    </developers>
  )
)

lazy val noPublishSettings = Seq(
  publish := (),
  publishLocal := (),
  publishArtifact := false
)

lazy val sharedReleaseProcess = Seq(
  releaseProcess := Seq[ReleaseStep](
    checkSnapshotDependencies,
    inquireVersions,
    runClean,
    runTest,
    setReleaseVersion,
    commitReleaseVersion,
    tagRelease,
    publishArtifacts,
    setNextVersion,
    commitNextVersion,
    ReleaseStep(action = Command.process("sonatypeReleaseAll", _)),
    pushChanges
  )
)

credentials ++= (
  for {
    username <- Option(System.getenv().get("SONATYPE_USERNAME"))
    password <- Option(System.getenv().get("SONATYPE_PASSWORD"))
  } yield Credentials(
    "Sonatype Nexus Repository Manager",
    "oss.sonatype.org",
    username,
    password
  )
).toSeq
