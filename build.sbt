import ReleaseTransformations._
import ScoverageSbtPlugin.ScoverageKeys.coverageExcludedPackages

val bijectionVersion = "0.8.1"
val catsVersion = "0.1.3-SNAPSHOT"
val utilVersion = "6.27.0"
val finagleVersion = "6.28.0"

lazy val buildSettings = Seq(
  organization := "io.catbird",
  version := "0.1.0-SNAPSHOT",
  scalaVersion := "2.11.7",
  crossScalaVersions := Seq("2.10.5", "2.11.7")
)

lazy val compilerOptions = Seq(
  "-deprecation",
  "-encoding", "UTF-8",
  "-feature",
  "-language:existentials",
  "-language:higherKinds",
  "-unchecked",
  "-Yno-adapted-args",
  "-Ywarn-dead-code",
  "-Ywarn-numeric-widen",
  "-Xfuture"
)

lazy val baseSettings = Seq(
  scalacOptions ++= compilerOptions ++ (
    CrossVersion.partialVersion(scalaVersion.value) match {
      case Some((2, 11)) => Seq("-Ywarn-unused-import")
      case _ => Nil
    }
  ),
  scalacOptions in (Compile, console) := compilerOptions,
  libraryDependencies ++= Seq(
    "org.spire-math" %% "cats-core" % catsVersion,
    compilerPlugin("org.spire-math" %% "kind-projector" % "0.6.3")
  ),
  resolvers += Resolver.sonatypeRepo("snapshots"),
  wartremoverWarnings in (Compile, compile) ++= Warts.allBut(
    Wart.NoNeedForMonad
  ),
  ScoverageSbtPlugin.ScoverageKeys.coverageHighlighting := (
    CrossVersion.partialVersion(scalaVersion.value) match {
      case Some((2, 10)) => false
      case _ => true
    }
  )
)

lazy val allSettings = buildSettings ++ baseSettings ++ unidocSettings

lazy val root = project.in(file("."))
  .settings(allSettings ++ noPublishSettings)
  .settings(unidocSettings ++ site.settings ++ ghpages.settings)
  .settings(
    site.addMappingsToSiteDir(mappings in (ScalaUnidoc, packageDoc), "api"),
    git.remoteRepo := "git@github.com:travisbrown/catbird.git"
  )
  .settings(scalacOptions in (Compile, console) := compilerOptions)
  .aggregate(util, finagle, laws)
  .dependsOn(util, finagle)

lazy val test = project
  .settings(buildSettings ++ baseSettings)
  .settings(
    libraryDependencies ++= Seq(
      "com.twitter" %% "bijection-core" % bijectionVersion,
      "com.twitter" %% "finagle-core" % finagleVersion,
      "com.twitter" %% "util-core" % utilVersion,
      "org.scalacheck" %% "scalacheck" % "1.12.5-SNAPSHOT",
      "org.scalatest" %% "scalatest" % "3.0.0-M7",
      "org.spire-math" %% "cats-laws" % catsVersion,
      "org.typelevel" %% "discipline" % "0.4"
    ),
    coverageExcludedPackages := "io\\.catbird\\.test\\..*"
  )
  .dependsOn(util)

lazy val laws = project
  .settings(buildSettings ++ baseSettings)
  .dependsOn(util, finagle, test % "test")

lazy val util = project
  .settings(buildSettings ++ baseSettings)
  .settings(
    libraryDependencies ++= Seq(
      "com.twitter" %% "bijection-core" % bijectionVersion,
      "com.twitter" %% "util-core" % utilVersion
    )
  )

lazy val finagle = project
  .settings(allSettings)
  .settings(
    libraryDependencies ++= Seq(
      "com.twitter" %% "finagle-core" % finagleVersion
    )
  )
  .dependsOn(util)

lazy val publishSettings = Seq(
  releaseCrossBuild := true,
  releasePublishArtifactsAction := PgpKeys.publishSigned.value,
  homepage := Some(url("https://github.com/travisbrown/catbird")),
  licenses := Seq("Apache 2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0")),
  publishMavenStyle := true,
  publishArtifact in Test := false,
  pomIncludeRepository := { _ => false },
  publishTo := {
    val nexus = "https://oss.sonatype.org/"
    if (isSnapshot.value)
      Some("snapshots" at nexus + "content/repositories/snapshots")
    else
      Some("releases"  at nexus + "service/local/staging/deploy/maven2")
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
  publish := (),
  publishLocal := (),
  publishArtifact := false
)

lazy val sharedReleaseProcess = Seq(
  releaseProcess := Seq[ReleaseStep](
    checkSnapshotDependencies,
    inquireVersions,
    runClean,
    runTest,
    setReleaseVersion,
    commitReleaseVersion,
    tagRelease,
    publishArtifacts,
    setNextVersion,
    commitNextVersion,
    ReleaseStep(action = Command.process("sonatypeReleaseAll", _)),
    pushChanges
  )
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
