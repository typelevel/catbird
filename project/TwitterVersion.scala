import sbt.*
import sbt.Keys.*
import sbt.internal.ProjectMatrix

case class TwitterVersion(version: Version) extends VirtualAxis.WeakAxis {
  override def directorySuffix: String =
    s"-twitter-$version"

  override def idSuffix: String =
    s"-twitter-$version".replace('.', '_')
}

object TwitterVersion {
  def resolve[T](matrix: ProjectMatrix,
                 key: TaskKey[T],
                ): Def.Initialize[Task[T]] =
    Def.taskDyn {
      val project = matrix.finder().apply(scalaVersion.value)
      Def.task((project / key).value)
    }

  def resolve[T](matrix: ProjectMatrix,
                 key: SettingKey[T]
                ): Def.Initialize[T] =
    Def.settingDyn {
      val project = matrix.finder().apply(scalaVersion.value)
      Def.setting((project / key).value)
    }
}
