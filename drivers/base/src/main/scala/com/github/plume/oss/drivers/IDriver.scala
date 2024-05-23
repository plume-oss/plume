package com.github.plume.oss.drivers

import io.shiftleft.codepropertygraph.generated.PropertyNames
import io.shiftleft.codepropertygraph.generated.nodes.*
import org.slf4j.LoggerFactory
import overflowdb.BatchedUpdate.DiffOrBuilder
import overflowdb.DetachedNodeGeneric

import java.util.concurrent.atomic.AtomicLong
import scala.collection.concurrent.TrieMap
import scala.collection.mutable
import scala.collection.mutable.ListBuffer

/** The interface for all methods that should be implemented by the driver's underlying database query language.
  */
trait IDriver extends AutoCloseable {

  private val logger = LoggerFactory.getLogger(IDriver.getClass)
  // ID Tracking
  protected val currId = new AtomicLong(1)
  private val nodeId   = TrieMap.empty[overflowdb.NodeOrDetachedNode, Long]

  /** Will return true if the database is connected, false if otherwise.
    */
  def isConnected: Boolean

  /** Removes all entries from the database.
    */
  def clear(): Unit

  /** Determines if the node exists in the database.
    */
  def exists(nodeId: Long): Boolean

  /** Determines if there exists an edge between two nodes. Edges are assumed to be directional.
    */
  def exists(srcId: Long, dstId: Long, edge: String): Boolean

  /** Executes all changes contained within the given overflowdb.BatchedUpdate.AppliedDiff as a (or set of) bulk
    * transaction(s).
    */
  def bulkTx(dg: DiffOrBuilder): Int

  /** Obtains properties from the specified node type and key(s). By default will return the ID property as one of the
    * keys as "id".
    */
  def propertyFromNodes(nodeType: String, keys: String*): List[Map[String, Any]]

  /** To handle the case where databases don't support lists, we simply serialize these as a comma-separated string.
    * @param properties
    *   the property map.
    * @return
    *   a property map where lists are serialized as strings.
    */
  protected def serializeLists(properties: Map[String, Any]): Seq[(String, Any)] = {
    properties.map { case (k, v) =>
      v match {
        case xs: Seq[_] => k -> xs.mkString(",")
        case _          => k -> v
      }
    }.toSeq
  }

  /** Where former list properties were serialized as strings, they will be deserialized as Seq.
    * @param properties
    *   the serialized property map.
    * @return
    *   a property map where comma-separated strings are made Seq objects.
    */
  protected def deserializeLists(properties: Map[String, Any]): Map[String, Any] = {
    properties.map { case (k, v) =>
      v match {
        case xs: String if k == PropertyNames.OVERLAYS || k == PropertyNames.INHERITS_FROM_TYPE_FULL_NAME =>
          k -> xs.split(",").toSeq
        case _ => k -> v
      }
    }
  }

  protected def unpack(p: Array[AnyRef]): Seq[(String, AnyRef)] = Option(p) match {
    case Some(buff) =>
      val buffer = new ListBuffer[(String, AnyRef)]()
      var i      = 0
      while (i < buff.length) {
        buffer += Tuple2(p(i).asInstanceOf[String], p(i + 1))
        i += 2
      }
      buffer.toList
    case None => Seq.empty
  }

  implicit class NodeOrDetachedNodeExt(node: overflowdb.NodeOrDetachedNode) {

    /** @return
      *   the internally tracked Plume ID.
      */
    def pID: Long = nodeId.getOrElseUpdate(node, currId.getAndIncrement())

  }

  protected val methodFullNameToNode = mutable.Map.empty[String, Any]

  protected def NODES_IN_SCHEMA: Seq[String] = Seq(
    MetaData.Label,
    File.Label,
    Method.Label,
    MethodParameterIn.Label,
    MethodParameterOut.Label,
    MethodReturn.Label,
    Modifier.Label,
    Type.Label,
    TypeDecl.Label,
    TypeParameter.Label,
    TypeArgument.Label,
    Member.Label,
    Namespace.Label,
    NamespaceBlock.Label,
    Literal.Label,
    Call.Label,
    Local.Label,
    Identifier.Label,
    FieldIdentifier.Label,
    Return.Label,
    Block.Label,
    MethodRef.Label,
    TypeRef.Label,
    JumpTarget.Label,
    ControlStructure.Label,
    Unknown.Label
  )

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

object IDriver {
  val STRING_DEFAULT: String    = "<empty>"
  val INT_DEFAULT: Int          = -1
  val LONG_DEFAULT: Long        = -1L
  val BOOL_DEFAULT: Boolean     = false
  val LIST_DEFAULT: Seq[String] = Seq.empty[String]

  /** Given a property, returns its known default.
    */
  def getPropertyDefault(prop: String): Any = {
    import PropertyNames.*
    prop match {
      case AST_PARENT_TYPE              => STRING_DEFAULT
      case AST_PARENT_FULL_NAME         => STRING_DEFAULT
      case NAME                         => STRING_DEFAULT
      case CODE                         => STRING_DEFAULT
      case ORDER                        => INT_DEFAULT
      case SIGNATURE                    => ""
      case ARGUMENT_INDEX               => INT_DEFAULT
      case FULL_NAME                    => STRING_DEFAULT
      case TYPE_FULL_NAME               => STRING_DEFAULT
      case TYPE_DECL_FULL_NAME          => STRING_DEFAULT
      case IS_EXTERNAL                  => BOOL_DEFAULT
      case DISPATCH_TYPE                => STRING_DEFAULT
      case LINE_NUMBER                  => INT_DEFAULT
      case COLUMN_NUMBER                => INT_DEFAULT
      case LINE_NUMBER_END              => INT_DEFAULT
      case COLUMN_NUMBER_END            => INT_DEFAULT
      case OVERLAYS                     => LIST_DEFAULT
      case INHERITS_FROM_TYPE_FULL_NAME => LIST_DEFAULT
      case _                            => STRING_DEFAULT
    }
  }
}
