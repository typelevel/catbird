import ReleaseTransformations._

val bijectionVersion = "0.9.0"
val catsVersion = "0.4.1"
val utilVersion = "6.32.0"
val finagleVersion = "6.33.0"

lazy val buildSettings = Seq(
  organization := "io.catbird",
  scalaVersion := "2.11.7",
  crossScalaVersions := Seq("2.10.6", "2.11.7")
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
    "org.typelevel" %% "cats-core" % catsVersion,
    compilerPlugin("org.spire-math" %% "kind-projector" % "0.7.1")
  ),
  resolvers += Resolver.sonatypeRepo("snapshots"),
  wartremoverWarnings in (Compile, compile) ++= Warts.allBut(
    Wart.NoNeedForMonad
  ),
  coverageHighlighting := (
    CrossVersion.partialVersion(scalaVersion.value) match {
      case Some((2, 10)) => false
      case _ => true
    }
  )
)

lazy val allSettings = buildSettings ++ baseSettings ++ publishSettings

lazy val root = project.in(file("."))
  .settings(allSettings ++ noPublishSettings)
  .settings(unidocSettings ++ site.settings ++ ghpages.settings)
  .settings(
    site.addMappingsToSiteDir(mappings in (ScalaUnidoc, packageDoc), "api"),
    git.remoteRepo := "git@github.com:travisbrown/catbird.git"
  )
  .settings(scalacOptions in (Compile, console) := compilerOptions)
  .aggregate(util, finagle, tests)
  .dependsOn(util, finagle)

lazy val tests = project
  .settings(allSettings ++ noPublishSettings)
  .settings(
    libraryDependencies ++= Seq(
      "com.twitter" %% "bijection-core" % bijectionVersion,
      "com.twitter" %% "finagle-core" % finagleVersion,
      "com.twitter" %% "util-core" % utilVersion,
      "org.scalacheck" %% "scalacheck" % "1.12.5",
      "org.scalatest" %% "scalatest" % "3.0.0-M9",
      "org.typelevel" %% "cats-laws" % catsVersion,
      "org.typelevel" %% "discipline" % "0.4"
    ),
    coverageExcludedPackages := "io\\.catbird\\.tests\\..*"
  )
  .dependsOn(util, finagle)

lazy val util = project
  .settings(moduleName := "catbird-util")
  .settings(allSettings)
  .settings(
    libraryDependencies ++= Seq(
      "com.twitter" %% "bijection-core" % bijectionVersion,
      "com.twitter" %% "util-core" % utilVersion
    )
  )

lazy val finagle = project
  .settings(moduleName := "catbird-finagle")
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
