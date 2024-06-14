import sbt.{Def, *}
import _root_.scalafix.sbt.ScalafixTestkitPlugin.autoImport.*
import _root_.scalafix.sbt.{ScalafixPlugin, ScalafixTestkitPlugin}
import com.typesafe.tools.mima.plugin.MimaPlugin
import com.typesafe.tools.mima.plugin.MimaPlugin.autoImport.*
import org.scalajs.jsenv.JSEnv
import org.scalajs.jsenv.nodejs.NodeJSEnv
import org.scalajs.sbtplugin.ScalaJSPlugin.autoImport.*
import org.typelevel.sbt.TypelevelMimaPlugin.autoImport.*
import org.typelevel.sbt.TypelevelSettingsPlugin
import org.typelevel.sbt.TypelevelSettingsPlugin.autoImport.*
import org.typelevel.sbt.TypelevelSonatypeCiReleasePlugin.autoImport.*
import org.typelevel.sbt.TypelevelSonatypePlugin.autoImport.*
import org.typelevel.sbt.TypelevelVersioningPlugin.autoImport.*
import org.typelevel.sbt.gha.GenerativePlugin.autoImport.*
import org.typelevel.sbt.gha.GitHubActionsPlugin.autoImport.*
import org.typelevel.sbt.mergify.MergifyPlugin
import org.typelevel.sbt.mergify.MergifyPlugin.autoImport.*
import sbt.Keys.*
import sbt.internal.ProjectMatrix
import sbt.librarymanagement.DependencyBuilders.OrganizationArtifactName
import sbtprojectmatrix.ProjectMatrixPlugin
import sbtprojectmatrix.ProjectMatrixPlugin.autoImport.*
import scalafix.sbt.ScalafixPlugin.autoImport.*
import com.typesafe.tools.mima.plugin.MimaKeys.*
import com.typesafe.tools.mima.plugin.SbtMima
import org.typelevel.sbt.NoPublishPlugin
import org.typelevel.sbt.TypelevelCiPlugin.autoImport.*
import pl.project13.scala.sbt.JmhPlugin
import sbt.nio.Keys.{ReloadOnSourceChanges, onChangedBuildSource}

object FinaglePlugin extends AutoPlugin {

  override def trigger = noTrigger

  override def requires: Plugins =
    ProjectMatrixPlugin && ScalafixPlugin && MimaPlugin && MergifyPlugin && TypelevelSettingsPlugin &&
      WarnNonUnitStatements && JmhPlugin

  object autoImport {
    val docMappingsApiDir = settingKey[String]("Subdirectory in site target directory for API docs")

    lazy val allProjects: Seq[Project] =
      List(
        util,
        effect3,
        finagle,
      ).flatMap { pm =>
          if (Set("scalafix-input", "scalafix-output", "scalafix-input-dependency", "scalafix-output-dependency", "scalafix-tests").contains(pm.id)) List(pm)
          else List(pm, latestVersionAlias(pm))
        }
        .flatMap(_.componentProjects)

    lazy val benchmark = FinaglePlugin.benchmark
    lazy val effect3 = FinaglePlugin.effect3
  }

  import autoImport.*

  private val currentTwitterVersion = Version("22.12.0").get

  // When a new version is released, move what was previously the current version into the list of old versions.
  // This plugin will automatically release a new suffixed artifact that can be used by users with bincompat issues.
  // Don't forget to regenerate the GitHub Actions workflow by running the `githubWorkflowGenerate` sbt task.
  private val oldVersions = List(
    "22.7.0",
    "22.4.0",
  )
    .flatMap(Version(_))

  private val supportedVersions = (currentTwitterVersion :: oldVersions).sorted.reverse

  private val SCALA_2_13: String = "2.13.10"
  private val SCALA_2_12 = "2.12.17"
  private val Scala2Versions: Seq[String] = Seq(SCALA_2_13, SCALA_2_12)
  private val catsEffect3Version = "3.4.3"
  private val catsVersion = "2.9.0"

  private def projectMatrixForSupportedTwitterVersions(id: String,
                                                       path: String)
                                                      (s: Version => List[Setting[?]]): ProjectMatrix =
    supportedVersions.foldLeft(ProjectMatrix(id, file(path)))(addTwitterCustomRow(s))

  private def addTwitterCustomRow(s: Version => List[Setting[?]])
                                 (p: ProjectMatrix, v: Version): ProjectMatrix =
    p.customRow(
      scalaVersions = Scala2Versions,
      axisValues = List(TwitterVersion(v), VirtualAxis.jvm),
      _.settings(
        s(v),
        // Finagle releases monthly using a {year}.{month}.{patch} version scheme.
        // The combination of year and month is effectively a major version, because
        // each monthly release often contains binary-incompatible changes.
        // This means we should release at least monthly as well, when Finagle does,
        // but in between those monthly releases, maintain binary compatibility.
        // This is effectively PVP style versioning.
        // We set this at the project-level instead of ThisBuild to circumvent sbt-typelevel checks.
        // TODO is this still desired?
        versionScheme := Some("pvp"),
      )
    )

  private def latestVersionAlias(p: ProjectMatrix): ProjectMatrix =
    ProjectMatrix(s"${p.id}-latest", file(s".${p.id}-latest"))
      .customRow(
        scalaVersions = Scala2Versions,
        axisValues = List(TwitterVersion(currentTwitterVersion), VirtualAxis.jvm),
        _.settings(
          moduleName := p.id,
          tlVersionIntroduced := Map("2.12" -> "1.1.0", "2.13" -> "1.1.0"),
        )
      )
      .dependsOn(p)

  lazy val util =
    projectMatrixForSupportedTwitterVersions("catbird-util", "util") { v =>
      List(
        moduleName := name.value + s"-$v",
        libraryDependencies ++= {
          Seq(
            "com.twitter" %% "util-core" % v,
          ) // TODO ++ (if (scalaVersion.value.startsWith("2")) scala2CompilerPlugins else Nil)
        },
        Test / scalacOptions ~= {
          _.filterNot(Set("-Yno-imports", "-Yno-predef"))
        }
      )
    }

  lazy val effect3 =
    projectMatrixForSupportedTwitterVersions("catbird-effect3", "effect3") { v =>
      List(
        moduleName := name.value + s"-$v",
        libraryDependencies ++= {
          Seq(
            "org.typelevel" %% "cats-effect" % catsEffect3Version,
            "org.typelevel" %% "cats-effect-laws" % catsEffect3Version % Test,
            "org.typelevel" %% "cats-effect-testkit" % catsEffect3Version % Test
          ) // TODO ++ (if (scalaVersion.value.startsWith("2")) scala2CompilerPlugins else Nil)
        },
        Test / scalacOptions ~= {
          _.filterNot(Set("-Yno-imports", "-Yno-predef"))
        }
      )
    }
      .dependsOn(util, util % "test->test")

  lazy val finagle =
    projectMatrixForSupportedTwitterVersions("catbird-finagle", "finagle") { v =>
      List(
        moduleName := name.value + s"-$v",
        libraryDependencies ++= {
          Seq(
            "com.twitter" %% "finagle-core" % v
          ) // TODO ++ (if (scalaVersion.value.startsWith("2")) scala2CompilerPlugins else Nil)
        },
        Test / scalacOptions ~= {
          _.filterNot(Set("-Yno-imports", "-Yno-predef"))
        }
      )
    }
      .dependsOn(util)

  lazy val benchmark =
    projectMatrixForSupportedTwitterVersions("benchmark", "benchmark") { v =>
      List(
        moduleName := name.value + s"-$v",
        libraryDependencies ++= {
          Seq(
            "org.scalatest" %% "scalatest" % "3.2.14"
          ) // TODO ++ (if (scalaVersion.value.startsWith("2")) scala2CompilerPlugins else Nil)
        },
        Test / scalacOptions ~= {
          _.filterNot(Set("-Yno-imports", "-Yno-predef"))
        }
      )
    }
      .enablePlugins(JmhPlugin)
      .dependsOn(util)

  override def buildSettings: Seq[Def.Setting[?]] = Seq(
    githubWorkflowScalaVersions := Seq("per-project-matrix"),
    githubWorkflowBuildSbtStepPreamble := Nil,
    tlBaseVersion := "22.12",
    tlCiHeaderCheck := false,

    // TODO this may change as we support multiple Twitter versions
    tlVersionIntroduced := // test bincompat starting from the beginning of this series
      List("2.12", "2.13").map(_ -> s"${tlBaseVersion.value}.0").toMap,

    apiURL := Some(url("https://typelevel.org/catbird/api/")),
    developers += Developer("travisbrown", "Travis Brown", "", url("https://twitter.com/travisbrown")),

    githubWorkflowBuild := Seq(
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
    ),

    libraryDependencies ++= Seq(
      "org.typelevel" %% "cats-core" % catsVersion,
      "org.scalacheck" %% "scalacheck" % "1.17.0" % Test,
      "org.scalatest" %% "scalatest" % "3.2.14" % Test,
      "org.typelevel" %% "cats-laws" % catsVersion % Test,
      "org.typelevel" %% "discipline-core" % "1.5.1" % Test,
      "org.typelevel" %% "discipline-scalatest" % "2.2.0" % Test
    ),
    resolvers ++= Resolver.sonatypeOssRepos("snapshots"),
    libraryDependencySchemes ++= Seq(
      // scoverage depends on scala-xml 1, but discipline-scalatest transitively pulls in scala-xml 2
      // this is normally discouraged but was recommended by one of the scoverage maintainers in the Typelevel Discord
      "org.scala-lang.modules" %% "scala-xml" % VersionScheme.Always
    ),
    docMappingsApiDir := "api",
    autoAPIMappings := true,

    // disable automatic copyright header creation otherwise added via sbt-typelevel
// TODO    headerMappings := Map.empty
  )

  //  /* When a new Finagle version is released, add it to the beginning of
//   * this list. If `rootFinagle/mimaReportBinaryIssues` shows no issues,
//   * we have a chain of binary-compatible releases and the new list can
//   * be left as it is (i.e. with the new version at the head). If issues
//   * are found, then remove everything other than the new version,
//   * because the newest release is not binary compatible with the older
//   * versions it was checked against.
//   */
//  val versions = Seq("22.12.0")
//
//  lazy val modules = Seq(
//    "com.twitter" %% "finagle-core" % versions.head
//  )
//
//  private lazy val subprojects = modules.map { module =>
//    Project(module.name, file(s".${module.name}"))
//      .enablePlugins(NoPublishPlugin)
//      .settings(
//        libraryDependencies += module,
//        mimaCurrentClassfiles := {
//          (Compile / dependencyClasspath).value.seq.map(_.data).find(_.getName.startsWith(module.name)).get
//        },
//        mimaPreviousArtifacts := versions.tail.map { v =>
//          module.withRevision(v)
//        }.toSet
//      )
//  }
//
//  lazy val rootFinagle =
//    project.in(file(s".rootFinagle")).enablePlugins(NoPublishPlugin).aggregate(subprojects.map(_.project): _*)
//

  override def extraProjects: Seq[Project] = autoImport.allProjects

  override def globalSettings: Seq[Def.Setting[?]] = Seq(
    onChangedBuildSource := ReloadOnSourceChanges,
  )

  private implicit class OrganizationArtifactNameOps(val oan: OrganizationArtifactName) extends AnyVal {
    def %(vav: Version): ModuleID =
      oan % vav.toString
  }
}

object WarnNonUnitStatements extends AutoPlugin {
  override def trigger = allRequirements

  override def projectSettings: Seq[Def.Setting[?]] = Seq(
    scalacOptions ++= {
      if (scalaVersion.value.startsWith("2.13"))
        Seq("-Wnonunit-statement")
      else if (scalaVersion.value.startsWith("2.12"))
        Seq()
      else
        Nil
    },
  )
}
