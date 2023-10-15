name := "tinkergraph"

dependsOn(Projects.base, Projects.base % "compile->compile;test->test", Projects.commons, Projects.gremlin)

libraryDependencies ++= Seq(
  "org.apache.tinkerpop" % "gremlin-driver"      % Versions.tinkerGraph,
  "org.apache.tinkerpop" % "tinkergraph-gremlin" % Versions.tinkerGraph
)
