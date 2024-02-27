val catsVersion = "2.9.0"

ThisBuild / tlBaseVersion := BaseVersion(finagleVersion)
ThisBuild / tlVersionIntroduced := // test bincompat starting from the beginning of this series
  List("2.12", "2.13").map(_ -> s"${tlBaseVersion.value}.0").toMap
ThisBuild / tlCiHeaderCheck := false

// For the transition period, we publish artifacts for both cats-effect 2.x and 3.x
val catsEffectVersion = "2.5.5"
val catsEffect3Version = "3.4.3"

ThisBuild / crossScalaVersions := Seq("2.12.17", "2.13.13")

ThisBuild / libraryDependencySchemes ++= Seq(
  // scoverage depends on scala-xml 1, but discipline-scalatest transitively pulls in scala-xml 2
  // this is normally discouraged but was recommended by one of the scoverage maintainers in the Typelevel Discord
  "org.scala-lang.modules" %% "scala-xml" % VersionScheme.Always
)

Global / onChangedBuildSource := ReloadOnSourceChanges

val docMappingsApiDir = settingKey[String]("Subdirectory in site target directory for API docs")

lazy val baseSettings = Seq(
  // Finagle releases monthly using a {year}.{month}.{patch} version scheme.
  // The combination of year and month is effectively a major version, because
  // each monthly release often contains binary-incompatible changes.
  // This means we should release at least monthly as well, when Finagle does,
  // but in between those monthly releases, maintain binary compatibility.
  // This is effectively PVP style versioning.
  // We set this at the project-level instead of ThisBuild to circumvent sbt-typelevel checks.
  versionScheme := Some("pvp"),
  libraryDependencies ++= Seq(
    "org.typelevel" %% "cats-core" % catsVersion,
    "org.scalacheck" %% "scalacheck" % "1.17.0" % Test,
    "org.scalatest" %% "scalatest" % "3.2.14" % Test,
    "org.typelevel" %% "cats-laws" % catsVersion % Test,
    "org.typelevel" %% "discipline-core" % "1.5.1" % Test,
    "org.typelevel" %% "discipline-scalatest" % "2.2.0" % Test
  ),
  resolvers ++= Resolver.sonatypeOssRepos("snapshots"),
  docMappingsApiDir := "api",
  autoAPIMappings := true,

  // disable automatic copyright header creation otherwise added via sbt-typelevel
  headerMappings := Map.empty
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
    addMappingsToSiteDir(ScalaUnidoc / packageDoc / mappings, docMappingsApiDir),
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
  .aggregate(util, effect, effect3, finagle, benchmark, `scalafix-rules`, `scalafix-tests`, FinaglePlugin.rootFinagle)
  .dependsOn(util, effect, finagle)

lazy val util = project
  .settings(moduleName := "catbird-util")
  .settings(allSettings)
  .settings(
    libraryDependencies += "com.twitter" %% "util-core" % finagleVersion,
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
    libraryDependencies += "com.twitter" %% "finagle-core" % finagleVersion,
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
    libraryDependencies += "org.scalatest" %% "scalatest" % "3.2.14",
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
      "mimaReportBinaryIssues",
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

lazy val `scalafix-rules` = (project in file("scalafix/rules"))
  .settings(allSettings)
  .settings(
    moduleName := "catbird-scalafix",
    libraryDependencies ++= Seq(
      "ch.epfl.scala" %% "scalafix-core" % _root_.scalafix.sbt.BuildInfo.scalafixVersion
    )
  )

lazy val `scalafix-input` = (project in file("scalafix/input"))
  .settings(
    libraryDependencies ++= Seq(
      "io.catbird" %% "catbird-util" % "21.8.0"
    ),
    scalacOptions ~= { _.filterNot(_ == "-Xfatal-warnings") },
    semanticdbEnabled := true,
    semanticdbVersion := scalafixSemanticdb.revision
  )
  .enablePlugins(NoPublishPlugin)

lazy val `scalafix-output` = (project in file("scalafix/output"))
  .settings(
    githubWorkflowArtifactUpload := false,
    scalacOptions ~= { _.filterNot(_ == "-Xfatal-warnings") }
  )
  .dependsOn(util)
  .enablePlugins(NoPublishPlugin)

lazy val `scalafix-tests` = (project in file("scalafix/tests"))
  .settings(allSettings)
  .settings(
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
  .enablePlugins(
    ScalafixTestkitPlugin,
    NoPublishPlugin
  )
