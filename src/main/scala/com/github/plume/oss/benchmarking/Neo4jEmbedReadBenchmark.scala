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
      stack.appendAll(childrenIds)
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
    for (nodeId <- nodeStart) {
      val orderArr = Using.resource(g.beginTx) { tx =>
        tx.execute(s"""
               |MATCH (n)
               |WHERE n.id = $nodeId
               |RETURN n.$ORDER
               |""".stripMargin)
          .map { result => result.get(s"n.$ORDER").asInstanceOf[Int] }
          .toArray
      }
      sumOrder += orderArr.head
    }
    Option(blackhole).foreach(_.consume(sumOrder))
    sumOrder
  }

  @Benchmark
  override def callOrderTrav(blackhole: Blackhole): Int = {
    val res = Using.resource(g.beginTx) { tx =>
      tx.execute(s"""
             |MATCH (n: $CALL)
             |WHERE n.$ORDER > 2 AND n.id IN [${nodeStart.mkString(",")}]
             |RETURN COUNT(n)
             |""".stripMargin)
        .map(_.get("COUNT(n)").asInstanceOf[Int])
        .next()
    }
    Option(blackhole).foreach(_.consume(res))
    res
  }

  @Benchmark
  override def callOrderExplicit(blackhole: Blackhole): Int = {
    var res = 0
    val nodes = Using.resource(g.beginTx) { tx =>
      tx.execute(s"""
             |MATCH (n: $CALL)
             |WHERE n.id IN [${nodeStart.mkString(",")}]
             |RETURN n.$ORDER
             |""".stripMargin)
        .map(_.get(s"n.$ORDER").asInstanceOf[Int])
        .toArray
    }
    for (order <- nodes) {
      if (order > 2) res += 1
    }
    Option(blackhole).foreach(_.consume(res))
    res
  }

  @Benchmark
  override def indexedMethodFullName(bh: Blackhole): Unit = {
    fullNames.foreach { fullName =>
      Using
        .resource(g.beginTx) { tx =>
          tx.execute(s"""
               |MATCH (n: $METHOD)
               |WHERE n.$FULL_NAME = $fullName
               |RETURN n
               |""".stripMargin)
            .map(_.get(s"n"))
            .toArray
        }
        .foreach(bh.consume)
    }
  }

  @Benchmark
  override def unindexedMethodFullName(bh: Blackhole): Unit = {
    fullNames.foreach { fullName =>
      Using
        .resource(g.beginTx) { tx =>
          tx.execute(s"""
               |MATCH (n)
               |WHERE n.$FULL_NAME = $fullName and $METHOD IN labels(n)
               |RETURN n
               |""".stripMargin)
            .map(_.get(s"n"))
            .toArray
        }
        .foreach(bh.consume)
    }
  }
}
