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
lazy val flatgraph  = Projects.flatgraph
// AST creation
lazy val astcreator = Projects.astcreator

lazy val root = (project in file("."))
  .dependsOn(astcreator, commons, base, flatgraph)
  .aggregate(astcreator, commons, base, flatgraph)

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
datasetDir := baseDirectory.value / "workspace" / "defects4j"

lazy val defect4jDataset = taskKey[Seq[(String, String)]]("JARs for projects used in `defects4j`")
defect4jDataset :=
  Seq(
    "Chart" -> "https://repo1.maven.org/maven2/org/jfree/jfreechart/1.5.5/jfreechart-1.5.5.jar",
    "Cli"   -> "https://repo1.maven.org/maven2/commons-cli/commons-cli/1.8.0/commons-cli-1.8.0.jar",
    "Closure" -> "https://repo1.maven.org/maven2/com/google/javascript/closure-compiler/v20240317/closure-compiler-v20240317.jar",
    "Codec" -> "https://repo1.maven.org/maven2/commons-codec/commons-codec/1.17.0/commons-codec-1.17.0.jar",
    "Collections" -> "https://repo1.maven.org/maven2/org/apache/commons/commons-collections4/4.4/commons-collections4-4.4.jar",
    "Compress" -> "https://repo1.maven.org/maven2/org/apache/commons/commons-compress/1.26.2/commons-compress-1.26.2.jar",
    "Csv" -> "https://repo1.maven.org/maven2/org/apache/commons/commons-csv/1.11.0/commons-csv-1.11.0.jar",
    "Gson" -> "https://repo1.maven.org/maven2/com/google/code/gson/gson/2.11.0/gson-2.11.0.jar",
    "JacksonCore" -> "https://repo1.maven.org/maven2/com/fasterxml/jackson/core/jackson-core/2.17.2/jackson-core-2.17.2.jar",
    "JacksonDatabind" -> "https://repo1.maven.org/maven2/com/fasterxml/jackson/core/jackson-databind/2.17.2/jackson-databind-2.17.2.jar",
    "JacksonXml" -> "https://repo1.maven.org/maven2/com/fasterxml/jackson/dataformat/jackson-dataformat-xml/2.17.2/jackson-dataformat-xml-2.17.2.jar",
    "Jsoup" -> "https://repo1.maven.org/maven2/org/jsoup/jsoup/1.18.1/jsoup-1.18.1.jar",
    "JxPath" -> "https://repo1.maven.org/maven2/commons-jxpath/commons-jxpath/1.3/commons-jxpath-1.3.jar",
    "Lang" -> "https://repo1.maven.org/maven2/org/apache/commons/commons-lang3/3.14.0/commons-lang3-3.14.0.jar",
    "Math" -> "https://repo1.maven.org/maven2/org/apache/commons/commons-math3/3.6.1/commons-math3-3.6.1.jar",
    "Mockito" -> "https://repo1.maven.org/maven2/org/mockito/mockito-core/5.12.0/mockito-core-5.12.0.jar",
    "Time" -> "https://repo1.maven.org/maven2/joda-time/joda-time/2.12.7/joda-time-2.12.7.jar"
  )

lazy val benchmarkDownloadTask = taskKey[Unit](s"Download `defects4j` candidates for benchmarking")
benchmarkDownloadTask := {
  defect4jDataset.value.foreach { case (name, url) =>
    DownloadHelper.ensureIsAvailable(url, datasetDir.value / s"$name.jar")
  }
}
