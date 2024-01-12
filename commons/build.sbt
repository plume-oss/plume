name := "commons"

libraryDependencies ++= Seq(
  "io.shiftleft" %% "overflowdb-traversal" % Versions.overflowDb,
  "io.shiftleft" %% "codepropertygraph"    % Versions.codePropertyGraph,
  "org.lz4"       % "lz4-java"             % Versions.lz4
)
