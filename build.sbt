import sbt.Keys._
import sbt._

val libName = "play-partials"

val compileDependencies = PlayCrossCompilation.dependencies(
  shared = Seq(
    "com.google.guava"   %  "guava"              % "19.0"
  ),
  play25 = Seq(
    "com.typesafe.play"  %% "play"               % "2.5.19",
    "com.typesafe.play"  %% "filters-helpers"    % "2.5.19",
    "uk.gov.hmrc"        %% "http-verbs"         % "10.7.0-play-25",
     // force dependencies due to security flaws found in jackson-databind < 2.9.x using XRay
     "com.fasterxml.jackson.core"     % "jackson-core"            % "2.9.7",
     "com.fasterxml.jackson.core"     % "jackson-databind"        % "2.9.7",
     "com.fasterxml.jackson.core"     % "jackson-annotations"     % "2.9.7",
     "com.fasterxml.jackson.datatype" % "jackson-datatype-jdk8"   % "2.9.7",
     "com.fasterxml.jackson.datatype" % "jackson-datatype-jsr310" % "2.9.7",
     // force dependencies due to security flaws found in xercesImpl 2.11.0
     "xerces" % "xercesImpl" % "2.12.0"
  ),
  play26 = Seq(
    "com.typesafe.play"  %% "play"               % "2.6.20",
    "com.typesafe.play"  %% "filters-helpers"    % "2.6.20",
    "uk.gov.hmrc"        %% "http-verbs"         % "10.7.0-play-26"
  ),
  play27 = Seq(
    "com.typesafe.play"  %% "play"               % "2.7.4",
    "com.typesafe.play"  %% "filters-helpers"    % "2.7.4",
    "uk.gov.hmrc"        %% "http-verbs"         % "10.7.0-play-27"
  )
)

val testDependencies = PlayCrossCompilation.dependencies(
  shared = Seq(
    "org.scalatest"     %% "scalatest"     % "3.0.2"             % "test",
    "org.pegdown"       %  "pegdown"       % "1.4.2"             % "test",
    "org.mockito"       %% "mockito-scala" % "1.2.0"             % "test"
  ),
  play25 = Seq(
    "com.typesafe.play" %% "play-test"   % "2.5.19"            % "test",
    "com.typesafe.play" %% "play-specs2" % "2.5.19"            % "test"
  ),
  play26 = Seq(
    "com.typesafe.play" %% "play-test"   % "2.6.20"            % "test",
    "com.typesafe.play" %% "play-specs2" % "2.6.20"            % "test"
  ),
  play27 = Seq(
    "com.typesafe.play" %% "play-test"   % "2.7.4"            % "test",
    "com.typesafe.play" %% "play-specs2" % "2.7.4"            % "test"
  )
)

lazy val playPartials = Project(libName, file("."))
  .enablePlugins(SbtAutoBuildPlugin, SbtGitVersioning, SbtArtifactory)
  .settings(
    majorVersion := 6,
    makePublicallyAvailableOnBintray := true,
    scalaVersion := "2.11.12",
    libraryDependencies ++= compileDependencies ++ testDependencies,
    crossScalaVersions := Seq("2.11.12", "2.12.8"),
    resolvers := Seq(
      Resolver.bintrayRepo("hmrc", "releases"),
      "typesafe-releases" at "http://repo.typesafe.com/typesafe/releases/"
    )
  ).disablePlugins(sbt.plugins.JUnitXmlReportPlugin)
  .settings(PlayCrossCompilation.playCrossCompilationSettings)
  .settings(
    // setting fork in Test, as without it the Play27 build fails with this error:
    // https://github.com/scala/scala-parser-combinators/issues/197
    fork in Test := true
  )

