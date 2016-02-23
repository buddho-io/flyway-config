
name := "flyway-config"

organization := "io.buddho"

scalaVersion := "2.11.7"

libraryDependencies ++= Seq(
  "com.typesafe" % "config" % "1.3.0",
  "org.flywaydb" % "flyway-core" % "3.2.1",
  "com.typesafe.scala-logging" %% "scala-logging" % "3.1.0",

  "org.scalatest" %% "scalatest" % "2.2.6" % "test"
)
