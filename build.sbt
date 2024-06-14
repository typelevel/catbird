lazy val `catbird-root` = (project in file("."))
  .aggregate(allProjects.map(_.project) *)
  .settings(
    publish / skip := true,
    publishArtifact := false,
    ScalaUnidoc / unidoc / unidocProjectFilter := {
      val excluded = benchmark.componentProjects ++ effect3.componentProjects
        //      `scalafix-input`,
        //      `scalafix-output`,
        //      `scalafix-tests`
      inAnyProject -- inProjects(excluded.map(_.project) *)
    },
// TODO    addMappingsToSiteDir(ScalaUnidoc / packageDoc / mappings, docMappingsApiDir),
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
  .enablePlugins(FinaglePlugin)



/*

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
  .aggregate(util, effect3, finagle, benchmark, `scalafix-rules`, `scalafix-tests`, FinaglePlugin.rootFinagle)
  .dependsOn(util, finagle)

lazy val util = project
  .settings(moduleName := "catbird-util")
  .settings(allSettings)
  .settings(
    libraryDependencies += "com.twitter" %% "util-core" % finagleVersion,
    Test / scalacOptions ~= {
      _.filterNot(Set("-Yno-imports", "-Yno-predef"))
    }
  )

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
*/
