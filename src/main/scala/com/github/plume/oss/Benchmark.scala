package com.github.plume.oss

import better.files.File
import com.github.plume.oss.Benchmark.BenchmarkType.{READ, WRITE}
import com.github.plume.oss.Benchmark.PlumeBenchmarkConfig
import com.github.plume.oss.drivers.{IDriver, Neo4jDriver, OverflowDbDriver, TinkerGraphDriver}
import io.joern.jimple2cpg.Config
import org.openjdk.jmh.annotations.{Benchmark, Mode, Scope, Setup, State}
import org.openjdk.jmh.infra.{BenchmarkParams, Blackhole}
import org.openjdk.jmh.profile.GCProfiler
import org.openjdk.jmh.runner.Runner
import org.openjdk.jmh.runner.options.{ChainedOptionsBuilder, OptionsBuilder, TimeValue}
import scopt.OptionParser

import java.util.concurrent.TimeUnit
import scala.compiletime.uninitialized

object Benchmark {

  def main(args: Array[String]): Unit = {
    optionParser.parse(args, PlumeBenchmarkConfig()).foreach { config =>
      val writeOptsBenchmark = createOptionsBoilerPlate(config, WRITE)
        .include(classOf[GraphWriteBenchmark].getSimpleName)
        .build()
      new Runner(writeOptsBenchmark).run()
      println(
        s"Finished WRITE JMH benchmarks. Results: ${config.jmhResultFile}-WRITE.csv; Output: ${config.jmhOutputFile}-WRITE.csv"
      )

//      val readOptsBenchmark = createOptionsBoilerPlate(config, READ)
//        .include(classOf[OverflowDbBenchmark].getSimpleName)
//        .build()
//      new Runner(readOptsBenchmark).run()
//      println(
//        s"Finished READ JMH benchmarks. Results: ${config.jmhResultFile}-READ.csv; Output: ${config.jmhOutputFile}-READ.csv"
//      )
    }
  }

  private def createOptionsBoilerPlate(
    config: PlumeBenchmarkConfig,
    benchmarkType: BenchmarkType
  ): ChainedOptionsBuilder = {
    new OptionsBuilder()
      .addProfiler(classOf[GCProfiler])
      .warmupIterations(1)
      .warmupTime(TimeValue.seconds(1))
      .measurementTime(TimeValue.seconds(2))
      .measurementIterations(3)
      .mode(Mode.AverageTime)
      .timeUnit(TimeUnit.NANOSECONDS)
      .forks(2)
      .output(s"${config.jmhOutputFile}-$benchmarkType.csv")
      .result(s"${config.jmhResultFile}-$benchmarkType.csv")
      .detectJvmArgs() // inherit stuff like max heap size
  }

  enum BenchmarkType {
    case READ, WRITE
  }

  private val optionParser: OptionParser[PlumeBenchmarkConfig] =
    new OptionParser[PlumeBenchmarkConfig]("plume-benchmark") {

      head("plume-benchmark")

      note("A benchmarking suite for graph databases as static analysis backends.")
      help('h', "help")

      arg[String]("input-dir")
        .text("The target application to parse and evaluate against.")
        .action((x, c) => c.copy(inputDir = x))

      opt[String]('o', "jmh-output-file")
        .text(s"The JMH output file path. Exclude file extensions.")
        .action((x, c) => c.copy(jmhOutputFile = x))

      opt[String]('r', "jmh-result-file")
        .text(s"The result file path. Exclude file extensions.")
        .action((x, c) => c.copy(jmhResultFile = x))

      cmd("tinkergraph")
        .action((_, c) => c.copy(dbConfig = TinkerGraphConfig()))

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

    }

  case class PlumeBenchmarkConfig(
    inputDir: String = "",
    jmhOutputFile: String = File.newTemporaryFile("plume-jmh-output").pathAsString,
    jmhResultFile: String = File.newTemporaryFile("plume-jmh-result").pathAsString,
    dbConfig: DatabaseConfig = OverflowDbConfig()
  )

}

class GraphWriteBenchmark(config: PlumeBenchmarkConfig) {

  var driver: IDriver = uninitialized

  @Setup
  def setupFun(params: BenchmarkParams): Unit = {
    driver = config.dbConfig.toDriver
  }

  @Benchmark
  def createAst(blackhole: Blackhole): Unit = {
    JimpleAst2Database(driver).createAst(Config().withInputPath(config.inputDir))
    Option(blackhole).foreach(_.consume(driver))
  }

}

sealed trait GraphReadBenchmark[D <: IDriver](protected val driver: D) {

  private var nodeStart: Array[Long]   = new Array[Long](0)
  private var fullNames: Array[String] = uninitialized

  @Setup
  def setupFun(params: BenchmarkParams): Unit = {
    params.getBenchmark
  }

  @Benchmark
  def astDFS(blackhole: Blackhole): Int

  @Benchmark
  def astUp(blackhole: Blackhole): Int

  @Benchmark
  def orderSumChecked(blackhole: Blackhole): Int

  @Benchmark
  def orderSumUnchecked(blackhole: Blackhole): Int

  @Benchmark
  def orderSumExplicit(blackhole: Blackhole): Int

  @Benchmark
  def callOrderTrav(blackhole: Blackhole): Int

  @Benchmark
  def callOrderExplicit(blackhole: Blackhole): Int

  @Benchmark
  def indexedMethodFullName(bh: Blackhole): Unit

  @Benchmark
  def unindexedMethodFullName(bh: Blackhole): Unit

}

//@State(Scope.Benchmark)
//class OverflowDbBenchmark(config: OverflowDbConfig)
//    extends GraphReadBenchmark(
//    ) {
//
//  override def createAst(blackhole: Blackhole): Int = {
//    0
//  }
//
//  override def astDFS(blackhole: Blackhole): Int = ???
//
//  override def astUp(blackhole: Blackhole): Int = ???
//
//  override def orderSumChecked(blackhole: Blackhole): Int = ???
//
//  override def orderSumUnchecked(blackhole: Blackhole): Int = ???
//
//  override def orderSumExplicit(blackhole: Blackhole): Int = ???
//
//  override def callOrderTrav(blackhole: Blackhole): Int = ???
//
//  override def callOrderExplicit(blackhole: Blackhole): Int = ???
//
//  override def indexedMethodFullName(bh: Blackhole): Unit = ???
//
//  override def unindexedMethodFullName(bh: Blackhole): Unit = ???
//}
