import sbt.*

object Projects {

  val driversRoot = file("drivers")

  lazy val base        = project.in(driversRoot / "base")
  lazy val neo4j       = project.in(driversRoot / "neo4j")
  lazy val neo4jEmbed  = project.in(driversRoot / "neo4j-embedded")
  lazy val tigergraph  = project.in(driversRoot / "tigergraph")
  lazy val gremlin     = project.in(driversRoot / "gremlin")
  lazy val neptune     = project.in(driversRoot / "neptune")
  lazy val overflowdb  = project.in(driversRoot / "overflowdb")
  lazy val tinkergraph = project.in(driversRoot / "tinkergraph")

  lazy val commons    = project.in(file("commons"))
  lazy val astcreator = project.in(file("astcreator"))
}
