name := "Plume"
organization := "io.github.plume-oss"
version := "0.1.0"

scalaVersion := "2.13.6"

val cpgVersion         = "1.3.433"
val joernVersion       = "1.1.349"
val sootVersion        = "4.2.1"
val tinkerGraphVersion = "3.4.8"
val neo4jVersion       = "4.3.4"
val tigerGraphVersion  = "3.1.0"
val sttpVersion        = "3.3.16"
val lz4Version         = "1.8.0"
val slf4jVersion       = "1.7.32"
val scalatestVersion   = "3.2.9"
val circeVersion       = "0.14.1"

lazy val scalatest         = "org.scalatest" %% "scalatest" % scalatestVersion
lazy val NeoIntTest        = config("neoTest") extend Test
lazy val TigerGraphIntTest = config("tgTest") extend Test
lazy val NeptuneIntTest    = config("nepTest") extend Test

fork := true

resolvers ++= Seq(
  Resolver.mavenLocal,
  Resolver.mavenCentral,
  Resolver.JCenterRepository
)

trapExit := false

libraryDependencies ++= Seq(
  "io.shiftleft"                  %% "codepropertygraph"   % cpgVersion,
  "io.shiftleft"                  %% "semanticcpg"         % cpgVersion,
  "io.joern"                      %% "dataflowengineoss"   % joernVersion,
  "io.shiftleft"                  %% "semanticcpg-tests"   % "1.3.405"        % Test classifier "tests",
  "org.soot-oss"                   % "soot"                % sootVersion,
  "org.apache.tinkerpop"           % "tinkergraph-gremlin" % tinkerGraphVersion,
  "org.apache.tinkerpop"           % "gremlin-driver"      % tinkerGraphVersion,
  "org.neo4j.driver"               % "neo4j-java-driver"   % neo4jVersion,
  "com.tigergraph.client"          % "gsql_client"         % tigerGraphVersion,
  "com.softwaremill.sttp.client3" %% "core"                % sttpVersion,
  "com.softwaremill.sttp.client3" %% "circe"               % sttpVersion,
  "org.scalaj"                     % "scalaj-http_2.11"    % "2.4.2",
  "org.lz4"                        % "lz4-java"            % lz4Version,
  "org.slf4j"                      % "slf4j-api"           % slf4jVersion,
  "org.slf4j"                      % "slf4j-simple"        % slf4jVersion,
  "org.scalatest"                 %% "scalatest"           % scalatestVersion % Test
) ++ Seq(
  "io.circe" %% "circe-core",
  "io.circe" %% "circe-generic",
  "io.circe" %% "circe-parser",
  "io.circe" %% "circe-yaml"
).map(_ % circeVersion)

enablePlugins(
  JavaAppPackaging,
  GitVersioning,
  BuildInfoPlugin,
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
  .configs(NeoIntTest)
  .settings(
    inConfig(NeoIntTest)(Defaults.testSettings),
    libraryDependencies += scalatest % NeoIntTest,
    Test / testOptions := Seq(Tests.Filter(s => !s.endsWith("IntTests"))),
    NeoIntTest / testOptions := Seq(Tests.Filter(s => s.contains("Neo4j")))
  )
  .configs(TigerGraphIntTest)
  .settings(
    inConfig(TigerGraphIntTest)(Defaults.testSettings),
    libraryDependencies += scalatest % TigerGraphIntTest,
    Test / testOptions := Seq(Tests.Filter(s => !s.endsWith("IntTests"))),
    TigerGraphIntTest / testOptions := Seq(Tests.Filter(s => s.contains("TigerGraph")))
  )
  .configs(NeptuneIntTest)
  .settings(
    inConfig(NeptuneIntTest)(Defaults.testSettings),
    libraryDependencies += scalatest % NeptuneIntTest,
    Test / testOptions := Seq(Tests.Filter(s => !s.endsWith("IntTests"))),
    NeptuneIntTest / testOptions := Seq(Tests.Filter(s => s.contains("Neptune")))
  )
