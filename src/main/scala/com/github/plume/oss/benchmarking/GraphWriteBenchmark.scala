package com.github.plume.oss.benchmarking

import com.github.plume.oss
import com.github.plume.oss.{Benchmark, JimpleAst2Database}
import com.github.plume.oss.drivers.IDriver
import io.joern.jimple2cpg.Config
import org.openjdk.jmh.annotations.{Benchmark, Level, Param, Scope, Setup, State, TearDown, Timeout}
import org.openjdk.jmh.infra.{BenchmarkParams, Blackhole}

import java.util.concurrent.TimeUnit
import scala.compiletime.uninitialized

@State(Scope.Benchmark)
@Timeout(5, TimeUnit.MINUTES)
class GraphWriteBenchmark {

  @Param(Array(""))
  var configStr: String        = ""
  private var driver: IDriver  = uninitialized
  private var inputDir: String = uninitialized

  @Setup
  def setupBenchmark(params: BenchmarkParams): Unit = {
    val (driver_, config) = oss.Benchmark.initializeDriverAndInputDir(configStr, useCachedGraph = false)
    driver = driver_
    inputDir = config.inputDir
  }

  @Setup(Level.Iteration)
  def clearDriver(params: BenchmarkParams): Unit = {
    driver.clear()
  }

  @Benchmark
  def createAst(blackhole: Blackhole): Unit = {
    JimpleAst2Database(driver).createAst(Config().withInputPath(inputDir))
    Option(blackhole).foreach(_.consume(driver))
  }

  @TearDown
  def cleanupBenchmark(): Unit = {
    driver.clear()
    driver.close()
  }

}
