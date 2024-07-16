package com.github.plume

import better.files.File
import com.github.plume.oss.drivers.*
import upickle.default.*

package object oss {

  case class PlumeConfig(
    inputDir: String = "",
    jmhMemoryGb: Int = 4,
    jmhOutputFile: String = File.newTemporaryFile("plume-jmh-output-").pathAsString,
    jmhResultFile: String = File.newTemporaryFile("plume-jmh-result-").pathAsString,
    dbConfig: DatabaseConfig = OverflowDbConfig()
  ) derives ReadWriter

  sealed trait DatabaseConfig derives ReadWriter {
    def toDriver: IDriver
  }

  case class TinkerGraphConfig(importPath: Option[String] = None, exportPath: Option[String] = None)
      extends DatabaseConfig {
    override def toDriver: IDriver = new TinkerGraphDriver()
  }

  case class OverflowDbConfig(
    storageLocation: String = "cpg.bin",
    heapPercentageThreshold: Int = 80,
    serializationStatsEnabled: Boolean = false
  ) extends DatabaseConfig {
    override def toDriver: IDriver =
      new OverflowDbDriver(Option(storageLocation), heapPercentageThreshold, serializationStatsEnabled)
  }

  case class Neo4jConfig(
    hostname: String = "localhost",
    port: Int = 7687,
    username: String = "neo4j",
    password: String = "neo4j",
    txMax: Int = 25
  ) extends DatabaseConfig {
    override def toDriver: IDriver = new Neo4jDriver(hostname, port, username, password, txMax)
  }

  case class Neo4jEmbeddedConfig(databaseName: String = "neo4j", databaseDir: String = "neo4j-db", txMax: Int = 25)
      extends DatabaseConfig {
    override def toDriver: IDriver = new Neo4jEmbeddedDriver(databaseName, File(databaseDir), txMax)
  }

  case class TigerGraphConfig(
    hostname: String = "localhost",
    restPpPort: Int = 7687,
    gsqlPort: Int = 14240,
    username: String = "tigergraph",
    password: String = "tigergraph",
    timeout: Int = 3000,
    txMax: Int = 25,
    scheme: String = "http"
  ) extends DatabaseConfig {
    override def toDriver: IDriver =
      new TigerGraphDriver(hostname, restPpPort, gsqlPort, username, password, timeout, scheme, txMax)
  }

  case class NeptuneConfig(
    hostname: String = "localhost",
    port: Int = 8182,
    keyCertChainFile: String = "src/main/resources/conf/SFSRootCAC2.pem",
    txMax: Int = 50
  ) extends DatabaseConfig {
    override def toDriver: IDriver = new NeptuneDriver(hostname, port, keyCertChainFile, txMax)
  }

}
