package io.github.plume.oss.drivers

import io.shiftleft.passes.AppliedDiffGraph
import overflowdb.Node

/** The interface for all methods that should be implemented by the driver's underlying database query language.
  */
trait IDriver extends AutoCloseable {

  def isConnected: Boolean

  def connect(): Unit

  /** Removes all entries from the database.
    */
  def clear(): Unit

  def addNode(v: Node): Unit

  def addEdge(src: Node, dst: Node, edge: String): Unit

  def exists(nodeId: Long): Boolean

  /** Determines if there exists an edge between two nodes. Edges are assumed to be directional.
    */
  def exists(srcId: Long, dstId: Long, edge: String): Boolean

  /** Executes all changes contained within the given [AppliedDiffGraph] as a (or set of) bulk transaction(s).
    */
  def bulkTx(dg: AppliedDiffGraph): Unit

  def deleteMethod(fullName: String): Unit

  /** Obtains properties from the specified node type and key(s).
    */
  def propertyFromNodes(nodeType: String, keys: String*): List[Seq[String]]

  def getVertexIds(lower: Long, upper: Long): Set[Long]

}

/** An interface that describes a driver with an underlying database that requires a defined schema.
  */
trait ISchemaSafeDriver extends IDriver {

  def buildSchema(): Unit

  def buildSchemaPayload(): String

}
