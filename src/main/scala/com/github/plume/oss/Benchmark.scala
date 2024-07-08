package com.github.plume.oss

import better.files.File
import com.github.plume.oss.Benchmark.BenchmarkType.*
import com.github.plume.oss.Benchmark.setOps
import com.github.plume.oss.drivers.{IDriver, OverflowDbDriver}
import io.joern.jimple2cpg.Config
import io.shiftleft.codepropertygraph.generated.Cpg
import io.shiftleft.codepropertygraph.generated.nodes.{AstNode, Call, StoredNode}
import io.shiftleft.semanticcpg.language.*
import org.cache2k.benchmark.jmh.ForcedGcMemoryProfiler
import org.openjdk.jmh.annotations.{Benchmark, Level, Mode, Param, Scope, Setup, State, TearDown}
import org.openjdk.jmh.infra.{BenchmarkParams, Blackhole}
import org.openjdk.jmh.runner.Runner
import org.openjdk.jmh.runner.options.{ChainedOptionsBuilder, OptionsBuilder, TimeValue}
import overflowdb.PropertyKey
import overflowdb.traversal.{jIteratortoTraversal, toNodeOps}
import upickle.default.*

import java.util.concurrent.TimeUnit
import scala.compiletime.uninitialized
import scala.util.Random

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
                .include(classOf[OverflowDbReadBenchmark].getSimpleName)
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
                .include(classOf[OverflowDbReadBenchmark].getSimpleName)
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
      .addProfiler(classOf[ForcedGcMemoryProfiler])
      .warmupIterations(1)
      .warmupTime(TimeValue.seconds(1))
      .measurementTime(TimeValue.minutes(1))
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

  def initializeDriverAndInputDir(configStr: String): (IDriver, String) = {
    val config = if (!configStr.isBlank) read[PlumeConfig](configStr) else PlumeConfig()
    config.dbConfig match {
      case OverflowDbConfig(storageLocation, _, _) => File(storageLocation).delete(swallowIOExceptions = true)
      case TinkerGraphConfig(Some(importPath), _)  => File(importPath).delete(swallowIOExceptions = true)
      case Neo4jEmbeddedConfig(_, databaseDir, _)  => File(databaseDir).delete(swallowIOExceptions = true)
      case _                                       =>
    }
    config.dbConfig.toDriver -> config.inputDir
  }

  def setOps(params: BenchmarkParams, ops: Int): Unit = {
    var field: java.lang.reflect.Field = null
    var clazz: Class[?]                = params.getClass
    while (field == null) {
      field = clazz.getDeclaredFields.find(_.getName == "opsPerInvocation").orNull
      clazz = clazz.getSuperclass
    }
    field.setAccessible(true)
    field.setInt(params, ops)
  }

}

@State(Scope.Benchmark)
class GraphWriteBenchmark {

  @Param(Array(""))
  var configStr: String        = ""
  private var driver: IDriver  = uninitialized
  private var inputDir: String = uninitialized

  @Setup
  def setupBenchmark(params: BenchmarkParams): Unit = {
    val (driver_, inputDir_) = Benchmark.initializeDriverAndInputDir(configStr)
    driver = driver_
    inputDir = inputDir_
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

@State(Scope.Benchmark)
sealed trait GraphReadBenchmark {

  @Param(Array(""))
  protected var configStr: String        = ""
  protected var nodeStart: Array[Long]   = new Array[Long](0)
  protected var fullNames: Array[String] = uninitialized
  protected var driver: IDriver          = uninitialized
  protected var inputDir: String         = uninitialized

  protected def setupBenchmarkParams(params: BenchmarkParams): Unit = {
    params.getBenchmark match {
      case name if name.endsWith("astDFS") =>
        nodeStart = setupAstDfs()
        setOps(params, astDFS(null))
      case name if name.endsWith("astUp") =>
        nodeStart = setupAstUp()
        setOps(params, astUp(null))
      case name if name.contains("orderSum") =>
        nodeStart = setUpOrderSum()
        setOps(params, nodeStart.length)
      case name if name.contains("callOrder") =>
        nodeStart = setUpCallOrder()
        setOps(params, nodeStart.length)
      case name if name.contains("MethodFullName") =>
        fullNames = setUpMethodFullName()
        setOps(params, fullNames.length)
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
  def orderSumChecked(blackhole: Blackhole): Int

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

  @Setup(Level.Iteration)
  def clearDriver(params: BenchmarkParams): Unit = {
    driver.clear()
  }

  @TearDown
  def cleanupBenchmark(): Unit = {
    driver.clear()
    driver.close()
  }

}

@State(Scope.Benchmark)
class OverflowDbReadBenchmark extends GraphReadBenchmark {

  private var cpg: Cpg = uninitialized

  @Setup
  def setupBenchmark(params: BenchmarkParams): Unit = {
    val (driver_, inputDir_) = Benchmark.initializeDriverAndInputDir(configStr)
    driver = driver_
    inputDir = inputDir_
    cpg = driver.asInstanceOf[OverflowDbDriver].cpg
    setupBenchmarkParams(params)
  }

  override def setupAstDfs(): Array[Long] = {
    cpg.graph.nodes.iterator.flatMap { nodesOfKind =>
      nodesOfKind.iterator.collect {
        case astNode: StoredNode if astNode._astIn.isEmpty && astNode._astOut.nonEmpty => astNode.id()
      }
    }.toArray
  }

  override def setupAstUp(): Array[Long] = {
    cpg.graph.nodes.iterator.flatMap {
      _.iterator.asInstanceOf[Iterator[StoredNode]].map(_.id)
    }.toArray
  }

  override def setUpOrderSum(): Array[Long] = {
    cpg.graph.nodes.iterator.flatMap { nodesOfKind =>
      nodesOfKind.iterator.collect { case astNode: AstNode =>
        astNode.asInstanceOf[StoredNode].id()
      }
    }.toArray
  }

  override def setUpCallOrder(): Array[Long] = {
    cpg.graph.nodes.iterator.flatMap { nodesOfKind =>
      nodesOfKind.iterator.collect { case node: Call =>
        node.asInstanceOf[StoredNode].id()
      }
    }.toArray
  }

  override def setUpMethodFullName(): Array[String] = {
    fullNames = new Random(1234).shuffle(cpg.method.fullName.iterator).toArray
    fullNames.slice(0, math.min(1000, fullNames.length))
  }

  @Benchmark
  override def astDFS(blackhole: Blackhole): Int = {
    val stack = scala.collection.mutable.ArrayDeque.empty[Long]
    stack.addAll(nodeStart)
    var nnodes = nodeStart.length
    while (stack.nonEmpty) {
      val nx = cpg.graph.node(stack.removeLast()).asInstanceOf[StoredNode]
      stack.appendAll(nx._astOut.map(_.id))
      nnodes += 1
    }
    if (blackhole != null) blackhole.consume(nnodes)
    nnodes
  }

  @Benchmark
  override def astUp(blackhole: Blackhole): Int = {
    var sumDepth = 0
    for (node <- nodeStart) {
      var p = cpg.graph.node(node)
      while (p != null) {
        sumDepth += 1
        p = p.asInstanceOf[StoredNode]._astIn.nextOption.orNull
      }
    }
    if (blackhole != null) blackhole.consume(sumDepth)
    sumDepth
  }

  @Benchmark
  override def orderSumChecked(blackhole: Blackhole): Int = {
    var sumOrder = 0
    for (node <- nodeStart.iterator.asInstanceOf[Iterator[AstNode]]) {
      // we use a checked cast to ensure that our node is an AST-node (i.e. implements the AstNode interface)
      sumOrder += node.order
    }
    if (blackhole != null) blackhole.consume(sumOrder)
    sumOrder
  }

  @Benchmark
  override def orderSumExplicit(blackhole: Blackhole): Int = {
    var sumOrder = 0
    val propKey  = PropertyKey[Int]("ORDER")
    for (node <- nodeStart.map(cpg.graph.node)) {
      sumOrder += node.asInstanceOf[StoredNode].property(propKey)
    }
    if (blackhole != null) blackhole.consume(sumOrder)
    sumOrder
  }

  @Benchmark
  override def callOrderTrav(blackhole: Blackhole): Int = {
    val res = nodeStart.map(cpg.graph.node).iterator.asInstanceOf[Iterator[Call]].orderGt(2).size
    if (blackhole != null) blackhole.consume(res)
    res
  }

  @Benchmark
  override def callOrderExplicit(blackhole: Blackhole): Int = {
    var res = 0
    for (node <- nodeStart.map(cpg.graph.node).iterator.asInstanceOf[Iterator[Call]]) {
      if (node.order > 2) res += 1
    }
    if (blackhole != null) blackhole.consume(res)
    res
  }

  @Benchmark
  override def indexedMethodFullName(bh: Blackhole): Unit = {
    fullNames.foreach { fullName =>
      cpg.method.fullNameExact(fullName).foreach(bh.consume)
    }
  }

  @Benchmark
  override def unindexedMethodFullName(bh: Blackhole): Unit = {
    for {
      str   <- fullNames
      found <- cpg.method.filter { _ => true }.fullNameExact(str)
    } bh.consume(found)
  }
}
