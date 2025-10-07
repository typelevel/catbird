object BaseVersion {
  private val versionRegex = """^(\d+)\.(\d+).*$""".r

  def apply(s: String): String =
    s match {
      case versionRegex(year, month) => s"$year.$month"
      case _                         =>
        throw new IllegalArgumentException(s"base version must start with {year}.{month}, but got $s")
    }
}
