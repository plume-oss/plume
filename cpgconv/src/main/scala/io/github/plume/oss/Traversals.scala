package io.github.plume.oss

import java.util
import io.shiftleft.codepropertygraph.Cpg
import io.shiftleft.codepropertygraph.generated.{EdgeTypes, nodes}
import io.shiftleft.codepropertygraph.generated.nodes.{AstNode, File, MetaData, Method, NamespaceBlock, StoredNode, TypeDecl}
import io.shiftleft.dataflowengineoss.passes.reachingdef.{ReachingDefProblem, ReachingDefTransferFunction}
import io.shiftleft.semanticcpg.language._
import overflowdb.{Edge, Graph}

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

  import overflowdb.traversal._
  def getProgramStructure(graph: Graph): util.List[Edge] = {
    val edgesFromFile: List[Edge] = Cpg(graph).file
      .outE(EdgeTypes.AST)
      .filter(_.inNode().isInstanceOf[nodes.NamespaceBlock])
      .l
    val edgesFromNamespaceBlock: List[Edge] = edgesFromFile
      .to(Traversal)
      .inV
      .collect {
        case x: nodes.NamespaceBlock =>
          x.outE(EdgeTypes.AST).filter(_.inNode().isInstanceOf[nodes.NamespaceBlock]).l
      }
      .l
      .flatten
    (edgesFromFile ++ edgesFromNamespaceBlock).asJava
  }

  def getTypeDecls(graph: Graph):util.List[TypeDecl] = Cpg(graph).typeDecl.l.asJava

  def getFiles(graph: Graph):util.List[File] = Cpg(graph).file.l.asJava

  def getNamespaceBlocks(graph: Graph):util.List[NamespaceBlock] = Cpg(graph).namespaceBlock.l.asJava

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
