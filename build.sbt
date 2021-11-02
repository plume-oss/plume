name := "Plume"
organization := "io.github.plume-oss"
version := "0.1.0"

scalaVersion := "2.13.6"

val cpgVersion         = "1.3.384"
val sootVersion        = "4.2.1"
val tinkerGraphVersion = "3.5.1"
val slf4jVersion       = "1.7.32"
val scalatestVersion   = "3.2.9"

lazy val scalatest  = "org.scalatest" %% "scalatest" % scalatestVersion
//lazy val OdbIntTest = config("odbTest") extend Test

fork := true

resolvers ++= Seq(
  Resolver.mavenLocal,
  Resolver.mavenCentral
)

trapExit := false

libraryDependencies ++= Seq(
  "io.shiftleft"        %% "codepropertygraph"   % cpgVersion,
  "io.shiftleft"        %% "semanticcpg"         % cpgVersion,
  "io.shiftleft"        %% "dataflowengineoss"   % cpgVersion,
  "io.shiftleft"        %% "semanticcpg-tests"   % cpgVersion       % Test classifier "tests",
  "org.soot-oss"         % "soot"                % sootVersion,
  "org.apache.tinkerpop" % "tinkergraph-gremlin" % tinkerGraphVersion,
  "org.slf4j"            % "slf4j-api"           % slf4jVersion,
  "org.slf4j"            % "slf4j-simple"        % slf4jVersion,
  "org.scalatest"       %% "scalatest"           % scalatestVersion % Test
)

enablePlugins(
  JavaAppPackaging,
  GitVersioning,
  BuildInfoPlugin,
  DockerComposePlugin,
  GhpagesPlugin,
  SiteScaladocPlugin
)

scmInfo := Some(
  ScmInfo(url("https://github.com/plume-oss/plume"), "scm:git@github.com:/plume-oss/plume.git")
)
homepage := Some(url("https://github.com/plume-oss/plume/"))
licenses := List("Apache-2.0" -> url("https://www.apache.org/licenses/LICENSE-2.0"))
developers := List(
  Developer(
    "DavidBakerEffendi",
    "David Baker Effendi",
    "dbe@sun.ac.za",
    url("https://github.com/DavidBakerEffendi")
  ),
  Developer(
    "fabsx00",
    "Fabian Yamaguchi",
    "fabs@shiftleft.io",
    url("https://github.com/fabsx00")
  )
)

git.remoteRepo := "git@github.com:plume-oss/plume.git"

Global / onChangedBuildSource := ReloadOnSourceChanges

lazy val root = (project in file("."))
//  .configs(OdbIntTest)
  .settings(
//    inConfig(OdbIntTest)(Defaults.testSettings),
//    libraryDependencies += scalatest % OdbIntTest,
    Test / testOptions := Seq(Tests.Filter(s => !s.endsWith("IntTests"))),
//    OdbIntTest / testOptions := Seq(Tests.Filter(s => s.contains("OverflowDb")))
  )
