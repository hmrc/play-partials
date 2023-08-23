val scala2_12 = "2.12.17"
val scala2_13 = "2.13.10"

lazy val commonSettings = Seq(
  majorVersion := 8,
  isPublicArtefact := true,
  crossScalaVersions := Seq(scala2_12, scala2_13),
)

lazy val sharedDependencies = Seq(
    "com.github.ben-manes.caffeine" % "caffeine"  % "2.9.3", // To bump to 3.x required Java 11
    "uk.gov.hmrc"                   %% "crypto"   % "7.3.0"
)

lazy val play28Version      = "2.8.19"
lazy val play28Dependencies = Seq(
  "com.typesafe.play"  %% "play"                % play28Version,
  "com.typesafe.play"  %% "filters-helpers"     % play28Version,
  "com.typesafe.play"  %% "play-guice"          % play28Version,
  "uk.gov.hmrc"        %% "http-verbs-play-28"  % "14.9.0",
  "com.typesafe.play"  %% "play-test"           % play28Version  % Test,
  "com.typesafe.play"  %% "play-specs2"         % play28Version  % Test
)

lazy val testSharedDependencies = Seq(
  "org.scalatest"         %% "scalatest"     % "3.1.2"   % Test,
  "com.vladsch.flexmark"  %  "flexmark-all"  % "0.35.10" % Test,
  "org.mockito"           %% "mockito-scala" % "1.5.11"  % Test
)

def sharedSources = Seq(
  Compile / unmanagedSourceDirectories   += baseDirectory.value / "../shared/src/main/scala",
  Compile / unmanagedResourceDirectories += baseDirectory.value / "../shared/src/main/resources",
  Test    / unmanagedSourceDirectories   += baseDirectory.value / "../shared/src/test/scala",
  Test    / unmanagedResourceDirectories += baseDirectory.value / "../shared/src/test/resources"
)

lazy val play28 = Project("play-partials-play-28", file("play-28"))
  .settings(
    commonSettings :+ (scalaVersion := scala2_12),
    libraryDependencies ++= play28Dependencies ++ sharedDependencies ++ testSharedDependencies,
    sharedSources
  )

lazy val library = (project in file("."))
  .disablePlugins(sbt.plugins.JUnitXmlReportPlugin)
  .settings(
    majorVersion := 8,
    isPublicArtefact := true,
    commonSettings :+ (scalaVersion := scala2_13),
    publish / skip := true,
  )
  .aggregate(
    play28,
  )
