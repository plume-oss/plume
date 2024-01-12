name := "tigergraph"

dependsOn(Projects.base, Projects.base % "compile->compile;test->test", Projects.commons)

libraryDependencies ++= Seq(
  "com.softwaremill.sttp.client3" %% "core"  % Versions.sttp,
  "com.softwaremill.sttp.client3" %% "circe" % Versions.sttp
) ++ Seq(
  "io.circe" %% "circe-core",
  "io.circe" %% "circe-generic",
  "io.circe" %% "circe-parser",
  "io.circe" %% "circe-yaml"
).map(_ % Versions.circe)
