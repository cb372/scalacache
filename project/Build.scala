import sbt._
import Keys._

object CacheableBuild extends Build {
    lazy val root = Project(
      id = "cacheable",
      base = file("."),
      settings = standardSettings) aggregate(core, guava)

    lazy val core = Project(
      id = "cacheable-core",
      base = file("core"),
      settings = standardSettings ++ Seq(
        libraryDependencies ++= Seq(
          "org.scala-lang" % "scala-reflect" % "2.10.0"
        )
      )
    )

    lazy val guava = Project(
      id = "cacheable-guava",
      base = file("guava"),
      settings = standardSettings ++ Seq(
        libraryDependencies ++= Seq(
          "com.google.guava" % "guava" % "13.0.1",
          "com.google.code.findbugs" % "jsr305" % "1.3.9"
        )
      )
    ) dependsOn(core)

    lazy val standardSettings = Defaults.defaultSettings ++ Seq(
        organization := "com.github.cb372",
        version      := "0.1-SNAPSHOT",
        scalaVersion := "2.10.0"
    )
}


