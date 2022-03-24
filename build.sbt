import laika.rewrite.link.ApiLinks
import laika.rewrite.link.LinkConfig
import sbtcrossproject.CrossProject

val Scala30  = "3.1.1"
val Scala213 = "2.13.8"
val Scala212 = "2.12.15"

val CatsEffectVersion = "3.3.12"
val ScalaTestVersion  = "3.2.12"
val Slf4jVersion      = "1.7.26"

lazy val scalatest     = "org.scalatest"     %% "scalatest"       % ScalaTestVersion         % Test
lazy val scalacheck    = "org.scalacheck"    %% "scalacheck"      % "1.15.4"                 % Test
lazy val scalatestplus = "org.scalatestplus" %% "scalacheck-1-15" % s"${ScalaTestVersion}.0" % Test

inThisBuild(
  List(
    tlBaseVersion    := "1.0",
    organization     := "com.github.cb372",
    organizationName := "scalacache",
    homepage         := Some(url("https://github.com/cb372/scalacache")),
    licenses         := List(License.Apache2),
    developers := List(
      Developer(
        "cb372",
        "Chris Birchall",
        "chris.birchall@gmail.com",
        url("https://twitter.com/cbirchall")
      )
    ),
    scalaVersion            := Scala213,
    crossScalaVersions      := Seq(Scala213, Scala212, Scala30),
    tlSonatypeUseLegacyHost := true,
    tlSitePublishBranch     := Some("master"),
    githubWorkflowJavaVersions := Seq(
      JavaSpec.temurin("11"),
      JavaSpec.temurin("17")
    ),
    githubWorkflowBuild := {
      WorkflowStep.Run(
        List("docker-compose up -d"),
        name = Some("Setup Dependencies")
      ) +: githubWorkflowBuild.value,
    },
    scalafmtOnCompile := true
  )
)

lazy val scalacache = tlCrossRootProject.aggregate(
  core,
  memcached,
  redis,
  caffeine,
  circe,
  tests,
  unidoc
)

def createModule(name: String) =
  Project(id = name, base = file(s"modules/$name"))
    .settings(
      moduleName               := s"scalacache-$name",
      Test / parallelExecution := false,
      libraryDependencies += scalatest,
      apiURL := {
        if (githubIsWorkflowBuild.value)
          apiURL.value
        else
          Some((crossTarget.value / "api").toURI.toURL)
      }
    )

def createSubModule(name: String) =
  createModule(name).dependsOn(core)

lazy val core = createModule("core")
  .settings(
    libraryDependencies ++= Seq(
      "org.slf4j"      % "slf4j-api"   % Slf4jVersion,
      "org.typelevel" %% "cats-effect" % CatsEffectVersion,
      scalacheck
    ) ++ (if (scalaVersion.value.startsWith("2.")) {
            Seq(
              "org.scala-lang"          % "scala-reflect"           % scalaVersion.value,
              "org.scala-lang.modules" %% "scala-collection-compat" % "2.6.0"
            )
          } else Nil)
  )

lazy val memcached = createSubModule("memcached")
  .settings(
    libraryDependencies ++= Seq(
      "net.spy" % "spymemcached" % "2.12.3"
    )
  )

lazy val redis = createSubModule("redis")
  .settings(
    libraryDependencies ++= Seq(
      "redis.clients" % "jedis" % "3.7.1"
    )
  )

lazy val caffeine = createSubModule("caffeine")
  .settings(
    libraryDependencies ++= Seq(
      "com.github.ben-manes.caffeine" % "caffeine"            % "3.0.6",
      "org.typelevel"                %% "cats-effect-testkit" % CatsEffectVersion % Test,
      "com.google.code.findbugs"      % "jsr305"              % "3.0.2"           % Provided
    )
  )

lazy val circe = createSubModule("circe")
  .settings(
    libraryDependencies ++= Seq(
      "io.circe" %% "circe-core"    % "0.14.1",
      "io.circe" %% "circe-parser"  % "0.14.1",
      "io.circe" %% "circe-generic" % "0.14.1" % Test,
      scalacheck,
      scalatestplus
    )
  )

lazy val tests = createSubModule("tests")
  .enablePlugins(NoPublishPlugin)
  .dependsOn(caffeine, memcached, redis, circe)

lazy val unidoc = createSubModule("unidoc")
  .enablePlugins(TypelevelUnidocPlugin)
  .dependsOn(caffeine, memcached, redis, circe)

lazy val docs = createSubModule("docs")
  .enablePlugins(TypelevelSitePlugin)
  .settings(
    tlSiteApiModule := Some((unidoc / projectID).value),
    tlSiteGenerate := {
      WorkflowStep.Run(
        List("docker-compose up -d"),
        name = Some("Setup Dependencies")
      ) +: tlSiteGenerate.value,
    },
    laikaConfig := {
      laikaConfig.value
        .withConfigValue(
          LinkConfig(
            apiLinks = tlSiteApiUrl.value.map { url =>
              ApiLinks(baseUri = url.toString)
            }.toList
          )
        )
    }
  )
  .dependsOn(
    core,
    memcached,
    redis,
    caffeine,
    circe
  )

lazy val benchmarks = createSubModule("benchmarks")
  .enablePlugins(JmhPlugin)
  .enablePlugins(NoPublishPlugin)
  .settings(
    githubWorkflowArtifactUpload := false,
    Compile / run / fork         := true,
    Jmh / javaOptions ++= Seq("-server", "-Xms2G", "-Xmx2G", "-XX:+UseG1GC", "-XX:-UseBiasedLocking"),
    Test / run / javaOptions ++= Seq(
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
