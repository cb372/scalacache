resolvers ++= Seq(
  Classpaths.sbtPluginReleases
)


addSbtPlugin("org.scoverage" %% "sbt-scoverage" % "0.99.7.1")

// TODO can't enable coveralls until sbt-scoverage aggregation feature is complete
// https://github.com/scoverage/sbt-scoverage/issues/13
//addSbtPlugin("org.scoverage" %% "sbt-coveralls" % "0.0.5")


