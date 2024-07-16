package com.github.plume.oss

import com.github.plume.oss.drivers.*
import io.joern.jimple2cpg.Config
import scopt.{OParser, OptionParser}

/** Entry point for command line CPG creator
  */
object Plume {

  private val frontendSpecificOptions = {
    val builder = OParser.builder[Config]
    import builder.programName
    OParser.sequence(programName("plume"))
  }

  def main(args: Array[String]): Unit = {
    Plume
      .optionParser("plume", "An AST creator for comparing graph databases as static analysis backends.")
      .parse(args, PlumeConfig())
      .foreach { config =>
        val driver = config.dbConfig.toDriver
        driver match {
          case d: TinkerGraphDriver =>
            config.dbConfig.asInstanceOf[TinkerGraphConfig].importPath.foreach(d.importGraph)
          case _ =>
        }
        new JimpleAst2Database(driver).createAst(Config().withInputPath(config.inputDir))
        driver match {
          case d: TinkerGraphDriver =>
            config.dbConfig.asInstanceOf[TinkerGraphConfig].exportPath.foreach(d.exportGraph)
          case _ =>
        }
      }
  }

  def optionParser(name: String, description: String): OptionParser[PlumeConfig] =
    new OptionParser[PlumeConfig](name) {

      note(description)
      help('h', "help")

      arg[String]("input-dir")
        .text("The target application to parse.")
        .action((x, c) => c.copy(inputDir = x))

      opt[Int]('m', "jmh-memory")
        .text(s"The -Xmx JVM argument in Gb for JMH. Default is 4 (-Xmx4G).")
        .hidden()
        .validate {
          case x if x < 1 => failure("Consider at least 1Gb")
          case _          => success
        }
        .action((x, c) => c.copy(jmhMemoryGb = x))

      opt[String]('o', "jmh-output-file")
        .text(s"The JMH output file path. Exclude file extensions.")
        .hidden()
        .action((x, c) => c.copy(jmhOutputFile = x))

      opt[String]('r', "jmh-result-file")
        .text(s"The result file path. Exclude file extensions.")
        .hidden()
        .action((x, c) => c.copy(jmhResultFile = x))

      cmd("tinkergraph")
        .action((_, c) => c.copy(dbConfig = TinkerGraphConfig()))
        .children(
          opt[String]("import-path")
            .text("The TinkerGraph to import.")
            .action((x, c) =>
              c.copy(dbConfig = c.dbConfig.asInstanceOf[TinkerGraphConfig].copy(importPath = Option(x)))
            ),
          opt[String]("export-path")
            .text("The TinkerGraph export path to serialize the result to.")
            .action((x, c) =>
              c.copy(dbConfig = c.dbConfig.asInstanceOf[TinkerGraphConfig].copy(exportPath = Option(x)))
            )
        )

      cmd("overflowdb")
        .action((_, c) => c.copy(dbConfig = OverflowDbConfig()))
        .children(
          opt[String]("storage-location")
            .action((x, c) => c.copy(dbConfig = c.dbConfig.asInstanceOf[OverflowDbConfig].copy(storageLocation = x))),
          opt[Int]("heap-percentage-threshold")
            .action((x, c) =>
              c.copy(dbConfig = c.dbConfig.asInstanceOf[OverflowDbConfig].copy(heapPercentageThreshold = x))
            ),
          opt[Unit]("enable-serialization-stats")
            .action((_, c) =>
              c.copy(dbConfig = c.dbConfig.asInstanceOf[OverflowDbConfig].copy(serializationStatsEnabled = true))
            )
        )

      cmd("neo4j")
        .action((_, c) => c.copy(dbConfig = Neo4jConfig()))
        .children(
          opt[String]("hostname")
            .action((x, c) => c.copy(dbConfig = c.dbConfig.asInstanceOf[Neo4jConfig].copy(hostname = x))),
          opt[Int]("port")
            .action((x, c) => c.copy(dbConfig = c.dbConfig.asInstanceOf[Neo4jConfig].copy(port = x))),
          opt[String]("username")
            .action((x, c) => c.copy(dbConfig = c.dbConfig.asInstanceOf[Neo4jConfig].copy(username = x))),
          opt[String]("password")
            .action((x, c) => c.copy(dbConfig = c.dbConfig.asInstanceOf[Neo4jConfig].copy(password = x))),
          opt[Int]("tx-max")
            .action((x, c) => c.copy(dbConfig = c.dbConfig.asInstanceOf[Neo4jConfig].copy(txMax = x)))
        )

      cmd("neo4j-embedded")
        .action((_, c) => c.copy(dbConfig = Neo4jEmbeddedConfig()))
        .children(
          opt[String]("databaseName")
            .action((x, c) => c.copy(dbConfig = c.dbConfig.asInstanceOf[Neo4jEmbeddedConfig].copy(databaseName = x))),
          opt[String]("databaseDir")
            .action((x, c) => c.copy(dbConfig = c.dbConfig.asInstanceOf[Neo4jEmbeddedConfig].copy(databaseDir = x))),
          opt[Int]("tx-max")
            .action((x, c) => c.copy(dbConfig = c.dbConfig.asInstanceOf[Neo4jEmbeddedConfig].copy(txMax = x)))
        )

      cmd("tigergraph")
        .action((_, c) => c.copy(dbConfig = TigerGraphConfig()))
        .children(
          opt[String]("hostname")
            .action((x, c) => c.copy(dbConfig = c.dbConfig.asInstanceOf[TigerGraphConfig].copy(hostname = x))),
          opt[Int]("restpp-port")
            .action((x, c) => c.copy(dbConfig = c.dbConfig.asInstanceOf[TigerGraphConfig].copy(restPpPort = x))),
          opt[Int]("gsql-port")
            .action((x, c) => c.copy(dbConfig = c.dbConfig.asInstanceOf[TigerGraphConfig].copy(gsqlPort = x))),
          opt[String]("username")
            .action((x, c) => c.copy(dbConfig = c.dbConfig.asInstanceOf[TigerGraphConfig].copy(username = x))),
          opt[String]("password")
            .action((x, c) => c.copy(dbConfig = c.dbConfig.asInstanceOf[TigerGraphConfig].copy(password = x))),
          opt[Int]("timeout")
            .action((x, c) => c.copy(dbConfig = c.dbConfig.asInstanceOf[TigerGraphConfig].copy(timeout = x))),
          opt[Int]("tx-max")
            .action((x, c) => c.copy(dbConfig = c.dbConfig.asInstanceOf[TigerGraphConfig].copy(txMax = x))),
          opt[String]("scheme")
            .action((x, c) => c.copy(dbConfig = c.dbConfig.asInstanceOf[TigerGraphConfig].copy(scheme = x)))
        )

      cmd("neptune")
        .action((_, c) => c.copy(dbConfig = NeptuneConfig()))
        .children(
          opt[String]("hostname")
            .action((x, c) => c.copy(dbConfig = c.dbConfig.asInstanceOf[NeptuneConfig].copy(hostname = x))),
          opt[Int]("port")
            .action((x, c) => c.copy(dbConfig = c.dbConfig.asInstanceOf[NeptuneConfig].copy(port = x))),
          opt[String]("key-cert-chain-file")
            .action((x, c) => c.copy(dbConfig = c.dbConfig.asInstanceOf[NeptuneConfig].copy(keyCertChainFile = x))),
          opt[Int]("tx-max")
            .action((x, c) => c.copy(dbConfig = c.dbConfig.asInstanceOf[NeptuneConfig].copy(txMax = x)))
        )

    }

}

case class DriverConfig(database: String, params: Map[String, String])
