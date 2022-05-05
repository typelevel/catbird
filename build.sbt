val catsVersion = "2.6.1"

// For the transition period, we publish artifacts for both cats-effect 2.x and 3.x
val catsEffectVersion = "2.5.4"
val catsEffect3Version = "3.2.9"

val utilVersion = "22.4.0"
val finagleVersion = "22.4.0"

ThisBuild / crossScalaVersions := Seq("2.12.15", "2.13.6")
ThisBuild / scalaVersion := crossScalaVersions.value.last

ThisBuild / organization := "org.typelevel"

(Global / onChangedBuildSource) := ReloadOnSourceChanges

val docMappingsApiDir = settingKey[String]("Subdirectory in site target directory for API docs")

lazy val baseSettings = Seq(
  (Compile / console / scalacOptions) ~= {
    _.filterNot(Set("-Ywarn-unused-import", "-Ywarn-unused:imports", "-Yno-imports", "-Yno-predef"))
  },
  (Test / console / scalacOptions) ~= {
    _.filterNot(Set("-Ywarn-unused-import", "-Ywarn-unused:imports", "-Yno-imports", "-Yno-predef"))
  },
  libraryDependencies ++= Seq(
    "org.typelevel" %% "cats-core" % catsVersion,
    "org.scalacheck" %% "scalacheck" % "1.15.4" % Test,
    "org.scalatest" %% "scalatest" % "3.2.10" % Test,
    "org.typelevel" %% "cats-laws" % catsVersion % Test,
    "org.typelevel" %% "discipline-core" % "1.3.0" % Test,
    "org.typelevel" %% "discipline-scalatest" % "2.1.5" % Test,
    compilerPlugin(("org.typelevel" %% "kind-projector" % "0.13.2").cross(CrossVersion.full))
  ),
  resolvers += Resolver.sonatypeRepo("snapshots"),
  docMappingsApiDir := "api"
)

lazy val allSettings = baseSettings ++ publishSettings

lazy val root = project
  .in(file("."))
  .enablePlugins(GhpagesPlugin, ScalaUnidocPlugin)
  .settings(allSettings)
  .settings(
    (ScalaUnidoc / unidoc / unidocProjectFilter) := inAnyProject -- inProjects(
      benchmark,
      effect3,
      `scalafix-input`,
      `scalafix-output`,
      `scalafix-tests`
    ),
    addMappingsToSiteDir((ScalaUnidoc / packageDoc / mappings), docMappingsApiDir),
    git.remoteRepo := "git@github.com:typelevel/catbird.git",
    publish / skip := true
  )
  .settings(
    (console / initialCommands) :=
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
    libraryDependencies += "com.twitter" %% "util-core" % utilVersion,
    (Test / scalacOptions) ~= {
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
    (Test / scalacOptions) ~= {
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
    (Test / scalacOptions) ~= {
      _.filterNot(Set("-Yno-imports", "-Yno-predef"))
    }
  )
  .dependsOn(util, util % "test->test")

lazy val finagle = project
  .settings(moduleName := "catbird-finagle")
  .settings(allSettings)
  .settings(
    libraryDependencies += "com.twitter" %% "finagle-core" % finagleVersion,
    (Test / scalacOptions) ~= {
      _.filterNot(Set("-Yno-imports", "-Yno-predef"))
    }
  )
  .dependsOn(util)

lazy val benchmark = project
  .settings(allSettings)
  .settings(
    moduleName := "catbird-benchmark",
    publish / skip := true,
    libraryDependencies += "org.scalatest" %% "scalatest" % "3.2.10",
    scalacOptions ~= {
      _.filterNot(Set("-Yno-imports", "-Yno-predef"))
    }
  )
  .enablePlugins(JmhPlugin)
  .dependsOn(util)

lazy val publishSettings = Seq(
  homepage := Some(url("https://github.com/typelevel/catbird")),
  licenses := Seq("Apache 2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0")),
  (Test / publishArtifact) := false,
  pomIncludeRepository := { _ => false },
  autoAPIMappings := true,
  apiURL := Some(url("https://typelevel.org/catbird/api/")),
  scmInfo := Some(
    ScmInfo(
      url("https://github.com/typelevel/catbird"),
      "scm:git:git@github.com:typelevel/catbird.git"
    )
  ),
  developers += Developer("travisbrown", "Travis Brown", "", url("https://twitter.com/travisbrown"))
)

ThisBuild / githubWorkflowJavaVersions := Seq("adopt@1.8")
ThisBuild / githubWorkflowPublishTargetBranches := Seq(RefPredicate.StartsWith(Ref.Tag("v")))
ThisBuild / githubWorkflowPublish := Seq(
  WorkflowStep.Sbt(
    List("ci-release"),
    env = Map(
      "PGP_PASSPHRASE" -> "${{ secrets.PGP_PASSPHRASE }}",
      "PGP_SECRET" -> "${{ secrets.PGP_SECRET }}",
      "SONATYPE_PASSWORD" -> "${{ secrets.SONATYPE_PASSWORD }}",
      "SONATYPE_USERNAME" -> "${{ secrets.SONATYPE_USERNAME }}"
    )
  )
)
ThisBuild / githubWorkflowBuild := Seq(
  WorkflowStep.Sbt(
    List(
      "clean",
      "coverage",
      "test",
      "scalastyle",
      "scalafmtCheck",
      "scalafmtSbtCheck",
      "Test / scalafmtCheck",
      "unidoc",
      "coverageReport"
    ),
    env = Map(
      "SBT_OPTS" -> "-J-Xmx8G"
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
