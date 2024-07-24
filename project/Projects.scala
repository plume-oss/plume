import sbt.*

object Projects {

  val driversRoot = file("drivers")

  lazy val base        = project.in(driversRoot / "base")
  lazy val flatgraph   = project.in(driversRoot / "flatgraph")

  lazy val commons    = project.in(file("commons"))
  lazy val astcreator = project.in(file("astcreator"))
}
