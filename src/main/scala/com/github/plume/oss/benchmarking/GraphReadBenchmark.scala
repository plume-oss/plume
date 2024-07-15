package com.github.plume.oss.benchmarking

import com.github.plume.oss
import com.github.plume.oss.{Benchmark, JimpleAst2Database, PlumeConfig, TinkerGraphConfig}
import com.github.plume.oss.drivers.{IDriver, TinkerGraphDriver}
import io.joern.jimple2cpg.Config
import org.openjdk.jmh.annotations.{Benchmark, Level, Param, Scope, Setup, State, TearDown, Timeout}
import org.openjdk.jmh.infra.{BenchmarkParams, Blackhole}

import java.util.concurrent.TimeUnit
import scala.compiletime.uninitialized

@State(Scope.Benchmark)
@Timeout(2, TimeUnit.MINUTES)
trait GraphReadBenchmark {

  @Param(Array(""))
  protected var configStr: String        = ""
  protected var config: PlumeConfig      = uninitialized
  protected var nodeStart: Array[Long]   = new Array[Long](0)
  protected var fullNames: Array[String] = uninitialized
  protected var driver: IDriver          = uninitialized

  protected def setupBenchmarkParams(params: BenchmarkParams): Unit = {
    params.getBenchmark match {
      case name if name.endsWith("astDFS") =>
        nodeStart = setupAstDfs()
      case name if name.endsWith("astUp") =>
        nodeStart = setupAstUp()
      case name if name.contains("orderSum") =>
        nodeStart = setUpOrderSum()
      case name if name.contains("callOrder") =>
        nodeStart = setUpCallOrder()
      case name if name.contains("MethodFullName") =>
        fullNames = setUpMethodFullName()
    }
  }

  protected def setupBenchmark(params: BenchmarkParams): Unit = {
    val (driver_, config_) = oss.Benchmark.initializeDriverAndInputDir(configStr, useCachedGraph = true)
    driver = driver_
    config = config_
    if (!driver.exists(1L)) {
      JimpleAst2Database(driver).createAst(Config().withInputPath(config_.inputDir))
      config.dbConfig match {
        case TinkerGraphConfig(_, Some(exportPath)) => driver.asInstanceOf[TinkerGraphDriver].exportGraph(exportPath)
        case _                                      =>
      }
    }
  }

  protected def setupAstDfs(): Array[Long]

  protected def setupAstUp(): Array[Long]

  protected def setUpOrderSum(): Array[Long]

  protected def setUpCallOrder(): Array[Long]

  protected def setUpMethodFullName(): Array[String]

  @Benchmark
  def astDFS(blackhole: Blackhole): Int

  @Benchmark
  def astUp(blackhole: Blackhole): Int

  @Benchmark
  def orderSum(blackhole: Blackhole): Int

  @Benchmark
  def callOrderTrav(blackhole: Blackhole): Int

  @Benchmark
  def callOrderExplicit(blackhole: Blackhole): Int

  @Benchmark
  def indexedMethodFullName(bh: Blackhole): Unit

  @Benchmark
  def unindexedMethodFullName(bh: Blackhole): Unit

  @TearDown
  def cleanupBenchmark(): Unit = {
    driver.close()
  }

}
