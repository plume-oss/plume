name := "base"

libraryDependencies ++= Seq(
  "io.shiftleft"            %% "overflowdb-traversal" % Versions.overflowDb,
  "io.shiftleft"            %% "codepropertygraph"    % Versions.codePropertyGraph,
  "org.slf4j"                % "slf4j-api"            % Versions.slf4j,
  "org.apache.logging.log4j" % "log4j-core"           % Versions.log4j % Test,
  "org.apache.logging.log4j" % "log4j-slf4j-impl"     % Versions.log4j % Test,
  "org.scalatest"           %% "scalatest"         % Versions.scalatest % Test
) ++ Seq(
  "io.circe" %% "circe-core",
  "io.circe" %% "circe-generic",
  "io.circe" %% "circe-parser"
).map(_ % Versions.circe % Test)
