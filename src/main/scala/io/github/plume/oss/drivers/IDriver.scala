package io.github.plume.oss.drivers

import io.shiftleft.codepropertygraph.generated.nodes.NewNode
import io.shiftleft.passes.{AppliedDiffGraph, DiffGraph}

trait IDriver extends AutoCloseable {

  def isConnected: Boolean

  def connect(): Unit

  def addNode(v: NewNode): Unit

  def addEdge(src: NewNode, dst: NewNode, edge: String): Unit

  def exists(nodeId: Long): Boolean

  def exists(srcId: Long, dstId: Long, edge: String): Boolean

  def bulkTx(dg: AppliedDiffGraph): Unit

  def deleteMethod(fullName: String): Unit

  def getNodeByLabel(label: String): List[NewNode]

  def getPropertyFromVertices(key: String, value: Any, label: String = null): List[NewNode]

  def getVertexIds(lower: Long, upper: Long): Set[Long]

}

trait ISchemaSafeDriver extends IDriver {

  def buildSchema(): Unit

  def buildSchemaPayload(): String

}
