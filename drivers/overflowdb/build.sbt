name := "overflowdb"

libraryDependencies ++= Seq(
  "io.shiftleft"        %% "overflowdb-traversal" % Versions.overflowDb,
  "io.shiftleft"        %% "codepropertygraph"    % Versions.codePropertyGraph,
  "io.joern"            %% "semanticcpg"          % Versions.joern,
  "io.joern"            %% "dataflowengineoss"    % Versions.joern,
  "org.apache.tinkerpop" % "gremlin-driver"       % Versions.tinkerGraph % Test,
  "org.apache.tinkerpop" % "tinkergraph-gremlin"  % Versions.tinkerGraph % Test
)
