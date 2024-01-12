name := "gremlin"

dependsOn(Projects.base, Projects.base % "compile->compile;test->test", Projects.commons)

libraryDependencies ++= Seq(
  "org.apache.tinkerpop" % "gremlin-driver"      % Versions.tinkerGraph,
  "org.apache.tinkerpop" % "tinkergraph-gremlin" % Versions.tinkerGraph,
  "commons-codec"        % "commons-codec"       % Versions.apacheCodec
)
