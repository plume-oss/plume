name := "Plume"

inThisBuild(
  List(
    organization := "com.github.plume-oss",
    version := "1.1.0",
    scalaVersion := "2.13.7",
    crossScalaVersions := Seq("2.13.7", "3.1.1"),
    resolvers ++= Seq(
      Resolver.mavenLocal,
      Resolver.mavenCentral,
      Resolver.JCenterRepository
    )
  )
)

val cpgVersion           = "1.3.515"
val joernVersion         = "1.1.619"
val sootVersion          = "4.2.1"
val tinkerGraphVersion   = "3.4.8"
val neo4jVersion         = "4.4.3"
val apacheCommonsVersion = "1.15"
val sttpVersion          = "3.5.1"
val jacksonVersion       = "2.13.2"
val scalajHttpVersion    = "2.4.2"
val lz4Version           = "1.8.0"
val slf4jVersion         = "1.7.36"
val log4jVersion         = "2.17.2"
val logbackVersion       = "1.2.11"
val scalatestVersion     = "3.2.11"
val circeVersion         = "0.14.1"

lazy val scalatest         = "org.scalatest" %% "scalatest" % scalatestVersion
lazy val NeoIntTest        = config("neoTest") extend Test
lazy val TigerGraphIntTest = config("tgTest") extend Test
lazy val NeptuneIntTest    = config("nepTest") extend Test

trapExit := false
Test / fork := true
Test / parallelExecution := false

libraryDependencies ++= Seq(
  "io.shiftleft"                  %% "codepropertygraph"    % cpgVersion,
  "io.shiftleft"                  %% "semanticcpg"          % cpgVersion,
  "io.joern"                      %% "dataflowengineoss"    % joernVersion,
  "io.joern"                      %% "x2cpg"                % joernVersion,
  "io.joern"                      %% "jimple2cpg"           % joernVersion,
  "io.joern"                      %% "x2cpg"                % joernVersion % Test classifier "tests",
  "org.soot-oss"                   % "soot"                 % sootVersion,
  "org.apache.tinkerpop"           % "tinkergraph-gremlin"  % tinkerGraphVersion,
  "org.apache.tinkerpop"           % "gremlin-driver"       % tinkerGraphVersion,
  "org.neo4j.driver"               % "neo4j-java-driver"    % neo4jVersion,
  "commons-codec"                  % "commons-codec"        % apacheCommonsVersion,
  "com.softwaremill.sttp.client3" %% "core"                 % sttpVersion,
  "com.softwaremill.sttp.client3" %% "circe"                % sttpVersion,
  "com.fasterxml.jackson.core"     % "jackson-databind"     % jacksonVersion,
  "com.fasterxml.jackson.module"  %% "jackson-module-scala" % jacksonVersion,
  "org.scalaj"                     % "scalaj-http_2.13"     % scalajHttpVersion,
  "org.lz4"                        % "lz4-java"             % lz4Version,
  "org.slf4j"                      % "slf4j-api"            % slf4jVersion,
  "org.scala-lang"                 % "scala-reflect"        % scalaVersion.value,
  "org.apache.logging.log4j"       % "log4j-core"           % log4jVersion     % Test,
  "org.apache.logging.log4j"       % "log4j-slf4j-impl"     % log4jVersion     % Test,
  "org.scalatest"                 %% "scalatest"            % scalatestVersion % Test
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
  ScmInfo(url("https://github.com/plume-oss/plume"), "git@github.com:plume-oss/plume.git")
)
git.remoteRepo := scmInfo.value.get.connection

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

Global / onChangedBuildSource := ReloadOnSourceChanges

publishMavenStyle := true

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
