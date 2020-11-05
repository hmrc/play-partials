resolvers += Resolver.bintrayIvyRepo("hmrc", "sbt-plugin-releases")
resolvers += Resolver.bintrayRepo("hmrc", "releases")
resolvers += Resolver.typesafeRepo("releases")

addSbtPlugin("uk.gov.hmrc" % "sbt-auto-build"             % "2.9.0")
addSbtPlugin("uk.gov.hmrc" % "sbt-git-versioning"         % "2.1.0")
addSbtPlugin("uk.gov.hmrc" % "sbt-artifactory"            % "1.6.0")
addSbtPlugin("uk.gov.hmrc" % "sbt-play-cross-compilation" % "1.0.0")
