name := "neptune"

dependsOn(
  Projects.base,
  Projects.base % "compile->compile;test->test",
  Projects.commons,
  Projects.gremlin
)

libraryDependencies ++= Seq(
  "com.softwaremill.sttp.client3" %% "core"  % Versions.sttp,
  "com.softwaremill.sttp.client3" %% "circe" % Versions.sttp
) ++ Seq(
  "io.circe" %% "circe-core",
  "io.circe" %% "circe-generic",
  "io.circe" %% "circe-parser"
).map(_ % Versions.circe)
