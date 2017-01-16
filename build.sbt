import sbt.Keys._

lazy val root = (project in file("."))
  .enablePlugins(GitVersioning, GitBranchPrompt)
  .settings(
    name := "flyway-config",
    organization := "io.buddho",
    scalaVersion := "2.12.1",
    crossScalaVersions := Seq("2.11.8", "2.12.1"),
    git.baseVersion := "1.2",
    scalacOptions := Seq("-unchecked", "-deprecation", "-feature"),
    bintrayOrganization := Some("buddho"),
    bintrayRepository := "mvn-public",
    publishMavenStyle := true,
    licenses += ("Apache-2.0", url("http://www.apache.org/licenses/LICENSE-2.0")),
    libraryDependencies ++= Seq(
      "com.typesafe" % "config" % "1.3.1",
      "org.flywaydb" % "flyway-core" % "3.2.1",
      "com.typesafe.scala-logging" %% "scala-logging" % "3.5.0",
      "org.scalatest" %% "scalatest" % "3.0.1" % "test"
    )
  )
