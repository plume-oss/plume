package com.github.plume.oss.benchmarking

import com.github.plume.oss
import com.github.plume.oss.drivers.IDriver
import com.github.plume.oss.{Benchmark, JimpleAst2Database}
import io.joern.jimple2cpg.Config
import org.openjdk.jmh.annotations.*
import org.openjdk.jmh.infra.{BenchmarkParams, Blackhole}

import java.util.concurrent.TimeUnit
import scala.compiletime.uninitialized

@State(Scope.Benchmark)
@Timeout(6, TimeUnit.MINUTES)
@OutputTimeUnit(TimeUnit.SECONDS)
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
  @Measurement(time = 10, timeUnit = TimeUnit.SECONDS)
  def createAst(blackhole: Blackhole): Unit = try {
    JimpleAst2Database(driver).createAst(Config().withInputPath(inputDir))
    Option(blackhole).foreach(_.consume(driver))
  } catch {
    case e: Throwable => Option(blackhole).foreach(_.consume(e))
  }

  @TearDown
  def cleanupBenchmark(): Unit = {
    driver.clear()
    driver.close()
  }

  @TearDown(Level.Iteration)
  def teardown(): Unit = {
    System.gc()
  }

}
