val catsVersion = "2.6.1"

// For the transition period, we publish artifacts for both cats-effect 2.x and 3.x
val catsEffectVersion = "2.5.3"
val catsEffect3Version = "3.2.7"

val utilVersion = "21.8.0"
val finagleVersion = "21.8.0"

val Scala212 = "2.12.14"
val Scala213 = "2.13.6"
val Scala3 = "3.0.2"

ThisBuild / crossScalaVersions := Seq(Scala212, Scala213, Scala3)
ThisBuild / scalaVersion := Scala213
ThisBuild / scalaBinaryVersion := "2.13"

ThisBuild / organization := "io.catbird"

(Global / onChangedBuildSource) := ReloadOnSourceChanges

def compilerOptions(scalaVersion: String): Seq[String] =
  if (ScalaArtifacts.isScala3(scalaVersion))
    Seq(
      "-deprecation",
      "-encoding",
      "utf-8",
      "-explain-types",
      "-feature",
      "-language:existentials",
      "-language:higherKinds",
      "-language:implicitConversions",
      "-unchecked",
      "-Ykind-projector"
    )
  else
    Seq(
      "-deprecation",
      "-encoding",
      "UTF-8",
      "-feature",
      "-language:existentials",
      "-language:higherKinds",
      "-unchecked",
      "-Ywarn-dead-code",
      "-Ywarn-numeric-widen",
      "-Yno-imports",
      "-Yno-predef"
    ) ++ (if (priorTo2_13(scalaVersion))
            Seq(
              "-Ywarn-unused-import",
              "-Yno-adapted-args",
              "-Xfuture"
            )
          else
            Seq(
              "-Ywarn-unused:imports",
              "-Ymacro-annotations"
            ))

val docMappingsApiDir = settingKey[String]("Subdirectory in site target directory for API docs")

lazy val baseSettings = Seq(
  scalacOptions ++= compilerOptions(scalaVersion.value),
  (Compile / console / scalacOptions) ~= {
    _.filterNot(Set("-Ywarn-unused-import", "-Ywarn-unused:imports", "-Yno-imports", "-Yno-predef"))
  },
  (Test / console / scalacOptions) ~= {
    _.filterNot(Set("-Ywarn-unused-import", "-Ywarn-unused:imports", "-Yno-imports", "-Yno-predef"))
  },
  libraryDependencies ++= (
    if (ScalaArtifacts.isScala3(scalaVersion.value)) Nil
    else Seq(compilerPlugin(("org.typelevel" %% "kind-projector" % "0.13.0").cross(CrossVersion.full)))
  ),
  libraryDependencies ++= Seq(
    "org.typelevel" %% "cats-core" % catsVersion,
    "org.scalacheck" %% "scalacheck" % "1.15.4" % Test,
    "org.scalatest" %% "scalatest" % "3.2.9" % Test,
    "org.typelevel" %% "cats-laws" % catsVersion % Test,
    "org.typelevel" %% "discipline-core" % "1.1.5" % Test,
    "org.typelevel" %% "discipline-scalatest" % "2.1.5" % Test
  ),
  resolvers += Resolver.sonatypeRepo("snapshots"),
  docMappingsApiDir := "api"
)

lazy val allSettings = baseSettings ++ publishSettings

lazy val root = project
  .in(file("."))
  .enablePlugins(GhpagesPlugin, ScalaUnidocPlugin)
  .settings(allSettings ++ noPublishSettings)
  .settings(
    (ScalaUnidoc / unidoc / unidocProjectFilter) := inAnyProject -- inProjects(benchmark, effect3),
    addMappingsToSiteDir((ScalaUnidoc / packageDoc / mappings), docMappingsApiDir),
    git.remoteRepo := "git@github.com:travisbrown/catbird.git"
  )
  .settings(
    (console / initialCommands) :=
      """
        |import com.twitter.finagle._
        |import com.twitter.util._
        |import io.catbird.finagle._
        |import io.catbird.util._
      """.stripMargin
  )
  .aggregate(util, effect, effect3, finagle, benchmark)
  .dependsOn(util, effect, finagle)

lazy val util = project
  .settings(moduleName := "catbird-util")
  .settings(allSettings)
  .settings(
    libraryDependencies +=
      ("com.twitter" %% "util-core" % utilVersion).cross(CrossVersion.for3Use2_13),
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
    libraryDependencies ++=
      Seq(
        ("com.twitter" %% "util-core" % utilVersion).cross(CrossVersion.for3Use2_13),
        ("com.twitter" %% "finagle-core" % finagleVersion).cross(CrossVersion.for3Use2_13)
      ),
    (Test / scalacOptions) ~= {
      _.filterNot(Set("-Yno-imports", "-Yno-predef"))
    }
  )
  .dependsOn(util)

lazy val benchmark = project
  .settings(moduleName := "catbird-benchmark")
  .settings(allSettings)
  .settings(noPublishSettings)
  .settings(
    libraryDependencies += "org.scalatest" %% "scalatest" % "3.2.9",
    scalacOptions ~= {
      _.filterNot(Set("-Yno-imports", "-Yno-predef"))
    }
  )
  .enablePlugins(JmhPlugin)
  .dependsOn(util)

lazy val publishSettings = Seq(
  releaseCrossBuild := true,
  releasePublishArtifactsAction := PgpKeys.publishSigned.value,
  releaseVcsSign := true,
  homepage := Some(url("https://github.com/travisbrown/catbird")),
  licenses := Seq("Apache 2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0")),
  publishMavenStyle := true,
  (Test / publishArtifact) := false,
  pomIncludeRepository := { _ => false },
  publishTo := {
    val nexus = "https://oss.sonatype.org/"
    if (isSnapshot.value)
      Some("snapshots".at(nexus + "content/repositories/snapshots"))
    else
      Some("releases".at(nexus + "service/local/staging/deploy/maven2"))
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
  publish := {},
  publishLocal := {},
  publishArtifact := false
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

def priorTo2_13(scalaVersion: String): Boolean =
  CrossVersion.partialVersion(scalaVersion) match {
    case Some((2, minor)) if minor < 13 => true
    case _                              => false
  }

ThisBuild / githubWorkflowJavaVersions := Seq("adopt@1.8")
// No auto-publish atm. Remove this line to generate publish stage
ThisBuild / githubWorkflowPublishTargetBranches := Seq.empty
ThisBuild / githubWorkflowBuild := Seq(
  WorkflowStep.Sbt(
    List(
      "clean",
      "coverage",
      "test",
      "scalastyle",
      "scalafmtCheck",
      "scalafmtSbtCheck",
      "test:scalafmtCheck",
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
