/*
 * Copyright 2015 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import sbt.Keys._
import sbt._

object HmrcBuild extends Build {

  import uk.gov.hmrc.DefaultBuildSettings._
  import uk.gov.hmrc.PublishingSettings._
  import uk.gov.hmrc.{SbtBuildInfo, ShellPrompt}
  import de.heikoseeberger.sbtheader.AutomateHeaderPlugin

  val appName = "play-partials"
  val appVersion = "1.3.0-SNAPSHOT"

  lazy val microservice = Project(appName, file("."))
    .enablePlugins(AutomateHeaderPlugin)
    .settings(version := appVersion)
    .settings(scalaSettings : _*)
    .settings(defaultSettings() : _*)
    .settings(
      targetJvm := "jvm-1.7",
      shellPrompt := ShellPrompt(appVersion),
      libraryDependencies ++= AppDependencies(),
      crossScalaVersions := Seq("2.11.6"),
      resolvers := Seq(
        Opts.resolver.sonatypeReleases,
        Opts.resolver.sonatypeSnapshots,
        "typesafe-releases" at "http://repo.typesafe.com/typesafe/releases/",
        "typesafe-snapshots" at "http://repo.typesafe.com/typesafe/snapshots/"
      )
    )
    .settings(publishAllArtefacts: _*)
    .settings(SbtBuildInfo(): _*)
    .settings(POMMetadata(): _*)
    .settings(HeaderSettings())
    .disablePlugins(sbt.plugins.JUnitXmlReportPlugin)
    .settings(resolvers += Resolver.bintrayRepo("hmrc", "releases"))
}

private object AppDependencies {

  import play.core.PlayVersion

  val compile = Seq(
    "com.typesafe.play" %% "play" % PlayVersion.current,
    "com.google.guava" % "guava" % "16.0.1",

    "uk.gov.hmrc" %% "http-verbs" % "1.4.1"
  )

  trait TestDependencies {
    lazy val scope: String = "test"
    lazy val test: Seq[ModuleID] = ???
  }

  object Test {
    def apply() = new TestDependencies {
      override lazy val test = Seq(
        "org.scalatest" %% "scalatest" % "2.2.4" % scope,
        "org.pegdown" % "pegdown" % "1.4.2" % scope,
        "org.mockito" % "mockito-all" % "1.9.5" % scope
      )
    }.test
  }

  def apply() = compile ++ Test()
}


object POMMetadata {

  def apply() = {
    pomExtra :=
      <url>https://www.gov.uk/government/organisations/hm-revenue-customs</url>
        <licenses>
          <license>
            <name>Apache 2</name>
            <url>http://www.apache.org/licenses/LICENSE-2.0.txt</url>
          </license>
        </licenses>
        <scm>
          <connection>scm:git@github.com:hmrc/play-partials.git</connection>
          <developerConnection>scm:git@github.com:hmrc/play-partials.git</developerConnection>
          <url>git@github.com:hmrc/play-partials.git</url>
        </scm>
        <developers>
          <developer>
            <id>xnejp03</id>
            <name>Petr Nejedly</name>
            <url>http://www.equalexperts.com</url>
          </developer>
        </developers>
  }
}


object HeaderSettings {
  import de.heikoseeberger.sbtheader.HeaderPlugin.autoImport._
  import de.heikoseeberger.sbtheader.license.Apache2_0

  def apply() = headers := Map("scala" -> Apache2_0("2015", "HM Revenue & Customs"))
}
