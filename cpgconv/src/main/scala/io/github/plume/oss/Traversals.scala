package io.github.plume.oss

import io.shiftleft.codepropertygraph.Cpg
import io.shiftleft.codepropertygraph.generated.EdgeTypes
import io.shiftleft.codepropertygraph.generated.nodes.{AstNode, MetaData, StoredNode}
import io.shiftleft.dataflowengineoss.passes.reachingdef.{ReachingDefProblem, ReachingDefTransferFunction}
import io.shiftleft.semanticcpg.language._
import overflowdb.{Edge, Graph}

import java.util
import scala.jdk.CollectionConverters._

object Traversals {

  def deleteMethod(graph: Graph, fullName: String): Unit = {
    val nodesToDelete = Cpg(graph).method.fullNameExact(fullName).ast.l
    nodesToDelete.foreach(v => graph.remove(v))
  }

  def getMethod(graph: Graph, fullName: String): util.List[Edge] = {
    Cpg(graph).method
      .fullNameExact(fullName)
      .ast
      .outE(EdgeTypes.AST, EdgeTypes.CFG, EdgeTypes.ARGUMENT, EdgeTypes.REF)
      .l
      .asJava
  }

  def getMethodStub(graph: Graph, fullName: String): util.List[Edge] = {
    Cpg(graph).method
      .fullNameExact(fullName)
      .outE(EdgeTypes.AST)
      .l
      .asJava
  }

  def getMetaData(graph: Graph):Option[MetaData] = Cpg(graph).metaData.nextOption()

  def getNeighbours(graph: Graph, nodeId: Long): util.List[Edge] = {
    Cpg(graph)
      .id[StoredNode](nodeId)
      .collect { case x: AstNode => x }
      .flatMap { f =>
        List(f) ++ f.astChildren
      }
      .inE
      .l
      .asJava
  }

  def clearGraph(graph: Graph): Unit = {
    val nodesToDelete = Cpg(graph).all.l
    nodesToDelete.foreach(v => graph.remove(v))
  }

  def maxNumberOfDefsFromAMethod(graph: Graph): Int = {
    Cpg(graph).method.map(ReachingDefProblem.create)
      .map(_.transferFunction.asInstanceOf[ReachingDefTransferFunction].gen.foldLeft(0)(_ + _._2.size))
      .maxOption match {
      case Some(value) => value
      case None => 0
    }
  }

}
