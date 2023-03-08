val compileDependencies = PlayCrossCompilation.dependencies(
  shared = Seq(
    "com.github.ben-manes.caffeine" % "caffeine"  % "2.9.3", // To bump to 3.x required Java 11
    "uk.gov.hmrc"        %% "crypto"              % "7.3.0"
  ),
  play28 = Seq(
    "com.typesafe.play"  %% "play"                % "2.8.19",
    "com.typesafe.play"  %% "filters-helpers"     % "2.8.19",
    "com.typesafe.play"  %% "play-guice"          % "2.8.19",
    "uk.gov.hmrc"        %% "http-verbs-play-28"  % "14.9.0"
  )
)

val testDependencies = PlayCrossCompilation.dependencies(
  shared = Seq(
    "org.scalatest"         %% "scalatest"     % "3.1.2"   % Test,
    "com.vladsch.flexmark"  %  "flexmark-all"  % "0.35.10" % Test,
    "org.mockito"           %% "mockito-scala" % "1.5.11"  % Test
  ),
  play28 = Seq(
    "com.typesafe.play" %% "play-test"         % "2.8.19"  % Test,
    "com.typesafe.play" %% "play-specs2"       % "2.8.19"  % Test
  )
)

val scala2_12 = "2.12.17"
val scala2_13 = "2.13.10"

lazy val playPartials = Project("play-partials", file("."))
  .disablePlugins(sbt.plugins.JUnitXmlReportPlugin)
  .settings(
    majorVersion := 8,
    isPublicArtefact := true,
    scalaVersion := scala2_13,
    crossScalaVersions := Seq(scala2_12, scala2_13),
    libraryDependencies ++= compileDependencies ++ testDependencies,
  )
  .settings(PlayCrossCompilation.playCrossCompilationSettings)
