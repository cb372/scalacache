inThisBuild(
  List(
    baseVersion := "1.0",
    organization := "com.github.cb372",
    organizationName := "scalacache",
    homepage     := Some(url("https://github.com/cb372/scalacache")),
    licenses     := List("Apache-2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0")),
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

val CatsEffectVersion = "3.2.9"

scalafmtOnCompile in ThisBuild := true

lazy val root: Project = Project(id = "scalacache", base = file("."))
  .enablePlugins(SonatypeCiReleasePlugin)
  .settings(
    commonSettings,
    publishArtifact := false
  )
  .aggregate(
    core,
    memcached,
    redis,
    caffeine,
    circe,
    tests
  )

lazy val core =
  Project(id = "core", file("modules/core"))
    .settings(commonSettings)
    .settings(
      moduleName := "scalacache-core",
      libraryDependencies ++= Seq(
        "org.slf4j"      % "slf4j-api"   % "1.7.32",
        "org.typelevel" %% "cats-effect" % CatsEffectVersion,
        scalatest,
        scalacheck
      ) ++ (if (scalaVersion.value.startsWith("2.")) {
        Seq(
          "org.scala-lang" % "scala-reflect" % scalaVersion.value,
          "org.scala-lang.modules" %% "scala-collection-compat" % "2.6.0"
          )
      } else Nil),
      coverageMinimum       := 60,
      coverageFailOnMinimum := true
    )

def createModule(name: String) =
  Project(id = name, base = file(s"modules/$name"))
    .settings(commonSettings)
    .settings(
      moduleName := s"scalacache-$name",
      libraryDependencies += scalatest
    )
    .dependsOn(core)

lazy val memcached = createModule("memcached")
  .settings(
    libraryDependencies ++= Seq(
      "net.spy" % "spymemcached" % "2.12.3"
    )
  )

lazy val redis = createModule("redis")
  .settings(
    libraryDependencies ++= Seq(
      "redis.clients" % "jedis" % "3.7.0"
    ),
    coverageMinimum       := 56,
    coverageFailOnMinimum := true
  )

lazy val caffeine = createModule("caffeine")
  .settings(
    libraryDependencies ++= Seq(
      "com.github.ben-manes.caffeine" % "caffeine"            % "3.0.4",
      "org.typelevel"                %% "cats-effect-testkit" % CatsEffectVersion % Test,
      "com.google.code.findbugs"      % "jsr305"              % "3.0.2"           % Provided
    ),
    coverageMinimum       := 80,
    coverageFailOnMinimum := true
  )

lazy val circe = createModule("circe")
  .settings(
    libraryDependencies ++= Seq(
      "io.circe" %% "circe-core"    % "0.14.1",
      "io.circe" %% "circe-parser"  % "0.14.1",
      "io.circe" %% "circe-generic" % "0.14.1" % Test,
      scalacheck,
      scalatestplus
    ),
    coverageMinimum       := 80,
    coverageFailOnMinimum := true
  )

lazy val tests = createModule("tests")
  .settings(publishArtifact := false)
  .dependsOn(caffeine, memcached, redis, circe)

lazy val docs = createModule("docs")
  .enablePlugins(MicrositesPlugin)
  .settings(
    publishArtifact      := false,
    micrositeName        := "ScalaCache",
    micrositeAuthor      := "Chris Birchall",
    micrositeDescription := "A facade for the most popular cache implementations, with a simple, idiomatic Scala API.",
    micrositeBaseUrl     := "/scalacache",
    micrositeDocumentationUrl := "/scalacache/docs",
    micrositeHomepage         := "https://github.com/cb372/scalacache",
    micrositeGithubOwner      := "cb372",
    micrositeGithubRepo       := "scalacache",
    micrositeGitterChannel    := true,
    micrositeTwitterCreator   := "@cbirchall",
    micrositeShareOnSocial    := true,
    mdocIn                    := (sourceDirectory in Compile).value / "mdoc"
  )
  .dependsOn(
    core,
    memcached,
    redis,
    caffeine,
    circe
  )

lazy val benchmarks = createModule("benchmarks")
  .enablePlugins(JmhPlugin)
  .settings(
    githubWorkflowArtifactUpload := false,
    publishArtifact        := false,
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

lazy val scalatest = "org.scalatest" %% "scalatest" % "3.2.10" % Test

lazy val scalacheck = "org.scalacheck" %% "scalacheck" % "1.15.4" % Test

lazy val scalatestplus = "org.scalatestplus" %% "scalacheck-1-15" % "3.2.10.0" % Test

lazy val commonSettings =
  mavenSettings ++
    Seq(
      organization := "com.github.cb372",
      scalacOptions ++= Seq("-language:higherKinds", "-language:postfixOps"),
      parallelExecution in Test := false
    )

lazy val mavenSettings = Seq(
  publishArtifact in Test := false,
  pomIncludeRepository := { _ =>
    false
  }
)

val Scala30  = "3.1.0"
val Scala213 = "2.13.7"
val Scala212 = "2.12.15"
val Jdk11    = "openjdk@1.11.0"

ThisBuild / scalaVersion               := Scala213
ThisBuild / crossScalaVersions         := Seq(Scala213, Scala212, Scala30)
ThisBuild / githubWorkflowJavaVersions := Seq(Jdk11)
ThisBuild / githubWorkflowBuild        := Seq(
  WorkflowStep.Sbt(List("scalafmtCheckAll"), name = Some("Check Formatting")),
  WorkflowStep.Run(List("docker-compose up -d"), name = Some("Setup Dependencies")),
  WorkflowStep.Sbt(List("ci"), name = Some("Run ci task from sbt-spiewak")),
  WorkflowStep.Sbt(List("docs/mdoc"), name = Some("Compile Docs"))
)
ThisBuild / spiewakCiReleaseSnapshots := true
ThisBuild / spiewakMainBranches := Seq("master")
