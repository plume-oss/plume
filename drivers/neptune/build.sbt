name := "neptune"

libraryDependencies ++= Seq(
  "com.softwaremill.sttp.client3"   %% "core"                    % Versions.sttp,
  "com.softwaremill.sttp.client3"   %% "circe"                   % Versions.sttp,
  "com.fasterxml.jackson.core"       % "jackson-databind"        % Versions.jackson,
  "com.fasterxml.jackson.module"    %% "jackson-module-scala"    % Versions.jackson,
  "com.fasterxml.jackson.dataformat" % "jackson-dataformat-cbor" % Versions.jackson,
  "org.scalaj"                       % "scalaj-http_2.13"        % Versions.scalajHttp
) ++ Seq(
  "io.circe" %% "circe-core",
  "io.circe" %% "circe-generic",
  "io.circe" %% "circe-parser",
  "io.circe" %% "circe-yaml"
).map(_ % Versions.circe)
