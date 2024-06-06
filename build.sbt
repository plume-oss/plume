name := "Plume"

inThisBuild(
  List(
    organization := "com.github.plume-oss",
    version := "2.0.0",
    scalaVersion := "3.4.2",
    resolvers ++= Seq(
      Resolver.mavenLocal,
      Resolver.mavenCentral,
      Resolver.JCenterRepository
    )
  )
)

lazy val commons = Projects.commons
// Drivers
lazy val base = Projects.base
// Implementation
lazy val gremlin     = Projects.gremlin
lazy val tinkergraph = Projects.tinkergraph
lazy val neptune     = Projects.neptune
lazy val neo4j       = Projects.neo4j
lazy val tigergraph  = Projects.tigergraph
lazy val overflowDb  = Projects.overflowdb

lazy val root = (project in file("."))
  .dependsOn(commons, base, gremlin, tinkergraph, neptune, neo4j, tigergraph, overflowDb)
  .aggregate(commons, base, gremlin, tinkergraph, neptune, neo4j, tigergraph, overflowDb)

trapExit := false
Test / fork := true
Test / parallelExecution := false

libraryDependencies ++= Seq(
  "io.joern"                %% "semanticcpg"       % Versions.joern,
  "io.joern"                %% "x2cpg"             % Versions.joern,
  "io.joern"                %% "jimple2cpg"        % Versions.joern,
  "io.joern"                %% "jimple2cpg"        % Versions.joern     % Test classifier "tests",
  "io.joern"                %% "x2cpg"             % Versions.joern     % Test classifier "tests",
  "org.slf4j"                % "slf4j-api"         % Versions.slf4j,
  "org.apache.logging.log4j" % "log4j-core"        % Versions.log4j     % Test,
  "org.apache.logging.log4j" % "log4j-slf4j-impl"  % Versions.log4j     % Test,
  "org.scalatest"           %% "scalatest"         % Versions.scalatest % Test
)

enablePlugins(JavaAppPackaging)

scmInfo := Some(
  ScmInfo(url("https://github.com/plume-oss/plume"), "git@github.com:plume-oss/plume.git")
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

Global / onChangedBuildSource := ReloadOnSourceChanges

publishMavenStyle := true
