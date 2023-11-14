import sbt._

object LibDependencies {
  val play28 = play("play-28")
  val play29 = play("play-29")
  val play30 = play("play-30")

  private def play(playSuffix: String) = Seq(
    playOrg(playSuffix)             %% playFilters(playSuffix)   % playVersion(playSuffix),
    playOrg(playSuffix)             %% "play-guice"              % playVersion(playSuffix),
    "uk.gov.hmrc"                   %% s"http-verbs-$playSuffix" % "14.12.0",
    "com.github.ben-manes.caffeine" %  "caffeine"                % "3.1.8",
    "uk.gov.hmrc"                   %% "crypto"                  % "7.4.0",
    playOrg(playSuffix)             %% "play-test"               % playVersion(playSuffix) % Test,
    playOrg(playSuffix)             %% "play-specs2"             % playVersion(playSuffix) % Test,
    "org.scalatest"                 %% "scalatest"               % "3.2.17"                % Test,
    "com.vladsch.flexmark"          %  "flexmark-all"            % "0.62.2"                % Test,
    "org.mockito"                   %% "mockito-scala-scalatest" % "1.17.14"               % Test
  ) ++
    (if (playSuffix == "play-30")
       Seq("org.scala-lang.modules" %% "scala-java8-compat" % "1.0.2") // can be dropped with scala 2.12 (play-28): https://github.com/scala/scala-java8-compat
     else
       Seq.empty // correct version provided transitively
    )

  private def playVersion(playSuffix: String) =
    playSuffix match {
      case "play-28" => "2.8.20"
      case "play-29" => "2.9.0"
      case "play-30" => "3.0.0"
    }

  private def playOrg(playSuffix: String) =
    playSuffix match {
      case "play-28"
         | "play-29" => "com.typesafe.play"
      case "play-30" => "org.playframework"
    }

  private def playFilters(playSuffix: String) =
    playSuffix match {
      case "play-28" => "filters-helpers"
      case "play-29"
         | "play-30" => "play-filters-helpers"
    }
}
