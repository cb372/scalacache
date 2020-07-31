import sbtcrossproject.CrossProject

inThisBuild(
  List(
    organization := "com.github.cb372",
    homepage := Some(url("https://github.com/cb372/scalacache")),
    licenses := List("Apache-2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0")),
    developers := List(
      Developer(
        "cb372",
        "Chris Birchall",
        "chris.birchall@gmail.com",
        url("https://twitter.com/cbirchall")
      )
    )
  )
)

scalafmtOnCompile in ThisBuild := true

lazy val root: Project = Project(id = "scalacache", base = file("."))
  .settings(
    commonSettings,
    publishArtifact := false
  )
  .aggregate(
    coreJS,
    coreJVM,
    memcached,
    redis,
    caffeine,
    circe,
    tests
  )

lazy val core =
  CrossProject(id = "core", file("modules/core"))(JSPlatform, JVMPlatform)
    .settings(commonSettings)
    .settings(
      moduleName := "scalacache-core",
      libraryDependencies ++= Seq(
        "org.scala-lang" % "scala-reflect" % scalaVersion.value,
        "org.typelevel"  %%% "cats-effect" % "2.1.3",
        "org.scalatest"  %%% "scalatest"   % "3.2.0" % Test,
        "org.scalacheck" %%% "scalacheck"  % "1.14.3" % Test
      ),
      coverageMinimum := 79,
      coverageFailOnMinimum := true
    )
    .jvmSettings(
      libraryDependencies ++= Seq(
        "org.slf4j" % "slf4j-api" % "1.7.30"
      )
    )

lazy val coreJVM = core.jvm
lazy val coreJS  = core.js.settings(coverageEnabled := false)

def jvmOnlyModule(name: String) =
  Project(id = name, base = file(s"modules/$name"))
    .settings(commonSettings)
    .settings(
      moduleName := s"scalacache-$name",
      libraryDependencies += scalatest
    )
    .dependsOn(coreJVM)

lazy val memcached = jvmOnlyModule("memcached")
  .settings(
    libraryDependencies ++= Seq(
      "net.spy" % "spymemcached" % "2.12.3"
    )
  )

lazy val redis = jvmOnlyModule("redis")
  .settings(
    libraryDependencies ++= Seq(
      "redis.clients" % "jedis" % "2.10.2"
    ),
    coverageMinimum := 56,
    coverageFailOnMinimum := true
  )

lazy val caffeine = jvmOnlyModule("caffeine")
  .settings(
    libraryDependencies ++= Seq(
      "com.github.ben-manes.caffeine" % "caffeine" % "2.8.5",
      "com.google.code.findbugs"      % "jsr305"   % "3.0.2" % Provided
    ),
    coverageMinimum := 80,
    coverageFailOnMinimum := true
  )

lazy val circe = jvmOnlyModule("circe")
  .settings(
    libraryDependencies ++= Seq(
      "io.circe" %% "circe-core"    % "0.13.0",
      "io.circe" %% "circe-parser"  % "0.13.0",
      "io.circe" %% "circe-generic" % "0.13.0" % Test,
      scalacheck
    ),
    coverageMinimum := 80,
    coverageFailOnMinimum := true
  )

lazy val tests = jvmOnlyModule("tests")
  .settings(publishArtifact := false)
  .dependsOn(caffeine, memcached, redis, circe)

lazy val docs = jvmOnlyModule("docs")
  .enablePlugins(MicrositesPlugin)
  .settings(
    publishArtifact := false,
    micrositeName := "ScalaCache",
    micrositeAuthor := "Chris Birchall",
    micrositeDescription := "A facade for the most popular cache implementations, with a simple, idiomatic Scala API.",
    micrositeBaseUrl := "/scalacache",
    micrositeDocumentationUrl := "/scalacache/docs",
    micrositeHomepage := "https://github.com/cb372/scalacache",
    micrositeGithubOwner := "cb372",
    micrositeGithubRepo := "scalacache",
    micrositeGitterChannel := true,
    micrositeTwitterCreator := "@cbirchall",
    micrositeShareOnSocial := true,
    mdocIn := (sourceDirectory in Compile).value / "mdoc"
  )
  .dependsOn(
    coreJVM,
    memcached,
    redis,
    caffeine,
    circe
  )

lazy val benchmarks = jvmOnlyModule("benchmarks")
  .enablePlugins(JmhPlugin)
  .settings(
    publishArtifact := false,
    fork in (Compile, run) := true,
    javaOptions in Jmh ++= Seq("-server", "-Xms2G", "-Xmx2G", "-XX:+UseG1GC", "-XX:-UseBiasedLocking"),
    javaOptions in (Test, run) ++= Seq(
      "-XX:+UnlockCommercialFeatures",
      "-XX:+FlightRecorder",
      "-XX:StartFlightRecording=delay=20s,duration=60s,filename=memoize.jfr",
      "-server",
      "-Xms2G",
      "-Xmx2G",
      "-XX:+UseG1GC",
      "-XX:-UseBiasedLocking"
    )
  )
  .dependsOn(caffeine)

lazy val scalatest = "org.scalatest" %% "scalatest" % "3.2.0" % Test

lazy val scalacheck = "org.scalacheck" %% "scalacheck" % "1.14.3" % Test

lazy val commonSettings =
  mavenSettings ++
    Seq(
      organization := "com.github.cb372",
      scalacOptions ++= Seq("-unchecked", "-deprecation", "-feature", "-language:higherKinds"),
      parallelExecution in Test := false
    )

lazy val mavenSettings = Seq(
  publishArtifact in Test := false,
  pomIncludeRepository := { _ =>
    false
  }
)
