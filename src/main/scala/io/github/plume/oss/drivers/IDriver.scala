package io.github.plume.oss.drivers

import io.shiftleft.passes.AppliedDiffGraph

/** The interface for all methods that should be implemented by the driver's underlying database query language.
  */
trait IDriver extends AutoCloseable {

  /** Will return true if the database is connected, false if otherwise.
    */
  def isConnected: Boolean

  /** Attempts to connect the database with the given parameters.
    */
  def connect(): Unit

  /** Removes all entries from the database.
    */
  def clear(): Unit

  /** Determines if the node exists in the database.
    */
  def exists(nodeId: Long): Boolean

  /** Determines if there exists an edge between two nodes. Edges are assumed to be directional.
    */
  def exists(srcId: Long, dstId: Long, edge: String): Boolean

  /** Executes all changes contained within the given [[io.shiftleft.passes.AppliedDiffGraph]] as a (or set of) bulk
    * transaction(s).
    */
  def bulkTx(dg: AppliedDiffGraph): Unit

  /** Removes the specified node and children based on the edge and key-value property. The given edge type will be
    * followed to determine node children and the rest of the parameters will be used to determine the starting node.
    * This detects already visited nodes and thus is safe from cycles.
    */
  def deleteNodeWithChildren(
      nodeType: String,
      edgeToFollow: String,
      propertyKey: String,
      propertyValue: Any
  ): Unit

  /** Obtains properties from the specified node type and key(s).
    */
  def propertyFromNodes(nodeType: String, keys: String*): List[Seq[String]]

  /** Returns all the taken IDs between the two boundaries (inclusive).
    */
  def idInterval(lower: Long, upper: Long): Set[Long]

}

/** An interface that describes a driver with an underlying database that requires a defined schema.
  */
trait ISchemaSafeDriver extends IDriver {

  /** Create the schema on the underlying database.
    */
  def buildSchema(): Unit

  /** Build the schema to be injected.
    */
  def buildSchemaPayload(): String

}
