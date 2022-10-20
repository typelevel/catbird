import sbt._, Keys._

import com.typesafe.tools.mima.plugin.MimaKeys._
import com.typesafe.tools.mima.plugin.SbtMima
import org.typelevel.sbt.NoPublishPlugin

object FinaglePlugin extends AutoPlugin {

  override def trigger = allRequirements

  object autoImport {
    lazy val finagleVersion = versions.head
  }

  /* When a new Finagle version is released, add it to the beginning of
   * this list. If `rootFinagle/mimaReportBinaryIssues` shows no issues,
   * we have a chain of binary-compatible releases and the new list can
   * be left as it is (i.e. with the new version at the head). If issues
   * are found, then remove everything other than the new version,
   * because the newest release is not binary compatible with the older
   * versions it was checked against.
   */
  val versions = Seq("22.7.0")

  lazy val modules = Seq(
    "com.twitter" %% "finagle-core" % versions.head
  )

  override lazy val extraProjects = {
    val subprojects = modules.map { module =>
      Project(module.name, file(s".${module.name}"))
        .enablePlugins(NoPublishPlugin)
        .settings(
          libraryDependencies += module,
          mimaCurrentClassfiles := {
            (Compile / dependencyClasspath).value.seq.map(_.data).find(_.getName.startsWith(module.name)).get
          },
          mimaPreviousArtifacts := versions.tail.map { v =>
            module.withRevision(v)
          }.toSet
        )
    }

    val rootFinagle =
      project.in(file(s".rootFinagle")).enablePlugins(NoPublishPlugin).aggregate(subprojects.map(_.project): _*)

    rootFinagle +: subprojects
  }

}
