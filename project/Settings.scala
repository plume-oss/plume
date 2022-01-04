
import sbt.Keys._
import sbtbuildinfo.{BuildInfoKey, BuildInfoOption}
import sbtbuildinfo.BuildInfoKeys.{buildInfoKeys, buildInfoOptions, buildInfoPackage}
import scoverage.ScoverageKeys.coverageEnabled

private lazy val buildInfoSettings = Seq(

  buildInfoKeys := Seq[BuildInfoKey](name, version, scalaVersion, sbtVersion),

  buildInfoOptions ++= { if (coverageEnabled.value) Seq() else Seq(BuildInfoOption.BuildTime) }, // <-- this line was changed
  buildInfoOptions += BuildInfoOption.ToJson,

  buildInfoPackage := "pme123.adapters.version"
)

