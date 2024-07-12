package com.github.plume.oss.benchmarking

import com.github.plume.oss.benchmarking.GraphReadBenchmark
import com.github.plume.oss.drivers.OverflowDbDriver
import io.shiftleft.codepropertygraph.generated.Cpg
import io.shiftleft.codepropertygraph.generated.EdgeTypes.AST
import io.shiftleft.codepropertygraph.generated.PropertyNames.ORDER
import io.shiftleft.codepropertygraph.generated.nodes.{Call, StoredNode}
import org.openjdk.jmh.annotations.{Benchmark, Scope, Setup, State}
import org.openjdk.jmh.infra.{BenchmarkParams, Blackhole}
import overflowdb.PropertyKey
import overflowdb.traversal.*
import scala.compiletime.uninitialized
import scala.util.Random
import io.shiftleft.semanticcpg.language.*

@State(Scope.Benchmark)
class OverflowDbReadBenchmark extends GraphReadBenchmark {

  private var cpg: Cpg = uninitialized

  @Setup
  override def setupBenchmark(params: BenchmarkParams): Unit = {
    super.setupBenchmark(params)
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
