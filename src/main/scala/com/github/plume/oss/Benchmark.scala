package com.github.plume.oss

import better.files.File
import com.github.plume.oss.Benchmark.BenchmarkType.*
import com.github.plume.oss.benchmarking.{
  GraphWriteBenchmark,
  Neo4jEmbedReadBenchmark,
  OverflowDbReadBenchmark,
  TinkerGraphReadBenchmark
}
import com.github.plume.oss.drivers.{IDriver, TinkerGraphDriver}
import org.cache2k.benchmark.jmh.{HeapProfiler, LinuxVmProfiler}
import org.openjdk.jmh.annotations.Mode
import org.openjdk.jmh.runner.Runner
import org.openjdk.jmh.runner.options.{ChainedOptionsBuilder, OptionsBuilder, TimeValue}
import upickle.default.*

import scala.util.{Try, Failure}

object Benchmark {

  def main(args: Array[String]): Unit = {
    Plume
      .optionParser("plume-benchmark", "A benchmarking suite for graph databases as static analysis backends.")
      .parse(args, PlumeConfig())
      .foreach { config =>
        val writeOptsBenchmark = createOptionsBoilerPlate(config, WRITE)
          .include(classOf[GraphWriteBenchmark].getSimpleName)
          .build()
        new Runner(writeOptsBenchmark).run()
        println(
          s"Finished WRITE JMH benchmarks. Results: ${config.jmhResultFile}-WRITE.csv; Output: ${config.jmhOutputFile}-WRITE.csv"
        )

        val readOptsBenchmark = config.dbConfig match {
          case _: TinkerGraphConfig =>
            Option(
              createOptionsBoilerPlate(config, READ)
                .include(classOf[TinkerGraphReadBenchmark].getSimpleName)
                .build()
            )
          case _: OverflowDbConfig =>
            Option(
              createOptionsBoilerPlate(config, READ)
                .include(classOf[OverflowDbReadBenchmark].getSimpleName)
                .build()
            )
          case _: Neo4jEmbeddedConfig =>
            Option(
              createOptionsBoilerPlate(config, READ)
                .include(classOf[Neo4jEmbedReadBenchmark].getSimpleName)
                .build()
            )
          case x =>
            println(s"Read benchmarks are not available for ${x.getClass.getSimpleName}, skipping...")
            Option.empty
        }
        readOptsBenchmark.foreach { opts =>
          new Runner(opts).run()
          println(
            s"Finished READ JMH benchmarks. Results: ${config.jmhResultFile}-READ.csv; Output: ${config.jmhOutputFile}-READ.csv"
          )
        }

      }
  }

  private def createOptionsBoilerPlate(config: PlumeConfig, benchmarkType: BenchmarkType): ChainedOptionsBuilder = {
    new OptionsBuilder()
      .addProfiler(classOf[HeapProfiler])
      .addProfiler(classOf[LinuxVmProfiler])
      .warmupTime(TimeValue.seconds(30))
      .mode(Mode.AverageTime)
      .forks(1)
      .output(s"${config.jmhOutputFile}-${benchmarkType.toString.toLowerCase}.txt")
      .result(s"${config.jmhResultFile}-${benchmarkType.toString.toLowerCase}.csv")
      .param("configStr", write(config))
      .jvmArgsAppend(s"-Xmx${config.jmhMemoryGb}G", "-XX:+UseZGC", "-XX:+UseStringDeduplication")
  }

  enum BenchmarkType {
    case READ, WRITE
  }

  private def clearExistingStorage(conf: DatabaseConfig): Unit = {
    conf match {
      case OverflowDbConfig(storageLocation, _, _) =>
        File(storageLocation).delete(swallowIOExceptions = true)
      case TinkerGraphConfig(Some(importPath), _) =>
        File(importPath).delete(swallowIOExceptions = true)
      case Neo4jEmbeddedConfig(_, databaseDir, _) =>
        File(databaseDir).delete(swallowIOExceptions = true)
      case _ =>
    }
  }

  def deserializeConfig(configStr: String): PlumeConfig =
    if (!configStr.isBlank) read[PlumeConfig](configStr) else PlumeConfig()

  def initializeDriverAndInputDir(configStr: String, deleteExistingStorage: Boolean = true): (IDriver, PlumeConfig) = {
    val config = deserializeConfig(configStr)
    if (deleteExistingStorage) {
      clearExistingStorage(config.dbConfig)
    }

    val driver = Try(config.dbConfig.toDriver).getOrElse {
      if (deleteExistingStorage) println("Unable to connect driver to existing storage, clearing...")
      else println("Unable to start driver clearing potential existing storage...")

      clearExistingStorage(config.dbConfig)
      config.dbConfig.toDriver
    }
    println(s"Initialized driver ${driver.getClass.getSimpleName}")
    if (!deleteExistingStorage) {
      driver match {
        case tinker: TinkerGraphDriver =>
          Try(config.dbConfig.asInstanceOf[TinkerGraphConfig].exportPath.foreach(tinker.importGraph)) match {
            case Failure(exception) =>
              println("Failed to import existing TinkerGraph, recreating...")
              exception.printStackTrace()
            case _ =>
          }
        case _ =>
      }
    }
    driver -> config
  }

}
