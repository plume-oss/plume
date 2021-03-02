package io.github.plume.oss

import java.util
import io.shiftleft.codepropertygraph.Cpg
import io.shiftleft.codepropertygraph.generated.{EdgeTypes, NodeKeyNames, nodes}
import io.shiftleft.codepropertygraph.generated.nodes.{AstNode, File, HasOrder, MetaData, NamespaceBlock, StoredNode, TypeDecl}
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

  def getMethodNames(graph: Graph): util.List[String] = {
    Cpg(graph).method.iterator.map(_.property(NodeKeyNames.FULL_NAME).asInstanceOf[String]).toList.asJava
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

  def getVertexIds(graph: Graph, lowerBound: Long, upperBound: Long): util.Set[Long] = {
    Cpg(graph).all
      .map { x =>
        x.id()
      }
      .filter { id =>
        lowerBound to upperBound contains id
      }
      .toSet
      .asJava
  }

  def clearGraph(graph: Graph): Unit = {
    val nodesToDelete = Cpg(graph).all.l
    nodesToDelete.foreach(v => graph.remove(v))
  }

}
