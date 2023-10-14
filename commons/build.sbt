name := "commons"

libraryDependencies ++= Seq(
  "io.shiftleft"                    %% "overflowdb-traversal"    % Versions.overflowDb,
  "io.shiftleft"                    %% "codepropertygraph"       % Versions.codePropertyGraph,
  "com.softwaremill.sttp.client3"   %% "core"                    % Versions.sttp,
  "com.softwaremill.sttp.client3"   %% "circe"                   % Versions.sttp,
  "com.fasterxml.jackson.core"       % "jackson-databind"        % Versions.jackson,
  "com.fasterxml.jackson.module"    %% "jackson-module-scala"    % Versions.jackson,
  "com.fasterxml.jackson.dataformat" % "jackson-dataformat-cbor" % Versions.jackson,
  "org.scalaj"                       % "scalaj-http_2.13"        % Versions.scalajHttp,
  "org.lz4"                          % "lz4-java"                % Versions.lz4
) ++ Seq(
  "io.circe" %% "circe-core",
  "io.circe" %% "circe-generic",
  "io.circe" %% "circe-parser",
  "io.circe" %% "circe-yaml"
).map(_ % Versions.circe)
