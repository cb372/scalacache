import sbt._
import Keys._
import com.typesafe.sbt.SbtScalariform._
import scalariform.formatter.preferences._
import xerial.sbt.Sonatype._
import SonatypeKeys._
import net.virtualvoid.sbt.graph.Plugin._
import org.scoverage.coveralls.CoverallsPlugin
import org.scoverage.coveralls.Imports.CoverallsKeys._
import com.typesafe.sbt.pgp.PgpKeys
import sbtrelease.ReleasePlugin
import sbtrelease.ReleasePlugin.autoImport._
import sbtrelease.ReleaseStateTransformations._

import scala.language.postfixOps

object ScalaCacheBuild extends Build {
  
  object Versions {
    val scala = "2.11.7"
  }

  lazy val root = Project(id = "scalacache",base = file("."))
    .enablePlugins(ReleasePlugin)
    .settings(commonSettings: _*)
    .settings(sonatypeSettings: _*)
    .settings(publishArtifact := false)
    .settings(coverallsTokenFile := Some("coveralls-token.txt"))
    .aggregate(core, guava, memcached, ehcache, redis, lrumap, caffeine)

  lazy val core = Project(id = "scalacache-core", base = file("core"))
    .settings(commonSettings: _*)
    .settings(
      libraryDependencies <+= scalaVersion { s =>
        "org.scala-lang" % "scala-reflect" % s
      }
    )
    .settings(
      libraryDependencies ++= Seq(
        "org.squeryl" %% "squeryl" % "0.9.5-7" % "test",
        "com.h2database" % "h2" % "1.4.182" % "test"
      )
    )
    .disablePlugins(CoverallsPlugin)

  lazy val guava = Project(id = "scalacache-guava", base = file("guava"))
    .settings(implProjectSettings: _*)
    .settings(
      libraryDependencies ++= Seq(
        "com.google.guava" % "guava" % "18.0",
        "com.google.code.findbugs" % "jsr305" % "1.3.9"
      )
    )
    .dependsOn(core)
    .disablePlugins(CoverallsPlugin)

  lazy val memcached = Project(id = "scalacache-memcached", base = file("memcached"))
    .settings(implProjectSettings: _*)
    .settings(
      libraryDependencies ++= Seq(
        "net.spy" % "spymemcached" % "2.11.7"
      )
    )
    .dependsOn(core)
    .disablePlugins(CoverallsPlugin)

  lazy val ehcache = Project(id = "scalacache-ehcache", base = file("ehcache"))
    .settings(implProjectSettings: _*)
    .settings(
      libraryDependencies ++= Seq(
        "net.sf.ehcache" % "ehcache" % "2.10.0",
        "javax.transaction" % "jta" % "1.1"
      )
    )
    .dependsOn(core)
    .disablePlugins(CoverallsPlugin)

  lazy val redis = Project(id = "scalacache-redis", base = file("redis"))
    .settings(implProjectSettings: _*)
    .settings(
      libraryDependencies ++= Seq(
        "redis.clients" % "jedis" % "2.7.2"
      ) ++ playTesting
    )
    .dependsOn(core)
    .disablePlugins(CoverallsPlugin)

  lazy val lrumap = Project(id = "scalacache-lrumap", base = file("lrumap"))
    .settings(implProjectSettings: _*)
    .settings(
      libraryDependencies ++= Seq(
        "com.twitter" % "util-collection_2.11" % "6.23.0"
      )
    )
    .dependsOn(core)
    .disablePlugins(CoverallsPlugin)

  lazy val caffeine = Project(id = "scalacache-caffeine", base = file("caffeine"))
    .settings(implProjectSettings: _*)
    .settings(
      libraryDependencies ++= Seq(
        "com.github.ben-manes.caffeine" % "caffeine" % "1.3.3",
        "com.google.code.findbugs" % "jsr305" % "3.0.0" % "provided"
      )
    )
    .dependsOn(core)
    .disablePlugins(CoverallsPlugin)

  lazy val jodaTime = Seq(
    "joda-time" % "joda-time" % "2.5",
    "org.joda" % "joda-convert" % "1.7"
  )

  lazy val scalaLogging = Seq(
    "com.typesafe.scala-logging" %% "scala-logging" % "3.1.0"
  )

  lazy val scalaTest = Seq(
    "org.scalatest" %% "scalatest" % "2.2.2" % "test"
  ) ++ (if (Versions.scala.startsWith("2.11")) {
    // used in the scalatest reporter
    Seq("org.scala-lang.modules" %% "scala-xml" % "1.0.1" % "test")
  } else Nil)

  val playVersion = "2.3.8"
  lazy val playTesting = Seq(
    "com.typesafe.play" %% "play-test" % playVersion % Test,
    "org.scalatestplus" %% "play" % "1.2.0" % Test
  )

  // Dependencies common to all projects
  lazy val commonDeps =
    scalaLogging ++
    scalaTest ++
    jodaTime

  lazy val commonSettings = 
    Defaults.coreDefaultSettings ++ 
    mavenSettings ++ 
    scalariformSettings ++
    formatterPrefs ++
    graphSettings ++
    Seq(
      organization := "com.github.cb372",
      scalaVersion := Versions.scala,
      scalacOptions ++= Seq("-unchecked", "-deprecation", "-feature"),
      resolvers += Resolver.typesafeRepo("releases"),
      libraryDependencies ++= commonDeps,
      parallelExecution in Test := false,
      releaseProcess := Seq[ReleaseStep](
        checkSnapshotDependencies,
        inquireVersions,
        runClean,
        runTest,
        setReleaseVersion,
        commitReleaseVersion,
        updateVersionInReadme,
        tagRelease,
        ReleaseStep(action = Command.process("publishSigned", _)),
        setNextVersion,
        commitNextVersion,
        ReleaseStep(action = Command.process("sonatypeReleaseAll", _)),
        pushChanges
      ),
      commands += Command.command("update-version-in-readme")(updateVersionInReadme)
    )

  lazy val implProjectSettings = commonSettings

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
      <scm>
        <url>git@github.com:cb372/scalacache.git</url>
        <connection>scm:git:git@github.com:cb372/scalacache.git</connection>
      </scm>
      <developers>
        <developer>
          <id>cb372</id>
          <name>Chris Birchall</name>
          <url>https://github.com/cb372</url>
        </developer>
      </developers>,
    publishTo <<= version { v =>
      val nexus = "https://oss.sonatype.org/"
      if (v.trim.endsWith("SNAPSHOT"))
        Some("snapshots" at nexus + "content/repositories/snapshots")
      else
        Some("releases"  at nexus + "service/local/staging/deploy/maven2")
    },
    publishMavenStyle := true,
    publishArtifact in Test := false,
    pomIncludeRepository := { _ => false }
  )

  // Scalariform preferences
  lazy val formatterPrefs = Seq(
    ScalariformKeys.preferences := ScalariformKeys.preferences.value
      .setPreference(AlignParameters, true)
      .setPreference(DoubleIndentClassDeclaration, true)
  )

  lazy val updateVersionInReadme = ReleaseStep(action = st => {
    val extracted = Project.extract(st)
    val projectVersion = extracted.get(Keys.version)

    println(s"Updating project version to $projectVersion in the README")
    Process(Seq("sed", "-i", "", "-E", "-e", s"""s/"scalacache-(.*)" % ".*"/"scalacache-\\1" % "$projectVersion"/g""", "README.md")).!
    println("Committing README.md")
    Process(Seq("git", "commit", "README.md", "-m", s"Update project version in README to $projectVersion")).!

    st
  })

}


