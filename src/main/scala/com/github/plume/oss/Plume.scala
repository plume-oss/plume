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
        new JimpleAst2Database(driver).createAst(Config().withInputPath(config.inputDir))
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

      cmd("flatgraph")
        .action((_, c) => c.copy(dbConfig = FlatGraphConfig()))
        .children(
          opt[String]("storage-location")
            .action((x, c) => c.copy(dbConfig = c.dbConfig.asInstanceOf[FlatGraphConfig].copy(storageLocation = x)))
        )

    }

}

case class DriverConfig(database: String, params: Map[String, String])
