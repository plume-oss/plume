name := "souffle"

dependsOn(Projects.base, Projects.base % "compile->compile;test->test", Projects.commons)

libraryDependencies ++= Seq()
