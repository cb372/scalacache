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
    .aggregate(core, guava, memcached)

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

  lazy val jodaTime = Seq(
    "joda-time" % "joda-time" % "2.3",
    "org.joda" % "joda-convert" % "1.2"
  )

  lazy val standardSettings = Defaults.defaultSettings ++ Seq(
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
}


