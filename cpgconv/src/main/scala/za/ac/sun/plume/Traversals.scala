package za.ac.sun.plume

import java.util

import io.shiftleft.codepropertygraph.Cpg
import io.shiftleft.codepropertygraph.generated.nodes.{AstNode, HasOrder}
import io.shiftleft.semanticcpg.language._
import overflowdb.Graph

import scala.jdk.CollectionConverters._

object Traversals {

  def maxOrder(graph : Graph) : Integer = {
    Cpg(graph).all.collect{ case x : HasOrder => x.order }.maxOption.getOrElse(0)
  }

  def deleteMethod(graph : Graph, fullName : String, signature : String) : Unit = {
    val nodesToDelete = Cpg(graph).method.fullNameExact(fullName).ast.l
    nodesToDelete.foreach(v => graph.remove(v))
  }

  def getMethod(graph : Graph, fullName : String, signature : String): util.List[(AstNode, util.List[AstNode])] = {
    Cpg(graph).method.fullNameExact(fullName).signatureExact(signature)
      .ast
      .map{ node => (node, node.astChildren.l.asJava) }
      .l.asJava
  }

  def getMethodStub(graph : Graph, fullName : String, signature : String) : util.List[(AstNode, util.List[AstNode])]  = {
    Cpg(graph).method.fullNameExact(fullName).signatureExact(signature).map{ m =>
      (m.asInstanceOf[AstNode], m.astChildren.l.asJava)
    }.l.asJava
  }

  def clearGraph(graph : Graph) : Unit = {
    val nodesToDelete = Cpg(graph).all.l
    nodesToDelete.foreach(v => graph.remove(v))
  }

}
