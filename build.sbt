import sbt.Keys._
import sbt._

val compileDependencies = PlayCrossCompilation.dependencies(
  shared = Seq(
    "com.github.ben-manes.caffeine" % "caffeine"  % "2.8.4",
  ),
  play26 = Seq(
    "com.typesafe.play"  %% "play"                % "2.6.25",
    "com.typesafe.play"  %% "filters-helpers"     % "2.6.25",
    "uk.gov.hmrc"        %% "http-verbs-play-26"  % "13.2.0"
  ),
  play27 = Seq(
    "com.typesafe.play"  %% "play"                % "2.7.9",
    "com.typesafe.play"  %% "filters-helpers"     % "2.7.9",
    "uk.gov.hmrc"        %% "http-verbs-play-27"  % "13.2.0"
  ),
  play28 = Seq(
    "com.typesafe.play"  %% "play"                % "2.8.7",
    "com.typesafe.play"  %% "filters-helpers"     % "2.8.7",
    "uk.gov.hmrc"        %% "http-verbs-play-27"  % "13.2.0"
  )
)

val testDependencies = PlayCrossCompilation.dependencies(
  shared = Seq(
    "org.scalatest"         %% "scalatest"     % "3.1.2"   % Test,
    "com.vladsch.flexmark"  %  "flexmark-all"  % "0.35.10" % Test,
    "org.mockito"           %% "mockito-scala" % "1.5.11"  % Test
  ),
  play26 = Seq(
    "com.typesafe.play" %% "play-test"         % "2.6.25" % Test,
    "com.typesafe.play" %% "play-specs2"       % "2.6.25" % Test
  ),
  play27 = Seq(
    "com.typesafe.play" %% "play-test"         % "2.7.9"  % Test,
    "com.typesafe.play" %% "play-specs2"       % "2.7.9"  % Test
  ),
  play28 = Seq(
    "com.typesafe.play" %% "play-test"         % "2.8.7"  % Test,
    "com.typesafe.play" %% "play-specs2"       % "2.8.7"  % Test
  )
)

lazy val playPartials = Project("play-partials", file("."))
  .enablePlugins(SbtAutoBuildPlugin, SbtGitVersioning)
  .disablePlugins(sbt.plugins.JUnitXmlReportPlugin)
  .settings(
    majorVersion := 8,
    makePublicallyAvailableOnBintray := true,
    scalaVersion := "2.12.13",
    libraryDependencies ++= compileDependencies ++ testDependencies,
  )
  .settings(PlayCrossCompilation.playCrossCompilationSettings)
  .settings(
    // setting fork in Test, as without it the Play27 build fails with this error:
    // https://github.com/scala/scala-parser-combinators/issues/197
    fork in Test := true
  )
