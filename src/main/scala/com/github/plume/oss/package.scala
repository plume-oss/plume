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
    dbConfig: DatabaseConfig = FlatGraphConfig()
  ) derives ReadWriter

  sealed trait DatabaseConfig derives ReadWriter {
    def toDriver: IDriver

    def shortName: String
  }

  case class FlatGraphConfig(storageLocation: String = "cpg.bin") extends DatabaseConfig {
    override def toDriver: IDriver =
      new FlatGraphDriver(Option(storageLocation))

    override def shortName: String = "flatgraph"
  }

}
