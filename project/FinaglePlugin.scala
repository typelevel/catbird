import sbt._, Keys._

import com.typesafe.tools.mima.plugin.MimaKeys._
import com.typesafe.tools.mima.plugin.SbtMima
import org.typelevel.sbt.NoPublishPlugin

object FinaglePlugin extends AutoPlugin {

  override def trigger = allRequirements

  object autoImport {
    lazy val finagleVersion = versions.head
  }

  val versions = Seq("21.8.0", "21.6.0")

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
