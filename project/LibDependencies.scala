import sbt._

object LibDependencies {
  val httpVerbsVersion = "15.2.0"

  val play29 = play("play-29")
  val play30 = play("play-30")

  private def play(playSuffix: String) = Seq(
    playOrg(playSuffix)             %% "play-filters-helpers"         % playVersion(playSuffix),
    playOrg(playSuffix)             %% "play-guice"                   % playVersion(playSuffix),
    "uk.gov.hmrc"                   %% s"http-verbs-$playSuffix"      % httpVerbsVersion,
    "com.github.ben-manes.caffeine" %  "caffeine"                     % "3.1.8",
    "uk.gov.hmrc"                   %% "crypto"                       % "8.2.0",
    playOrg(playSuffix)             %% "play-test"                    % playVersion(playSuffix) % Test,
    playOrg(playSuffix)             %% "play-specs2"                  % playVersion(playSuffix) % Test,
    "org.scalatest"                 %% "scalatest"                    % "3.2.18"                % Test,
    "com.vladsch.flexmark"          %  "flexmark-all"                 % "0.64.8"                % Test,
    "org.scalatestplus"             %% "mockito-3-4"                  % "3.2.10.0"              % Test,
    "uk.gov.hmrc"                   %% s"http-verbs-test-$playSuffix" % httpVerbsVersion        % Test
  )

  private def playVersion(playSuffix: String) =
    playSuffix match {
      case "play-29" => "2.9.3"
      case "play-30" => "3.0.3"
    }

  private def playOrg(playSuffix: String) =
    playSuffix match {
      case "play-29" => "com.typesafe.play"
      case "play-30" => "org.playframework"
    }

}
