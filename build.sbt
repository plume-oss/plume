name := "Plume"

inThisBuild(
  List(
    organization := "com.github.plume-oss",
    version      := "2.0.0",
    scalaVersion := "3.4.2",
    resolvers ++= Seq(
      Resolver.mavenLocal,
      Resolver.mavenCentral,
      Resolver.JCenterRepository,
      "Sonatype OSS" at "https://oss.sonatype.org/content/repositories/public",
      "Atlassian" at "https://packages.atlassian.com/mvn/maven-atlassian-external",
      "Gradle Releases" at "https://repo.gradle.org/gradle/libs-releases/"
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
lazy val neo4jEmbed  = Projects.neo4jEmbed
lazy val tigergraph  = Projects.tigergraph
lazy val overflowDb  = Projects.overflowdb
// AST creation
lazy val astcreator = Projects.astcreator

lazy val root = (project in file("."))
  .dependsOn(astcreator, commons, base, gremlin, tinkergraph, neptune, neo4j, neo4jEmbed, tigergraph, overflowDb)
  .aggregate(astcreator, commons, base, gremlin, tinkergraph, neptune, neo4j, neo4jEmbed, tigergraph, overflowDb)

trapExit                 := false
Test / fork              := true
Test / parallelExecution := false

libraryDependencies ++= Seq(
  "org.openjdk.jmh"          % "jmh-generator-annprocess" % Versions.jmh,
  "org.openjdk.jmh"          % "jmh-core"                 % Versions.jmh,
  "org.openjdk.jmh"          % "jmh-generator-bytecode"   % Versions.jmh,
  "org.openjdk.jmh"          % "jmh-generator-reflection" % Versions.jmh,
  "org.openjdk.jmh"          % "jmh-generator-asm"        % Versions.jmh,
  "org.slf4j"                % "slf4j-api"                % Versions.slf4j,
  "org.apache.logging.log4j" % "log4j-core"               % Versions.log4j     % Test,
  "org.apache.logging.log4j" % "log4j-slf4j-impl"         % Versions.log4j     % Test,
  "org.scalatest"           %% "scalatest"                % Versions.scalatest % Test
)

enablePlugins(JavaAppPackaging, JmhPlugin)

scmInfo := Some(ScmInfo(url("https://github.com/plume-oss/plume"), "git@github.com:plume-oss/plume.git"))

homepage := Some(url("https://github.com/plume-oss/plume/"))
licenses := List("Apache-2.0" -> url("https://www.apache.org/licenses/LICENSE-2.0"))
developers := List(
  Developer("DavidBakerEffendi", "David Baker Effendi", "dbe@sun.ac.za", url("https://github.com/DavidBakerEffendi")),
  Developer("fabsx00", "Fabian Yamaguchi", "fabs@shiftleft.io", url("https://github.com/fabsx00"))
)

Global / onChangedBuildSource := ReloadOnSourceChanges

publishMavenStyle := true

// Benchmark Tasks

lazy val datasetDir = taskKey[File]("Dataset directory")
datasetDir := baseDirectory.value / "workspace" / "defects4"
lazy val driversToBenchmark = taskKey[Seq[String]]("Drivers to benchmark")
driversToBenchmark := Seq("overflowdb")

lazy val defect4jDataset = taskKey[Seq[(String, String)]]("JARs for projects used in `defects4j`")
defect4jDataset :=
  Seq("Chart" -> "https://repo1.maven.org/maven2/org/jfree/jfreechart/1.5.5/jfreechart-1.5.5.jar")

lazy val benchmarkDownloadTask = taskKey[Unit](s"Download `defects4j` candidates for benchmarking")
benchmarkDownloadTask := {
  defect4jDataset.value.foreach { case (name, url) =>
    DownloadHelper.ensureIsAvailable(url, datasetDir.value / s"$name.jar")
  }
}

lazy val benchmarkTask = taskKey[Unit](s"Run JMH benchmarks against drivers")
benchmarkTask := {

  def benchmarkArgs(driver: String, project: String): String = {
    val projectDir  = (datasetDir.value / project).getAbsolutePath
    val resultsPath = baseDirectory.value / "results" / s"results-$driver-$project"
    val outputPath  = baseDirectory.value / "results" / s"output-$driver-$project"
    s"com.github.plume.oss.Benchmark $driver $projectDir -o ${outputPath.getAbsolutePath} -r ${resultsPath.getAbsolutePath}"
  }

  driversToBenchmark.value.foreach { driver =>
    defect4jDataset.value.foreach { case (_, project) =>
      println(s"[INFO] Benchmarking $driver on $project")
      (Jmh / runMain).toTask(benchmarkArgs(driver, project))
    }
  }

}
