import sbt._
import Keys._
import CoverallsPlugin.CoverallsKeys._

object CacheableBuild extends Build {
  
  object Versions {
    val scala = "2.10.3"
  }

  lazy val root = Project(id = "cacheable",base = file("."))
    .settings(standardSettings: _*)
    .settings(ScctPlugin.mergeReportSettings: _*)
    .settings(CoverallsPlugin.multiProject: _*)
    .settings(coverallsTokenFile := "coveralls-token.txt")
    .settings(publishArtifact := false)
    .aggregate(core, guava, memcached, ehcache, redis)

  lazy val core = Project(id = "cacheable-core", base = file("core"))
    .settings(standardSettings: _*)
    .settings(ScctPlugin.instrumentSettings: _*) 
    .settings(
      libraryDependencies ++= Seq(
        "org.scala-lang" % "scala-reflect" % Versions.scala
      )
    )

  lazy val guava = Project(id = "cacheable-guava", base = file("guava"))
    .settings(standardSettings: _*)
    .settings(ScctPlugin.instrumentSettings: _*) 
    .settings(
      libraryDependencies ++= jodaTime ++ Seq(
        "com.google.guava" % "guava" % "15.0",
        "com.google.code.findbugs" % "jsr305" % "1.3.9"
      )
    )
    .dependsOn(core)

  lazy val memcached = Project(id = "cacheable-memcached", base = file("memcached"))
    .settings(standardSettings: _*)
    .settings(ScctPlugin.instrumentSettings: _*) 
    .settings(
      libraryDependencies ++= jodaTime ++ Seq(
        "net.spy" % "spymemcached" % "2.10.2"
      )
    )
    .dependsOn(core)

  lazy val ehcache = Project(id = "cacheable-ehcache", base = file("ehcache"))
    .settings(standardSettings: _*)
    .settings(ScctPlugin.instrumentSettings: _*) 
    .settings(
      libraryDependencies ++= jodaTime ++ Seq(
        "net.sf.ehcache" % "ehcache" % "2.7.4",
        "javax.transaction" % "jta" % "1.1"
      )
    )
    .dependsOn(core)

  lazy val redis = Project(id = "cacheable-redis", base = file("redis"))
    .settings(standardSettings: _*)
    .settings(ScctPlugin.instrumentSettings: _*) 
    .settings(
      libraryDependencies ++= jodaTime ++ Seq(
        "net.debasishg" %% "redisclient" % "2.11"
      )
    )
    .dependsOn(core)

  lazy val jodaTime = Seq(
    "joda-time" % "joda-time" % "2.3",
    "org.joda" % "joda-convert" % "1.2"
  )

  lazy val standardSettings = Defaults.defaultSettings ++ mavenSettings ++ Seq(
    organization := "com.github.cb372",
    version      := "0.1-SNAPSHOT",
    scalaVersion := Versions.scala,
    scalacOptions ++= Seq("-unchecked", "-deprecation", "-feature"),
    libraryDependencies ++= Seq(
      "com.typesafe" %% "scalalogging-slf4j" % "1.0.1",
      "org.scalatest" %% "scalatest" % "2.0" % "test"
      //"org.scalamock" %% "scalamock-scalatest-support" % "3.0.1" % "test"
    ),
    parallelExecution in Test := false
  )

  lazy val mavenSettings = Seq(
    pomExtra :=
      <url>https://github.com/cb372/cacheable</url>
      <licenses>
        <license>
          <name>Apache License, Version 2.0</name>
          <url>http://www.apache.org/licenses/LICENSE-2.0.html</url>
          <distribution>repo</distribution>
        </license>
      </licenses>
      <scm>
        <url>git@github.com:cb372/cacheable.git</url>
        <connection>scm:git:git@github.com:cb372/cacheable.git</connection>
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
}


