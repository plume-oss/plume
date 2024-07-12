package com.github.plume.oss.benchmarking

import com.github.plume.oss.benchmarking.GraphReadBenchmark
import com.github.plume.oss.drivers.{Neo4jEmbeddedDriver, TinkerGraphDriver}
import io.shiftleft.codepropertygraph.generated.EdgeTypes.AST
import io.shiftleft.codepropertygraph.generated.NodeTypes.{CALL, METHOD}
import io.shiftleft.codepropertygraph.generated.PropertyNames.{FULL_NAME, ORDER}
import org.apache.tinkerpop.gremlin.process.traversal.P
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.{GraphTraversalSource, __}
import org.neo4j.graphdb.{GraphDatabaseService, Label}
import org.openjdk.jmh.annotations.{Benchmark, Scope, Setup, State}
import org.openjdk.jmh.infra.{BenchmarkParams, Blackhole}
import overflowdb.traversal.*

import scala.compiletime.uninitialized
import scala.jdk.CollectionConverters.*
import scala.util.{Random, Using}

@State(Scope.Benchmark)
class Neo4jEmbedReadBenchmark extends GraphReadBenchmark {

  private var g: GraphDatabaseService = uninitialized

  @Setup
  override def setupBenchmark(params: BenchmarkParams): Unit = {
    super.setupBenchmark(params)
    g = driver.asInstanceOf[Neo4jEmbeddedDriver].graph
    setupBenchmarkParams(params)
  }

  override def setupAstDfs(): Array[Long] = {
    Using.resource(g.beginTx) { tx =>
      tx.execute(s"""
           |MATCH (n)-[$AST]->()
           |WHERE NOT (n)<-[$AST]-()
           |RETURN n.id
           |""".stripMargin)
        .map { result => result.get("n.id").asInstanceOf[Long] }
        .toArray
    }
  }

  override def setupAstUp(): Array[Long] = {
    Using.resource(g.beginTx) { tx =>
      tx.execute(s"""
            |MATCH (n)-[$AST]->()
            |RETURN n.id
            |""".stripMargin)
        .map { result => result.get("n.id").asInstanceOf[Long] }
        .toArray
    }
  }

  override def setUpOrderSum(): Array[Long] = {
    Using.resource(g.beginTx) { tx =>
      tx.execute(s"""
            |MATCH (n)
            |WHERE n.$ORDER IS NOT NULL
            |RETURN n.id
            |""".stripMargin)
        .map { result => result.get("n.id").asInstanceOf[Long] }
        .toArray
    }
  }

  override def setUpCallOrder(): Array[Long] = {
    Using.resource(g.beginTx) { tx =>
      val res = tx
        .execute(s"""
            |MATCH (n: $CALL)
            |WHERE n.$ORDER IS NOT NULL
            |RETURN n.id
            |""".stripMargin)
        .map { result => result.get("n.id").asInstanceOf[Long] }
        .toList
      println(res)
      res.toArray
    }
  }

  override def setUpMethodFullName(): Array[String] = {
    val fullNames_ = Using.resource(g.beginTx) { tx =>
      tx.execute(s"""
            |MATCH (n: $METHOD)
            |WHERE n.$FULL_NAME IS NOT NULL
            |RETURN n.$FULL_NAME
            |""".stripMargin)
        .map { result => result.get(s"n.$FULL_NAME").asInstanceOf[String] }
        .toArray
    }
    fullNames = new Random(1234).shuffle(fullNames_).toArray
    fullNames.slice(0, math.min(1000, fullNames.length))
  }

  @Benchmark
  override def astDFS(blackhole: Blackhole): Int = {
    val stack = scala.collection.mutable.ArrayDeque.empty[Long]
    stack.addAll(nodeStart)
    var nnodes = nodeStart.length
    while (stack.nonEmpty) {
      val childrenIds = Using.resource(g.beginTx) { tx =>
        tx.execute(s"""
               |MATCH (n)-[AST]->(m)
               |WHERE n.id = ${stack.removeLast()}
               |RETURN m.id
               |""".stripMargin)
          .map { result => result.get("m.id").asInstanceOf[Long] }
          .toArray
      }
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
      var nodeId = node
      def getResult = Using.resource(g.beginTx) { tx =>
        tx.execute(s"""
               |MATCH (n)<-[AST]-(m)
               |WHERE n.id = $nodeId
               |RETURN m.id
               |""".stripMargin)
          .map { result => result.get("m.id").asInstanceOf[Long] }
          .toArray
      }
      var result  = getResult
      def hasNext = result.nonEmpty
      while (hasNext) {
        sumDepth += 1
        nodeId = result.head
        result = getResult
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
