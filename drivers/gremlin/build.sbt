name := "gremlin"

libraryDependencies ++= Seq(
  "org.apache.tinkerpop" % "gremlin-driver"      % Versions.tinkerGraph,
  "org.apache.tinkerpop" % "tinkergraph-gremlin" % Versions.tinkerGraph,
  "commons-codec"        % "commons-codec"       % Versions.apacheCodec,
  "commons-io"           % "commons-io"          % Versions.apacheIo,
  "org.apache.commons"   % "commons-lang3"       % Versions.apacheLang
)
