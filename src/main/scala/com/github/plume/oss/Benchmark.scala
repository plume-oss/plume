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
      .jvmArgsAppend(s"-Xmx${config.jmhMemoryGb}G", "-XX:+UseZGC")
  }

  enum BenchmarkType {
    case READ, WRITE
  }

  def initializeDriverAndInputDir(configStr: String, useCachedGraph: Boolean): (IDriver, PlumeConfig) = {
    val config = if (!configStr.isBlank) read[PlumeConfig](configStr) else PlumeConfig()
    if (!useCachedGraph) {
      config.dbConfig match {
        case OverflowDbConfig(storageLocation, _, _) if !useCachedGraph =>
          File(storageLocation).delete(swallowIOExceptions = true)
        case TinkerGraphConfig(Some(importPath), _) if !useCachedGraph =>
          File(importPath).delete(swallowIOExceptions = true)
        case Neo4jEmbeddedConfig(_, databaseDir, _) if !useCachedGraph =>
          File(databaseDir).delete(swallowIOExceptions = true)
        case _ =>
      }
    }

    val driver = if (useCachedGraph) {
      config.dbConfig match {
        case TinkerGraphConfig(Some(importPath), _) if File(importPath).exists =>
          val driver = config.dbConfig.toDriver.asInstanceOf[TinkerGraphDriver]
          driver.importGraph(importPath)
          driver
        case _ => config.dbConfig.toDriver
      }
    } else {
      config.dbConfig.toDriver
    }

    driver -> config
  }

}
