package com.github.plume.oss.benchmarking

import better.files.File
import com.github.plume.oss
import com.github.plume.oss.Benchmark.deserializeConfig
import com.github.plume.oss.drivers.IDriver
import com.github.plume.oss.{Benchmark, JimpleAst2Database, PlumeConfig, TinkerGraphConfig}
import io.joern.jimple2cpg.Config
import io.shiftleft.codepropertygraph.generated.{NodeTypes, PropertyNames}
import org.openjdk.jmh.annotations.*
import org.openjdk.jmh.infra.{BenchmarkParams, Blackhole}

import java.util.concurrent.TimeUnit
import scala.compiletime.uninitialized

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
    val config       = deserializeConfig(configStr)
    val (driver_, _) = oss.Benchmark.initializeDriverAndInputDir(configStr, deleteExistingStorage = false)
    driver = driver_
    val createAst = config.dbConfig match {
      case TinkerGraphConfig(_, Some(path)) => !File(path).exists()
      case oss.OverflowDbConfig(path, _, _) => !File(path).exists()
      case oss.Neo4jEmbeddedConfig(_, path, _) =>
        if !File(path).exists() then true else driver.propertyFromNodes(NodeTypes.FILE, PropertyNames.NAME).isEmpty
      case _ => true
    }
    if (createAst) JimpleAst2Database(driver).createAst(Config().withInputPath(config.inputDir))
  }

  protected def setupAstDfs(): Array[Long]

  protected def setupAstUp(): Array[Long]

  protected def setUpOrderSum(): Array[Long]

  protected def setUpCallOrder(): Array[Long]

  protected def setUpMethodFullName(): Array[String]

  @Benchmark
  @Measurement(time = 5, timeUnit = TimeUnit.SECONDS)
  def astDFS(blackhole: Blackhole): Int

  @Benchmark
  @Measurement(time = 5, timeUnit = TimeUnit.SECONDS)
  def astUp(blackhole: Blackhole): Int

  @Benchmark
  @Measurement(time = 5, timeUnit = TimeUnit.SECONDS)
  def orderSum(blackhole: Blackhole): Int

  @Benchmark
  @Measurement(time = 5, timeUnit = TimeUnit.SECONDS)
  def callOrderTrav(blackhole: Blackhole): Int

  @Benchmark
  @Measurement(time = 5, timeUnit = TimeUnit.SECONDS)
  def callOrderExplicit(blackhole: Blackhole): Int

  @Benchmark
  @Measurement(time = 5, timeUnit = TimeUnit.SECONDS)
  def indexedMethodFullName(bh: Blackhole): Unit

  @Benchmark
  @Measurement(time = 5, timeUnit = TimeUnit.SECONDS)
  def unindexedMethodFullName(bh: Blackhole): Unit

  @TearDown
  def cleanupBenchmark(): Unit = {
    driver.close()
  }

  @TearDown(Level.Iteration)
  def teardown(): Unit = {
    System.gc()
  }

}
