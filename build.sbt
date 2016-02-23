import sbt.Keys._

lazy val root = (project in file("."))
  .enablePlugins(GitVersioning, GitBranchPrompt)
  .settings(
    name := "flyway-config",
    organization := "io.buddho",
    scalaVersion := "2.11.7",
    git.baseVersion := "1.0",
    scalacOptions := Seq("-unchecked", "-deprecation", "-feature"),
    bintrayOrganization := Some("buddho"),
    bintrayRepository := "mvn-public",
    publishMavenStyle := true,
    licenses += ("Apache-2.0", url("http://www.apache.org/licenses/LICENSE-2.0")),
    libraryDependencies ++= Seq(
      "com.typesafe" % "config" % "1.3.0",
      "org.flywaydb" % "flyway-core" % "3.2.1",
      "com.typesafe.scala-logging" %% "scala-logging" % "3.1.0",
      "org.scalatest" %% "scalatest" % "2.2.6" % "test"
    )
  )
