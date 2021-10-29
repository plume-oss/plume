package io.github.plume.oss.drivers

import io.shiftleft.passes.AppliedDiffGraph
import overflowdb.Node

trait IDriver extends AutoCloseable {

  def isConnected: Boolean

  def connect(): Unit

  def addNode(v: Node): Unit

  def addEdge(src: Node, dst: Node, edge: String): Unit

  def exists(nodeId: Long): Boolean

  def exists(srcId: Long, dstId: Long, edge: String): Boolean

  def bulkTx(dg: AppliedDiffGraph): Unit

  def deleteMethod(fullName: String): Unit

  def nodesByLabel(label: String): List[Node]

  def nodesByProperty(key: String, value: Any, label: String = null): List[Node]

  def getVertexIds(lower: Long, upper: Long): Set[Long]

}

trait ISchemaSafeDriver extends IDriver {

  def buildSchema(): Unit

  def buildSchemaPayload(): String

}
