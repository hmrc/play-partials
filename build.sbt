import play.core.PlayVersion
import sbt.Keys._
import sbt._

val libName = "play-partials"

val compileDependencies = Seq(
  filters,
  "com.typesafe.play"  %% "play"               % PlayVersion.current,
  "com.google.guava"   %  "guava"              % "19.0",
  "uk.gov.hmrc"        %% "http-verbs"         % "9.1.0-play-25",
   // force dependencies due to security flaws found in jackson-databind < 2.9.x using XRay
   "com.fasterxml.jackson.core"     % "jackson-core"            % "2.9.7",
   "com.fasterxml.jackson.core"     % "jackson-databind"        % "2.9.7",
   "com.fasterxml.jackson.core"     % "jackson-annotations"     % "2.9.7",
   "com.fasterxml.jackson.datatype" % "jackson-datatype-jdk8"   % "2.9.7",
   "com.fasterxml.jackson.datatype" % "jackson-datatype-jsr310" % "2.9.7",
   // force dependencies due to security flaws found in xercesImpl 2.11.0
   "xerces" % "xercesImpl" % "2.12.0"

)

val testDependencies = Seq(
  "com.typesafe.play" %% "play-test"   % PlayVersion.current % "test",
  "com.typesafe.play" %% "play-specs2" % PlayVersion.current % "test",
  "org.scalatest"     %% "scalatest"   % "2.2.4"             % "test",
  "org.pegdown"       %  "pegdown"     % "1.4.2"             % "test",
  "org.mockito"       %  "mockito-all" % "1.9.5"             % "test"
)

lazy val playPartials = Project(libName, file("."))
  .enablePlugins(SbtAutoBuildPlugin, SbtGitVersioning, SbtArtifactory)
  .settings(
    majorVersion := 6,
    makePublicallyAvailableOnBintray := true,
    scalaVersion := "2.11.7",
    libraryDependencies ++= compileDependencies ++ testDependencies,
    crossScalaVersions := Seq("2.11.7"),
    resolvers := Seq(
      Resolver.bintrayRepo("hmrc", "releases"),
      "typesafe-releases" at "http://repo.typesafe.com/typesafe/releases/"
    )
  )
  .disablePlugins(sbt.plugins.JUnitXmlReportPlugin)
