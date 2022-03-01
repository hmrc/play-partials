val compileDependencies = PlayCrossCompilation.dependencies(
  shared = Seq(
    "com.github.ben-manes.caffeine" % "caffeine"  % "2.8.8",
    "uk.gov.hmrc"        %% "crypto"              % "6.1.0"
  ),
  play28 = Seq(
    "com.typesafe.play"  %% "play"                % "2.8.8",
    "com.typesafe.play"  %% "filters-helpers"     % "2.8.8",
    "com.typesafe.play"  %% "play-guice"          % "2.8.8",
    "uk.gov.hmrc"        %% "http-verbs-play-28"  % "13.12.0"
  )
)

val testDependencies = PlayCrossCompilation.dependencies(
  shared = Seq(
    "org.scalatest"         %% "scalatest"     % "3.1.2"   % Test,
    "com.vladsch.flexmark"  %  "flexmark-all"  % "0.35.10" % Test,
    "org.mockito"           %% "mockito-scala" % "1.5.11"  % Test
  ),
  play28 = Seq(
    "com.typesafe.play" %% "play-test"         % "2.8.8"  % Test,
    "com.typesafe.play" %% "play-specs2"       % "2.8.8"  % Test
  )
)

val scala2_12 = "2.12.15"
val scala2_13 = "2.13.7"

lazy val playPartials = Project("play-partials", file("."))
  .disablePlugins(sbt.plugins.JUnitXmlReportPlugin)
  .settings(
    majorVersion := 8,
    isPublicArtefact := true,
    scalaVersion := scala2_12,
    crossScalaVersions := Seq(scala2_12, scala2_13),
    libraryDependencies ++= compileDependencies ++ testDependencies,
  )
  .settings(PlayCrossCompilation.playCrossCompilationSettings)
