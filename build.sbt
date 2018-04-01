import sbt.Keys._

lazy val root = (project in file("."))
  .enablePlugins(GitVersioning, GitBranchPrompt)
  .settings(
    name := "flyway-config",
    organization := "io.buddho",
    scalaVersion := "2.12.4",
    crossScalaVersions := Seq("2.11.8", "2.12.4"),
    git.baseVersion := "2.0",
    scalacOptions := Seq("-unchecked", "-deprecation", "-feature"),
    bintrayOrganization := Some("buddho"),
    bintrayRepository := "mvn-public",
    publishMavenStyle := true,
    licenses += ("Apache-2.0", url("http://www.apache.org/licenses/LICENSE-2.0")),
    libraryDependencies ++= Seq(
      "com.typesafe" % "config" % "1.3.3",
      "org.flywaydb" % "flyway-core" % "5.0.7",
      "com.typesafe.scala-logging" %% "scala-logging" % "3.8.0",
      "org.scalatest" %% "scalatest" % "3.0.5" % "test"
    )
  )
