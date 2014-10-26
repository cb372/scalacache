import sbt._
import Keys._
import com.typesafe.sbt.SbtScalariform._
import scalariform.formatter.preferences._
import xerial.sbt.Sonatype._
import SonatypeKeys._
import net.virtualvoid.sbt.graph.Plugin._
import scoverage.ScoverageSbtPlugin._

object ScalaCacheBuild extends Build {
  
  object Versions {
    val scala = "2.11.2"
    val project = "0.4.3-SNAPSHOT"
  }

  lazy val root = Project(id = "scalacache",base = file("."))
    .settings(commonSettings: _*)
    .settings(sonatypeSettings: _*)
    .settings(publishArtifact := false)
    .aggregate(core, guava, memcached, ehcache, redis)

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

  lazy val guava = Project(id = "scalacache-guava", base = file("guava"))
    .settings(implProjectSettings: _*)
    .settings(
      libraryDependencies ++= Seq(
        "com.google.guava" % "guava" % "18.0",
        "com.google.code.findbugs" % "jsr305" % "1.3.9"
      )
    )
    .dependsOn(core)

  lazy val memcached = Project(id = "scalacache-memcached", base = file("memcached"))
    .settings(implProjectSettings: _*)
    .settings(
      libraryDependencies ++= Seq(
        "net.spy" % "spymemcached" % "2.11.4"
      )
    )
    .dependsOn(core)

  lazy val ehcache = Project(id = "scalacache-ehcache", base = file("ehcache"))
    .settings(implProjectSettings: _*)
    .settings(
      libraryDependencies ++= Seq(
        "net.sf.ehcache" % "ehcache" % "2.8.4",
        "javax.transaction" % "jta" % "1.1"
      )
    )
    .dependsOn(core)

  lazy val redis = Project(id = "scalacache-redis", base = file("redis"))
    .settings(implProjectSettings: _*)
    .settings(
      libraryDependencies ++= Seq(
        "redis.clients" % "jedis" % "2.6.0"
      )
    )
    .dependsOn(core)

  lazy val jodaTime = Seq(
    "joda-time" % "joda-time" % "2.5",
    "org.joda" % "joda-convert" % "1.7"
  )

  lazy val scalaLogging = Seq(
    "com.typesafe.scala-logging" %% "scala-logging-slf4j" % "2.1.2"
  )

  lazy val scalaTest = Seq(
    "org.scalatest" %% "scalatest" % "2.2.2" % "test"
  ) ++ (if (Versions.scala.startsWith("2.11")) {
    // used in the scalatest reporter
    Seq("org.scala-lang.modules" %% "scala-xml" % "1.0.1" % "test")
  } else Nil)

  // Dependencies common to all projects
  lazy val commonDeps =
    scalaLogging ++
    scalaTest

  // Dependencies common to all implementation projects (i.e. everything except core)
  lazy val implProjectDeps = jodaTime

  lazy val commonSettings = 
    Defaults.coreDefaultSettings ++ 
    mavenSettings ++ 
    scalariformSettings ++
    formatterPrefs ++
    graphSettings ++
    instrumentSettings ++
    Seq(
      organization := "com.github.cb372",
      version      := Versions.project,
      scalaVersion := Versions.scala,
      scalacOptions ++= Seq("-unchecked", "-deprecation", "-feature"),
      libraryDependencies ++= commonDeps,
      parallelExecution in Test := false
    )

  lazy val implProjectSettings = commonSettings ++ Seq(
    libraryDependencies ++= implProjectDeps
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
}


