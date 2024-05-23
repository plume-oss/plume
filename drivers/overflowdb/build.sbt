name := "overflowdb"

dependsOn(Projects.base, Projects.base % "compile->compile;test->test", Projects.commons)

libraryDependencies ++= Seq(
  "io.shiftleft" %% "overflowdb-traversal" % Versions.overflowDb,
  "io.shiftleft" %% "codepropertygraph" % Versions.codePropertyGraph,
  "org.apache.commons" % "commons-text" % Versions.apacheCommonsText,
  "org.apache.tinkerpop" % "gremlin-driver"       % Versions.tinkerGraph % Test,
  "org.apache.tinkerpop" % "tinkergraph-gremlin"  % Versions.tinkerGraph % Test
)
