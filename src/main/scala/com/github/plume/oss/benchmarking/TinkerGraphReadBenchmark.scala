package com.github.plume.oss.benchmarking

import com.github.plume.oss.drivers.TinkerGraphDriver
import io.shiftleft.codepropertygraph.generated.EdgeTypes.AST
import io.shiftleft.codepropertygraph.generated.NodeTypes.{CALL, METHOD}
import io.shiftleft.codepropertygraph.generated.PropertyNames.{FULL_NAME, ORDER}
import org.apache.tinkerpop.gremlin.process.traversal.P
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.{GraphTraversalSource, __}
import org.openjdk.jmh.annotations.*
import org.openjdk.jmh.infra.{BenchmarkParams, Blackhole}
import overflowdb.traversal.*

import java.util.concurrent.TimeUnit
import scala.collection.mutable
import scala.compiletime.uninitialized
import scala.jdk.CollectionConverters.*
import scala.util.Random

@State(Scope.Benchmark)
@Timeout(5, TimeUnit.MINUTES)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Measurement(iterations = 3, time = 5, timeUnit = TimeUnit.SECONDS)
@Warmup(iterations = 1, time = 1, timeUnit = TimeUnit.SECONDS)
class TinkerGraphReadBenchmark extends GraphReadBenchmark {

  private var g: () => GraphTraversalSource = uninitialized

  @Setup
  override def setupBenchmark(params: BenchmarkParams): Unit = {
    super.setupBenchmark(params)
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
      val nx       = g().V(stack.removeAll()*)
      val children = nx.out(AST).id().toList.asScala.asInstanceOf[mutable.Buffer[Long]].toArray
      nnodes += children.length
      stack.appendAll(children)
    }
    Option(blackhole).foreach(_.consume(nnodes))
    nnodes
  }

  @Benchmark
  override def astUp(blackhole: Blackhole): Int = {
    var sumDepth = 0
    val stack    = scala.collection.mutable.ArrayDeque.empty[Long]
    stack.addAll(nodeStart)
    while (stack.nonEmpty) {
      val ps      = g().V(stack.removeAll()*)
      val parents = ps.out(AST).id().toList.asScala.asInstanceOf[mutable.Buffer[Long]].toArray
      sumDepth += parents.size
      stack.appendAll(parents)
    }
    Option(blackhole).foreach(_.consume(sumDepth))
    sumDepth
  }

  @Benchmark
  override def orderSum(blackhole: Blackhole): Int = {
    var sumOrder = g().V(nodeStart*).properties(ORDER).value().asScala.asInstanceOf[Iterator[Int]].sum

//    for (node <- g().V(nodeStart*).properties(ORDER)) {
//      sumOrder += g().V(node).properties(ORDER).value().next().asInstanceOf[Int]
//    }
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
      if (node.property(ORDER).value().asInstanceOf[Int] > 2) res += 1
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
