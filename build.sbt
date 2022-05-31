val catsVersion = "2.7.0"

ThisBuild / tlBaseVersion := "21.8"
ThisBuild / tlMimaPreviousVersions := Set.empty

// Finagle releases monthly using a {year}.{month}.{patch} version scheme.
// The combination of year and month is effectively a major version, because
// each monthly release often contains binary-incompatible changes.
// This means we should release at least monthly as well, when Finagle does,
// but in between those monthly releases, maintain binary compatibility.
ThisBuild / versionScheme := Option("year-month-patch")

// For the transition period, we publish artifacts for both cats-effect 2.x and 3.x
val catsEffectVersion = "2.5.5"
val catsEffect3Version = "3.3.11"

ThisBuild / crossScalaVersions := Seq("2.12.15", "2.13.8")

Global / onChangedBuildSource := ReloadOnSourceChanges

val docMappingsApiDir = settingKey[String]("Subdirectory in site target directory for API docs")

lazy val baseSettings = Seq(
  libraryDependencies ++= Seq(
    "org.typelevel" %% "cats-core" % catsVersion,
    "org.scalacheck" %% "scalacheck" % "1.16.0" % Test,
    "org.scalatest" %% "scalatest" % "3.2.12" % Test,
    "org.typelevel" %% "cats-laws" % catsVersion % Test,
    "org.typelevel" %% "discipline-core" % "1.5.1" % Test,
    "org.typelevel" %% "discipline-scalatest" % "2.1.5" % Test
  ),
  resolvers += Resolver.sonatypeRepo("snapshots"),
  docMappingsApiDir := "api",
  autoAPIMappings := true
)

lazy val allSettings = baseSettings

lazy val root = project
  .in(file("."))
  .enablePlugins(GhpagesPlugin, ScalaUnidocPlugin, NoPublishPlugin)
  .settings(allSettings)
  .settings(
    ScalaUnidoc / unidoc / unidocProjectFilter := inAnyProject -- inProjects(
      benchmark,
      effect3,
      `scalafix-input`,
      `scalafix-output`,
      `scalafix-tests`
    ),
    addMappingsToSiteDir((ScalaUnidoc / packageDoc / mappings), docMappingsApiDir),
    git.remoteRepo := "git@github.com:typelevel/catbird.git"
  )
  .settings(
    console / initialCommands :=
      """
        |import com.twitter.finagle._
        |import com.twitter.util._
        |import org.typelevel.catbird.finagle._
        |import org.typelevel.catbird.util._
      """.stripMargin
  )
  .aggregate(util, effect, effect3, finagle, benchmark, `scalafix-rules`, `scalafix-tests`)
  .dependsOn(util, effect, finagle)

lazy val util = project
  .settings(moduleName := "catbird-util")
  .settings(allSettings)
  .settings(
    libraryDependencies += "com.twitter" %% "util-core" % (tlBaseVersion.value + ".0"),
    Test / scalacOptions ~= {
      _.filterNot(Set("-Yno-imports", "-Yno-predef"))
    }
  )

lazy val effect = project
  .settings(moduleName := "catbird-effect")
  .settings(allSettings)
  .settings(
    libraryDependencies ++= Seq(
      "org.typelevel" %% "cats-effect" % catsEffectVersion,
      "org.typelevel" %% "cats-effect-laws" % catsEffectVersion % Test
    ),
    Test / scalacOptions ~= {
      _.filterNot(Set("-Yno-imports", "-Yno-predef"))
    }
  )
  .dependsOn(util, util % "test->test")

lazy val effect3 = project
  .in(file("effect3"))
  .settings(moduleName := "catbird-effect3")
  .settings(allSettings)
  .settings(
    libraryDependencies ++= Seq(
      "org.typelevel" %% "cats-effect" % catsEffect3Version,
      "org.typelevel" %% "cats-effect-laws" % catsEffect3Version % Test,
      "org.typelevel" %% "cats-effect-testkit" % catsEffect3Version % Test
    ),
    Test / scalacOptions ~= {
      _.filterNot(Set("-Yno-imports", "-Yno-predef"))
    }
  )
  .dependsOn(util, util % "test->test")

lazy val finagle = project
  .settings(moduleName := "catbird-finagle")
  .settings(allSettings)
  .settings(
    libraryDependencies += "com.twitter" %% "finagle-core" % (tlBaseVersion.value + ".0"),
    Test / scalacOptions ~= {
      _.filterNot(Set("-Yno-imports", "-Yno-predef"))
    }
  )
  .dependsOn(util)

lazy val benchmark = project
  .enablePlugins(NoPublishPlugin)
  .settings(allSettings)
  .settings(
    moduleName := "catbird-benchmark",
    libraryDependencies += "org.scalatest" %% "scalatest" % "3.2.12",
    scalacOptions ~= {
      _.filterNot(Set("-Yno-imports", "-Yno-predef"))
    }
  )
  .enablePlugins(JmhPlugin)
  .dependsOn(util)

ThisBuild / apiURL := Some(url("https://typelevel.org/catbird/api/"))
ThisBuild / developers += Developer("travisbrown", "Travis Brown", "", url("https://twitter.com/travisbrown"))

ThisBuild / githubWorkflowBuild := Seq(
  WorkflowStep.Sbt(
    List(
      "clean",
      "coverage",
      "test",
      "scalastyle",
      "scalafmtCheckAll",
      "scalafmtSbtCheck",
      "unidoc",
      "coverageReport"
    )
  ),
  WorkflowStep.Use(
    UseRef.Public("codecov", "codecov-action", "v1"),
    name = Some("Code coverage analysis")
  )
)

lazy val `scalafix-rules` = (project in file("scalafix/rules")).settings(
  moduleName := "catbird-scalafix",
  libraryDependencies ++= Seq(
    "ch.epfl.scala" %% "scalafix-core" % _root_.scalafix.sbt.BuildInfo.scalafixVersion
  )
)

lazy val `scalafix-input` = (project in file("scalafix/input")).settings(
  publish / skip := true,
  libraryDependencies ++= Seq(
    "io.catbird" %% "catbird-util" % "21.8.0"
  ),
  scalacOptions ~= { _.filterNot(_ == "-Xfatal-warnings") },
  semanticdbEnabled := true,
  semanticdbVersion := scalafixSemanticdb.revision
)

lazy val `scalafix-output` = (project in file("scalafix/output"))
  .settings(
    githubWorkflowArtifactUpload := false,
    publish / skip := true,
    scalacOptions ~= { _.filterNot(_ == "-Xfatal-warnings") }
  )
  .dependsOn(util)

lazy val `scalafix-tests` = (project in file("scalafix/tests"))
  .settings(
    publish / skip := true,
    libraryDependencies += {
      import _root_.scalafix.sbt.BuildInfo.scalafixVersion
      ("ch.epfl.scala" % "scalafix-testkit" % scalafixVersion % Test).cross(CrossVersion.full)
    },
    scalafixTestkitOutputSourceDirectories := (`scalafix-output` / Compile / unmanagedSourceDirectories).value,
    scalafixTestkitInputSourceDirectories := (`scalafix-input` / Compile / unmanagedSourceDirectories).value,
    scalafixTestkitInputClasspath := (`scalafix-input` / Compile / fullClasspath).value,
    scalafixTestkitInputScalacOptions := (`scalafix-input` / Compile / scalacOptions).value,
    scalafixTestkitInputScalaVersion := (`scalafix-input` / Compile / scalaVersion).value
  )
  .dependsOn(`scalafix-rules`)
  .enablePlugins(ScalafixTestkitPlugin)
