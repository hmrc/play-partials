
val scala2_13 = "2.13.16"
val scala3    = "3.3.6"

ThisBuild / majorVersion     := 10
ThisBuild / isPublicArtefact := true
ThisBuild / scalaVersion     := scala2_13

// Disable multiple project tests running at the same time
// https://www.scala-sbt.org/1.x/docs/Parallel-Execution.html
Global / concurrentRestrictions += Tags.limitSum(1, Tags.Test, Tags.Untagged)

lazy val library = (project in file("."))
  .settings(publish / skip := true)
  .aggregate(
    playPartialsPlay29,
    playPartialsPlay30
  )

val sharedSources = Seq(
  Compile / unmanagedSourceDirectories   += baseDirectory.value / s"../src-common/main/scala",
  Compile / unmanagedResourceDirectories += baseDirectory.value / s"../src-common/main/resources",
  Test    / unmanagedSourceDirectories   += baseDirectory.value / s"../src-common/test/scala",
  Test    / unmanagedResourceDirectories += baseDirectory.value / s"../src-common/test/resources"
)

lazy val playPartialsPlay29 = Project("play-partials-play-29", file("play-partials-play-29"))
  .disablePlugins(sbt.plugins.JUnitXmlReportPlugin)
  .settings(
    sharedSources,
    libraryDependencies ++= LibDependencies.play29
  )

lazy val playPartialsPlay30 = Project("play-partials-play-30", file("play-partials-play-30"))
  .disablePlugins(sbt.plugins.JUnitXmlReportPlugin)
  .settings(
    crossScalaVersions := Seq(scala2_13, scala3),
    sharedSources,
    libraryDependencies ++= LibDependencies.play30
  )
