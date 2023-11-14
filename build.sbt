
val scala2_12 = "2.12.18"
val scala2_13 = "2.13.12"

ThisBuild / majorVersion     := 9
ThisBuild / isPublicArtefact := true
ThisBuild / scalaVersion     := scala2_13

lazy val library = (project in file("."))
  .settings(publish / skip := true)
  .aggregate(
    playPartialsPlay28,
    playPartialsPlay29,
    playPartialsPlay30
  )

val sharedSources = Seq(
  Compile / unmanagedSourceDirectories   += baseDirectory.value / s"../src-common/main/scala",
  Compile / unmanagedResourceDirectories += baseDirectory.value / s"../src-common/main/resources",
  Test    / unmanagedSourceDirectories   += baseDirectory.value / s"../src-common/test/scala",
  Test    / unmanagedResourceDirectories += baseDirectory.value / s"../src-common/test/resources"
)

lazy val playPartialsPlay28 = Project("play-partials-play-28", file("play-partials-play-28"))
  .disablePlugins(sbt.plugins.JUnitXmlReportPlugin)
  .settings(
    crossScalaVersions := Seq(scala2_12, scala2_13),
    sharedSources,
    libraryDependencies ++= LibDependencies.play28
  )

lazy val playPartialsPlay29 = Project("play-partials-play-29", file("play-partials-play-29"))
  .disablePlugins(sbt.plugins.JUnitXmlReportPlugin)
  .settings(
    crossScalaVersions := Seq(scala2_13),
    sharedSources,
    libraryDependencies ++= LibDependencies.play29
  )

lazy val playPartialsPlay30 = Project("play-partials-play-30", file("play-partials-play-30"))
  .disablePlugins(sbt.plugins.JUnitXmlReportPlugin)
  .settings(
    crossScalaVersions := Seq(scala2_13),
    sharedSources,
    libraryDependencies ++= LibDependencies.play30
  )
