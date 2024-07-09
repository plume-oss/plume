package com.github.plume.oss

import better.files.File
import com.github.plume.oss.Benchmark.BenchmarkType.*
import com.github.plume.oss.drivers.{IDriver, OverflowDbDriver, TinkerGraphDriver}
import io.joern.jimple2cpg.Config
import io.shiftleft.codepropertygraph.generated.Cpg
import io.shiftleft.codepropertygraph.generated.EdgeTypes.AST
import io.shiftleft.codepropertygraph.generated.NodeTypes.{CALL, METHOD}
import io.shiftleft.codepropertygraph.generated.PropertyNames.{FULL_NAME, ORDER}
import io.shiftleft.codepropertygraph.generated.nodes.{Call, StoredNode}
import io.shiftleft.semanticcpg.language.*
import org.apache.tinkerpop.gremlin.process.traversal.P
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.{GraphTraversalSource, __}
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.__.{and, not}
import org.apache.tinkerpop.gremlin.structure.Vertex
import org.cache2k.benchmark.jmh.ForcedGcMemoryProfiler
import org.openjdk.jmh.annotations.{Benchmark, Level, Mode, Param, Scope, Setup, State, TearDown}
import org.openjdk.jmh.infra.{BenchmarkParams, Blackhole}
import org.openjdk.jmh.runner.Runner
import org.openjdk.jmh.runner.options.{ChainedOptionsBuilder, OptionsBuilder, TimeValue}
import overflowdb.PropertyKey
import overflowdb.traversal.jIteratortoTraversal
import upickle.default.*

import java.util
import java.util.concurrent.TimeUnit
import scala.compiletime.uninitialized
import scala.jdk.CollectionConverters.*
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
    cpg.graph.nodes.iterator.collect {
      case node if node.in(AST).isEmpty && node.out(AST).nonEmpty => node.id()
    }.toArray
  }

  override def setupAstUp(): Array[Long] = {
    cpg.graph.nodes.iterator.map(_.id()).toArray
  }

  override def setUpOrderSum(): Array[Long] = {
    cpg.graph.nodes.iterator.filter(n => n.propertiesMap().containsKey(ORDER)).map(_.id()).toArray
  }

  override def setUpCallOrder(): Array[Long] = {
    cpg.graph.nodes.iterator.collect { case node: Call => node.id() }.toArray
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
    Option(blackhole).foreach(_.consume(nnodes))
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
    Option(blackhole).foreach(_.consume(sumDepth))
    sumDepth
  }

  @Benchmark
  override def orderSum(blackhole: Blackhole): Int = {
    var sumOrder = 0
    val propKey  = PropertyKey[Int](ORDER)
    for (node <- nodeStart.map(cpg.graph.node)) {
      sumOrder += node.asInstanceOf[StoredNode].property(propKey)
    }
    Option(blackhole).foreach(_.consume(sumOrder))
    sumOrder
  }

  @Benchmark
  override def callOrderTrav(blackhole: Blackhole): Int = {
    val res = cpg.graph.nodes(nodeStart*).iterator.asInstanceOf[Iterator[Call]].orderGt(2).size
    Option(blackhole).foreach(_.consume(res))
    res
  }

  @Benchmark
  override def callOrderExplicit(blackhole: Blackhole): Int = {
    var res = 0
    for (node <- cpg.graph.nodes(nodeStart*).iterator.asInstanceOf[Iterator[Call]]) {
      if (node.order > 2) res += 1
    }
    Option(blackhole).foreach(_.consume(res))
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

@State(Scope.Benchmark)
class TinkerGraphReadBenchmark extends GraphReadBenchmark {

  private var g: () => GraphTraversalSource = uninitialized

  @Setup
  def setupBenchmark(params: BenchmarkParams): Unit = {
    val (driver_, inputDir_) = Benchmark.initializeDriverAndInputDir(configStr)
    driver = driver_
    inputDir = inputDir_
    g = () => driver.asInstanceOf[TinkerGraphDriver].g()
    setupBenchmarkParams(params)
  }

  override def setupAstDfs(): Array[Long] = {
    g().V().where(__.and(__.not(__.inE(AST)), __.outE(AST))).id().asScala.map(_.asInstanceOf[Long]).toArray
  }

  override def setupAstUp(): Array[Long] = {
    g().V().id().asScala.map(_.asInstanceOf[Long]).toArray
  }

  override def setUpOrderSum(): Array[Long] = {
    g().V().has(ORDER).id().asScala.map(_.asInstanceOf[Long]).toArray
  }

  override def setUpCallOrder(): Array[Long] = {
    g().V().hasLabel(CALL).id().asScala.map(_.asInstanceOf[Long]).toArray
  }

  override def setUpMethodFullName(): Array[String] = {
    fullNames = new Random(1234).shuffle(g().V().hasLabel(METHOD).properties(FULL_NAME).value()).toArray
    fullNames.slice(0, math.min(1000, fullNames.length))
  }

  @Benchmark
  override def astDFS(blackhole: Blackhole): Int = {
    val stack = scala.collection.mutable.ArrayDeque.empty[Long]
    stack.addAll(nodeStart)
    var nnodes = nodeStart.length
    while (stack.nonEmpty) {
      val nx = g().V(stack.removeLast())
      stack.appendAll(nx.out(AST).id().map(_.asInstanceOf[Long]).asScala.toArray)
      nnodes += 1
    }
    Option(blackhole).foreach(_.consume(nnodes))
    nnodes
  }

  @Benchmark
  override def astUp(blackhole: Blackhole): Int = {
    var sumDepth = 0
    for (node <- nodeStart) {
      var p       = g().V(node)
      var hasNext = false
      while (!hasNext) {
        sumDepth += 1
        hasNext = p.in(AST).hasNext
        if (hasNext) {
          p = g().V(p.in(AST).id().asScala.toSeq*)
        }
      }
    }
    Option(blackhole).foreach(_.consume(sumDepth))
    sumDepth
  }

  @Benchmark
  override def orderSum(blackhole: Blackhole): Int = {
    var sumOrder = 0
    for (node <- nodeStart.map(g().V(_))) {
      sumOrder += node.properties(ORDER).value().next().asInstanceOf[Int]
    }
    Option(blackhole).foreach(_.consume(sumOrder))
    sumOrder
  }

  @Benchmark
  override def callOrderTrav(blackhole: Blackhole): Int = {
    val res = g().V(nodeStart*).hasLabel(CALL).has(ORDER, P.gt(2)).size
    Option(blackhole).foreach(_.consume(res))
    res
  }

  @Benchmark
  override def callOrderExplicit(blackhole: Blackhole): Int = {
    var res = 0
    for (node <- g().V(nodeStart*).hasLabel(CALL)) {
      if (node.property(ORDER).asInstanceOf[Int] > 2) res += 1
    }
    Option(blackhole).foreach(_.consume(res))
    res
  }

  @Benchmark
  override def indexedMethodFullName(bh: Blackhole): Unit = {
    fullNames.foreach { fullName =>
      g().V().hasLabel(METHOD).has(FULL_NAME, fullName).foreach(bh.consume)
    }
  }

  @Benchmark
  override def unindexedMethodFullName(bh: Blackhole): Unit = {
    for {
      str   <- fullNames
      found <- g().V().hasLabel(METHOD).where(__.has(FULL_NAME, str))
    } bh.consume(found)
  }
}
