name := "Plume"

inThisBuild(
  List(
    organization := "com.github.plume-oss",
    version := "1.2.7",
    scalaVersion := "2.13.8",
    crossScalaVersions := Seq("2.13.8", "3.1.1"),
    resolvers ++= Seq(
      Resolver.mavenLocal,
      Resolver.mavenCentral,
      Resolver.JCenterRepository,
      "Gradle Tooling" at "https://repo.gradle.org/gradle/libs-releases-local/"
    )
  )
)

lazy val commons = Projects.commons
// Drivers
lazy val base        = Projects.base
// Implementation
lazy val gremlin     = Projects.gremlin.dependsOn(base, commons)
lazy val tinkergraph = Projects.tinkergraph.dependsOn(base, commons, gremlin)
lazy val neptune     = Projects.neptune.dependsOn(base, commons, gremlin)
lazy val neo4j       = Projects.neo4j.dependsOn(base, commons)
lazy val tigergraph  = Projects.tigergraph.dependsOn(base, commons)
lazy val overflowDb  = Projects.overflowdb.dependsOn(base, commons)

lazy val root = (project in file("."))
  .aggregate(commons, base, gremlin, tinkergraph, neptune, neo4j, tigergraph, overflowDb)
  .dependsOn(commons, base, gremlin, tinkergraph, neptune, neo4j, tigergraph, overflowDb)

trapExit := false
Test / fork := true
Test / parallelExecution := false

libraryDependencies ++= Seq(
  "io.joern"                %% "semanticcpg"       % Versions.joern,
  "io.joern"                %% "dataflowengineoss" % Versions.joern,
  "io.joern"                %% "x2cpg"             % Versions.joern,
  "io.joern"                %% "jimple2cpg"        % Versions.joern,
  "io.joern"                %% "x2cpg"             % Versions.joern     % Test classifier "tests",
  "org.soot-oss"             % "soot"              % Versions.soot,
  "org.slf4j"                % "slf4j-api"         % Versions.slf4j,
  "org.scala-lang"           % "scala-reflect"     % scalaVersion.value,
  "org.apache.logging.log4j" % "log4j-core"        % Versions.log4j     % Test,
  "org.apache.logging.log4j" % "log4j-slf4j-impl"  % Versions.log4j     % Test,
  "org.scalatest"           %% "scalatest"         % Versions.scalatest % Test
)

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
