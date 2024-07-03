name := "astcreator"

dependsOn(Projects.base, Projects.commons, Projects.gremlin, Projects.overflowdb)

libraryDependencies ++= Seq(
  "io.joern"       %% "semanticcpg"              % Versions.joern,
  "io.joern"       %% "x2cpg"                    % Versions.joern,
  "io.joern"       %% "jimple2cpg"               % Versions.joern,
  "io.joern"       %% "jimple2cpg"               % Versions.joern % Test classifier "tests",
  "io.joern"       %% "x2cpg"                    % Versions.joern % Test classifier "tests",
)
