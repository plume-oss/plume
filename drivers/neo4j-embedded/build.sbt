name := "neo4j-embedded"

dependsOn(Projects.base, Projects.base % "compile->compile;test->test", Projects.commons)

libraryDependencies ++= Seq(
  "org.neo4j" % "neo4j" % Versions.neo4j
)
