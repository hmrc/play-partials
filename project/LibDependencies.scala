import sbt._

object LibDependencies {
  private val play28Version = "2.8.20"
  private val play29Version = "2.9.0"


  val common = Seq(
    "com.github.ben-manes.caffeine" %  "caffeine"                % "3.1.8",
    "uk.gov.hmrc"                   %% "crypto"                  % "7.4.0",
    "org.scalatest"                 %% "scalatest"               % "3.2.17"  % Test,
    "com.vladsch.flexmark"          %  "flexmark-all"            % "0.62.2"  % Test,
    "org.mockito"                   %% "mockito-scala-scalatest" % "1.17.14" % Test
  )

  val play28 = Seq(
    "com.typesafe.play" %% "play"                % play28Version,
    "com.typesafe.play" %% "filters-helpers"     % play28Version,
    "com.typesafe.play" %% "play-guice"          % play28Version,
    "uk.gov.hmrc"       %% "http-verbs-play-28"  % "14.11.0",
    "com.typesafe.play" %% "play-test"           % play28Version  % Test,
    "com.typesafe.play" %% "play-specs2"         % play28Version  % Test
  )

  val play29 = Seq(
    "com.typesafe.play" %% "play"                 % play29Version,
    "com.typesafe.play" %% "play-filters-helpers" % play29Version,
    "com.typesafe.play" %% "play-guice"           % play29Version,
    "uk.gov.hmrc"       %% "http-verbs-play-29"   % "14.11.0",
    "com.typesafe.play" %% "play-test"            % play29Version % Test,
    "com.typesafe.play" %% "play-specs2"          % play29Version % Test
  )
}
