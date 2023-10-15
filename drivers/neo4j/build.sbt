name := "neo4j"

dependsOn(Projects.base, Projects.base % "compile->compile;test->test", Projects.commons)

libraryDependencies ++= Seq(
  "org.neo4j.driver" % "neo4j-java-driver" % Versions.neo4j
)
