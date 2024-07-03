package com.github.plume.oss

import com.github.plume.oss.Benchmark.BenchmarkType.WRITE
import com.github.plume.oss.drivers.IDriver
import io.joern.jimple2cpg.Config
import org.cache2k.benchmark.jmh.ForcedGcMemoryProfiler
import org.openjdk.jmh.annotations.{Benchmark, Level, Mode, Param, Scope, Setup, State, TearDown}
import org.openjdk.jmh.infra.{BenchmarkParams, Blackhole}
import org.openjdk.jmh.runner.Runner
import org.openjdk.jmh.runner.options.{ChainedOptionsBuilder, OptionsBuilder, TimeValue}
import scopt.OptionParser
import upickle.default.*

import java.util.concurrent.TimeUnit
import scala.compiletime.uninitialized

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

//      val readOptsBenchmark = createOptionsBoilerPlate(config, READ)
//        .include(classOf[OverflowDbBenchmark].getSimpleName)
//        .build()
//      new Runner(readOptsBenchmark).run()
//      println(
//        s"Finished READ JMH benchmarks. Results: ${config.jmhResultFile}-READ.csv; Output: ${config.jmhOutputFile}-READ.csv"
//      )
      }
  }

  private def createOptionsBoilerPlate(config: PlumeConfig, benchmarkType: BenchmarkType): ChainedOptionsBuilder = {
    new OptionsBuilder()
      .addProfiler(classOf[ForcedGcMemoryProfiler])
      .warmupIterations(1)
      .warmupTime(TimeValue.seconds(1))
      .measurementTime(TimeValue.seconds(2))
      .measurementIterations(3)
      .mode(Mode.AverageTime)
      .timeUnit(TimeUnit.NANOSECONDS)
      .forks(2)
      .output(s"${config.jmhOutputFile}-$benchmarkType.txt")
      .result(s"${config.jmhResultFile}-$benchmarkType.csv")
      .param("configStr", write(config))
      .detectJvmArgs() // inherit stuff like max heap size
  }

  enum BenchmarkType {
    case READ, WRITE
  }

}

@State(Scope.Benchmark)
class GraphWriteBenchmark {

  @Param(Array(""))
  var configStr: String = ""
  var config: PlumeConfig =
    if (!configStr.isBlank) read[PlumeConfig](configStr) else PlumeConfig()
  var driver: IDriver = uninitialized

  @Setup
  def setupBenchmark(params: BenchmarkParams): Unit = {
    config = if (!configStr.isBlank) read[PlumeConfig](configStr) else PlumeConfig()
    driver = config.dbConfig.toDriver
  }

  @Setup(Level.Iteration)
  def clearDriver(params: BenchmarkParams): Unit = {
    driver.clear()
  }

  @Benchmark
  def createAst(blackhole: Blackhole): Unit = {
    JimpleAst2Database(driver).createAst(Config().withInputPath(config.inputDir))
    Option(blackhole).foreach(_.consume(driver))
  }

  @TearDown
  def cleanupBenchmark(): Unit = {
    driver.clear()
    driver.close()
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
