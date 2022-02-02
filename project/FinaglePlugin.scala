import sbt._, Keys._

import com.typesafe.tools.mima.plugin.MimaKeys._
import com.typesafe.tools.mima.plugin.SbtMima
import org.typelevel.sbt.NoPublishPlugin

object FinaglePlugin extends AutoPlugin {

  val versions = Seq("21.8.0", "21.6.0")

  override def trigger = allRequirements

  lazy val modules = Seq(
    "com.twitter" %% "finagle-core" % versions.head
  )

  override lazy val extraProjects = modules.map { module =>
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

}
