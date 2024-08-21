package com.github.plume.oss.benchmarking

import com.github.plume.oss
import com.github.plume.oss.drivers.{IDriver, TinkerGraphDriver}
import com.github.plume.oss.{Benchmark, JimpleAst2Database, PlumeConfig, TinkerGraphConfig}
import io.joern.jimple2cpg.Config
import org.openjdk.jmh.annotations.*
import org.openjdk.jmh.infra.{BenchmarkParams, Blackhole}

import java.util.concurrent.TimeUnit
import scala.compiletime.uninitialized

@State(Scope.Benchmark)
@Timeout(6, TimeUnit.MINUTES)
@OutputTimeUnit(TimeUnit.SECONDS)
@Measurement(iterations = 10, time = 2, timeUnit = TimeUnit.MINUTES)
@Warmup(iterations = 1, time = 5, timeUnit = TimeUnit.SECONDS)
class GraphWriteBenchmark {

  @Param(Array(""))
  var configStr: String           = ""
  private var config: PlumeConfig = uninitialized
  private var driver: IDriver     = uninitialized
  private var inputDir: String    = uninitialized

  @Setup
  def setupBenchmark(params: BenchmarkParams): Unit = {
    val (driver_, config_) = oss.Benchmark.initializeDriverAndInputDir(configStr)
    driver = driver_
    config = config_
    inputDir = config.inputDir
  }

  @Setup(Level.Iteration)
  def clearDriver(params: BenchmarkParams): Unit = {
    driver.clear()
  }

  @Benchmark
  @Measurement(time = 10, timeUnit = TimeUnit.SECONDS)
  def createAst(blackhole: Blackhole): Unit = try {
    JimpleAst2Database(driver).createAst(Config().withInputPath(inputDir))
    Option(blackhole).foreach(_.consume(driver))
  } catch {
    case e: Throwable => Option(blackhole).foreach(_.consume(e))
  }

  @TearDown
  def cleanupBenchmark(): Unit = {
    driver match {
      case x: TinkerGraphDriver =>
        config.dbConfig.asInstanceOf[TinkerGraphConfig].exportPath.foreach { path =>
          x.exportGraph(path)
        }
      case _ =>
    }
    driver.close()
  }

  @TearDown(Level.Iteration)
  def teardown(): Unit = {
    System.gc()
  }

}
