package com.github.plume.oss

import better.files.File
import com.github.plume.oss.drivers.{
  IDriver,
  Neo4jDriver,
  NeptuneDriver,
  OverflowDbDriver,
  TigerGraphDriver,
  TinkerGraphDriver
}
import io.circe.Json
import io.shiftleft.x2cpg.{X2Cpg, X2CpgConfig}
import scopt.OParser

import java.io.InputStreamReader

/** Command line configuration parameters
  */
final case class Config(
    inputPaths: Set[String] = Set.empty,
    outputPath: String = X2CpgConfig.defaultOutputPath
) extends X2CpgConfig[Config] {

  override def withAdditionalInputPath(inputPath: String): Config =
    copy(inputPaths = inputPaths + inputPath)
  override def withOutputPath(x: String): Config = copy(outputPath = x)
}

/** Entry point for command line CPG creator
  */
object Plume extends App {

  private val frontendSpecificOptions = {
    val builder = OParser.builder[Config]
    import builder.programName
    OParser.sequence(programName("plume"))
  }

  private def parseDriverConfig(): (DriverConfig, IDriver) = {
    import io.circe.generic.auto._
    import io.circe.yaml.parser
    def interpretConfig(rawConf: Json) = {
      rawConf.as[DriverConfig] match {
        case Left(_)     => (null, new OverflowDbDriver())
        case Right(conf) => (conf, createDriver(conf))
      }
    }
    val f = File("driver.yaml")
    if (!f.exists) {
      println("No driver.yaml found, defaulting to OverflowDB driver.")
      return (null, new OverflowDbDriver())
    }
    parser.parse(new InputStreamReader(f.newInputStream)) match {
      case Left(_)        => (null, new OverflowDbDriver())
      case Right(rawConf) => interpretConfig(rawConf)
    }
  }

  private def createDriver(conf: DriverConfig): IDriver = {
    conf match {
      case _ if conf.database == "OverflowDB" =>
        new OverflowDbDriver(
          storageLocation = Option(conf.params.getOrElse("storageLocation", "cpg.odb")),
          heapPercentageThreshold = conf.params.getOrElse("heapPercentageThreshold", "80").toInt,
          serializationStatsEnabled =
            conf.params.getOrElse("serializationStatsEnabled", "false").toBoolean
        )
      case _ if conf.database == "TinkerGraph" => new TinkerGraphDriver()
      case _ if conf.database == "Neo4j" =>
        new Neo4jDriver(
          hostname = conf.params.getOrElse("hostname", "localhost"),
          port = conf.params.getOrElse("port", "7687").toInt,
          username = conf.params.getOrElse("username", "neo4j"),
          password = conf.params.getOrElse("password", "neo4j"),
          txMax = conf.params.getOrElse("txMax", "25").toInt
        )
      case _ if conf.database == "TigerGraph" =>
        new TigerGraphDriver(
          hostname = conf.params.getOrElse("hostname", "localhost"),
          restPpPort = conf.params.getOrElse("restPpPort", "7687").toInt,
          gsqlPort = conf.params.getOrElse("gsqlPort", "14240").toInt,
          username = conf.params.getOrElse("username", "tigergraph"),
          password = conf.params.getOrElse("password", "tigergraph"),
          timeout = conf.params.getOrElse("timeout", "3000").toInt,
          txMax = conf.params.getOrElse("txMax", "25").toInt
        )
      case _ if conf.database == "Neptune" =>
        new NeptuneDriver(
          hostname = conf.params.getOrElse("hostname", "localhost"),
          port = conf.params.getOrElse("port", "8182").toInt,
          keyCertChainFile =
            conf.params.getOrElse("keyCertChainFile", "src/main/resources/conf/SFSRootCAC2.pem"),
          txMax = conf.params.getOrElse("txMax", "50").toInt
        )
      case _ =>
        println(
          "No supported database specified by driver.yaml. Supported databases are: OverflowDB, TinkerGraph, Neo4j, Neptune, and TigerGraph."
        ); null
    }
  }

  private val configOption = X2Cpg.parseCommandLine(args, frontendSpecificOptions, Config())
  if (configOption.isEmpty) {
    System.exit(1)
  } else if (configOption.get.inputPaths.size != 1) {
    println("This frontend requires exactly one input path")
    System.exit(1)
  } else {
    val config = configOption.get
    if (config.inputPaths.size == 1) {
      val (conf, driver) = parseDriverConfig()
      if (driver == null) {
        println("Unable to create driver, bailing out...")
        System.exit(1)
      }
      driver match {
        case d: TinkerGraphDriver if conf != null =>
          val importPath = conf.params.get("importPath")
          if (importPath.isDefined) d.importGraph(importPath.get)
        case _ =>
      }
      val cpg = new Jimple2Cpg().createCpg(
        config.inputPaths.head,
        Some(config.outputPath),
        driver
      )
      driver match {
        case d: TinkerGraphDriver if conf != null =>
          val exportPath = conf.params.get("exportPath")
          if (exportPath.isDefined) d.exportGraph(exportPath.get)
        case _ =>
      }
      cpg.close()
    }
  }

}

case class DriverConfig(database: String, params: Map[String, String])
