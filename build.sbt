import org.scalajs.sbtplugin.cross.CrossProject

import xerial.sbt.Sonatype.sonatypeSettings
import sbtrelease.ReleaseStateTransformations._
import sys.process.Process

scalafmtOnCompile in ThisBuild := true

lazy val root: Project = Project(id = "scalacache", base = file("."))
  .enablePlugins(ReleasePlugin)
  .settings(
    commonSettings,
    sonatypeSettings,
    publishArtifact := false,
    releaseCrossBuild := true,
    releaseProcess := Seq[ReleaseStep](
      checkSnapshotDependencies,
      inquireVersions,
      runClean,
      runTest,
      setReleaseVersion,
      updateVersionInDocs,
      releaseStepTask(tut in doc),
      commitUpdatedDocFiles,
      commitReleaseVersion,
      tagRelease,
      publishArtifacts,
      setNextVersion,
      commitNextVersion,
      releaseStepCommand("sonatypeReleaseAll"),
      pushChanges
    )
  )
  .aggregate(coreJS,
             coreJVM,
             guava,
             memcached,
             ehcache,
             redis,
             cache2k,
             caffeine,
             ohc,
             catsEffect,
             monix,
             scalaz72,
             circe,
             tests)

lazy val core =
  CrossProject(id = "core", file("modules/core"), CrossType.Full)
    .settings(commonSettings)
    .settings(
      moduleName := "scalacache-core",
      libraryDependencies ++= Seq(
        "org.scala-lang" % "scala-reflect" % scalaVersion.value
      ) ++ scalacheck,
      scala211OnlyDeps(
        "org.squeryl" %% "squeryl" % "0.9.9" % Test,
        "com.h2database" % "h2" % "1.4.196" % Test
      ),
      coverageMinimum := 79,
      coverageFailOnMinimum := true
    )

lazy val coreJVM = core.jvm
lazy val coreJS = core.js.settings(coverageEnabled := false)

def module(name: String) =
  Project(id = name, base = file(s"modules/$name"))
    .settings(commonSettings)
    .settings(moduleName := s"scalacache-$name")
    .dependsOn(coreJVM)

lazy val guava = module("guava")
  .settings(
    libraryDependencies ++= Seq(
      "com.google.guava" % "guava" % "25.0-jre",
      "com.google.code.findbugs" % "jsr305" % "1.3.9"
    )
  )

lazy val memcached = module("memcached")
  .settings(
    libraryDependencies ++= Seq(
      "net.spy" % "spymemcached" % "2.12.3"
    )
  )

lazy val ehcache = module("ehcache")
  .settings(
    libraryDependencies ++= Seq(
      "net.sf.ehcache" % "ehcache" % "2.10.4",
      "javax.transaction" % "jta" % "1.1"
    ),
    coverageMinimum := 80,
    coverageFailOnMinimum := true
  )

lazy val redis = module("redis")
  .settings(
    libraryDependencies ++= Seq(
      "redis.clients" % "jedis" % "2.9.0"
    ),
    coverageMinimum := 56,
    coverageFailOnMinimum := true
  )

lazy val cache2k = module("cache2k")
  .settings(
    libraryDependencies ++= Seq(
      "org.cache2k" % "cache2k-core" % "1.0.2.Final",
      "org.cache2k" % "cache2k-api" % "1.0.2.Final"
    )
  )

lazy val caffeine = module("caffeine")
  .settings(
    libraryDependencies ++= Seq(
      "com.github.ben-manes.caffeine" % "caffeine" % "2.6.2",
      "com.google.code.findbugs" % "jsr305" % "3.0.0" % Provided
    ),
    coverageMinimum := 80,
    coverageFailOnMinimum := true
  )

lazy val ohc = module("ohc")
  .settings(
    libraryDependencies ++= Seq(
      "org.caffinitas.ohc" % "ohc-core-j8" % "0.6.1"
    )
  )

lazy val catsEffect = module("cats-effect")
  .settings(
    libraryDependencies ++= Seq(
      "org.typelevel" %% "cats-effect" % "1.0.0-RC2"
    ),
    coverageMinimum := 50,
    coverageFailOnMinimum := true
  )

lazy val monix = module("monix")
  .settings(
    libraryDependencies ++= Seq(
      "io.monix" %% "monix" % "3.0.0-RC1"
    ),
    coverageMinimum := 80,
    coverageFailOnMinimum := true
  )
  .dependsOn(catsEffect)

lazy val scalaz72 = module("scalaz72")
  .settings(
    libraryDependencies ++= Seq(
      "org.scalaz" %% "scalaz-concurrent" % "7.2.22"
    ),
    coverageMinimum := 40,
    coverageFailOnMinimum := true
  )

lazy val circe = module("circe")
  .settings(
    libraryDependencies ++= Seq(
      "io.circe" %% "circe-core" % "0.9.3",
      "io.circe" %% "circe-parser" % "0.9.3",
      "io.circe" %% "circe-generic" % "0.9.3" % Test
    ) ++ scalacheck,
    coverageMinimum := 80,
    coverageFailOnMinimum := true
  )

lazy val tests = module("tests")
  .settings(publishArtifact := false)
  .dependsOn(cache2k, caffeine, memcached, redis, ohc, guava, ehcache, catsEffect, monix, scalaz72, circe)

lazy val doc = module("doc")
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
    micrositeShareOnSocial := true
  )
  .dependsOn(coreJVM, guava, memcached, ehcache, redis, cache2k, caffeine, ohc, catsEffect, monix, scalaz72, circe)

lazy val benchmarks = module("benchmarks")
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
  .dependsOn(cache2k)
  .dependsOn(caffeine)
  .dependsOn(ohc)

lazy val slf4j = Seq(
  "org.slf4j" % "slf4j-api" % "1.7.25"
)

lazy val scalaTest = Seq(
  "org.scalatest" %% "scalatest" % "3.0.5" % Test
)

lazy val scalacheck = Seq(
  "org.scalacheck" %% "scalacheck" % "1.14.0" % Test
)

// Dependencies common to all projects
lazy val commonDeps = slf4j ++ scalaTest

lazy val commonSettings =
  mavenSettings ++
    scalafmtSettings ++
    Seq(
      organization := "com.github.cb372",
      scalacOptions ++= Seq("-unchecked", "-deprecation", "-feature"),
      resolvers += Resolver.typesafeRepo("releases"),
      releasePublishArtifactsAction := PgpKeys.publishSigned.value,
      libraryDependencies ++= commonDeps,
      parallelExecution in Test := false
    )

lazy val mavenSettings = Seq(
  pomExtra :=
    <url>https://github.com/cb372/scalacache</url>
    <licenses>
      <license>
        <name>Apache License, Version 2.0</name>
        <url>http://www.apache.org/licenses/LICENSE-2.0.html</url>
        <distribution>repo</distribution>
      </license>
    </licenses>
    <developers>
      <developer>
        <id>cb372</id>
        <name>Chris Birchall</name>
        <url>https://github.com/cb372</url>
      </developer>
    </developers>,
  publishTo := {
    val nexus = "https://oss.sonatype.org/"
    if (version.value.trim.endsWith("SNAPSHOT"))
      Some("snapshots" at nexus + "content/repositories/snapshots")
    else
      Some("releases" at nexus + "service/local/staging/deploy/maven2")
  },
  publishMavenStyle := true,
  publishArtifact in Test := false,
  pomIncludeRepository := { _ =>
    false
  }
)

lazy val updateVersionInDocs = ReleaseStep(action = st => {
  val extracted = Project.extract(st)
  val projectVersion = extracted.get(Keys.version)

  println(s"Updating project version to $projectVersion in the docs")

  val find = Process(Seq("find", "modules/doc/src/main/tut", "-name", "*.md"))

  val sed = Process(
    Seq("xargs", "sed", "-i", "", "-E", "-e", s"""s/"scalacache-(.*)" % ".*"/"scalacache-\\1" % "$projectVersion"/g"""))

  (find #| sed).!

  st
})

lazy val commitUpdatedDocFiles = ReleaseStep(action = st => {
  val extracted = Project.extract(st)
  val projectVersion = extracted.get(Keys.version)

  println("Committing docs")

  Process(
    Seq("git", "commit", "modules/doc/src/main/tut/*", "-m", s"Update project version in docs to $projectVersion")).!

  st
})

def scala211OnlyDeps(moduleIDs: ModuleID*) =
  libraryDependencies ++= (scalaBinaryVersion.value match {
    case "2.11" => moduleIDs
    case other  => Nil
  })

lazy val scalafmtSettings = Seq(
  // work around https://github.com/lucidsoftware/neo-sbt-scalafmt/issues/18
  sourceDirectories in scalafmt in Compile := (unmanagedSourceDirectories in Compile).value,
  sourceDirectories in scalafmt in Test := (unmanagedSourceDirectories in Test).value
)
