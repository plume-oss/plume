package com.github.plume.oss.drivers

import io.shiftleft.codepropertygraph.generated.nodes.{AbstractNode, NewNode, StoredNode}
import io.shiftleft.codepropertygraph.generated.{EdgeTypes, NodeTypes, PropertyNames}
import io.shiftleft.passes.AppliedDiffGraph

import scala.collection.mutable

/** The interface for all methods that should be implemented by the driver's underlying database query language.
  */
trait IDriver extends AutoCloseable {

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

  /** Executes all changes contained within the given io.shiftleft.passes.AppliedDiffGraph as a (or set of) bulk
    * transaction(s).
    */
  def bulkTx(dg: AppliedDiffGraph): Unit

  /** Given filenames, will remove related TYPE, TYPE_DECL, METHOD (with AST children), and NAMESPACE_BLOCK.
    */
  def removeSourceFiles(filenames: String*): Unit

  /** Obtains properties from the specified node type and key(s). By default will return the ID property as one of the
    * keys as "id".
    */
  def propertyFromNodes(nodeType: String, keys: String*): List[Map[String, Any]]

  protected def removeLists(properties: Map[String, Any]): Map[String, Any] = {
    properties.map { case (k, v) =>
      v match {
        case Seq(head, _*) => k -> head
        case _             => k -> v
      }
    }
  }

  /** Returns all the taken IDs between the two boundaries (inclusive).
    */
  def idInterval(lower: Long, upper: Long): Set[Long]

  protected val typeDeclFullNameToNode = mutable.Map.empty[String, Any]
  protected val typeFullNameToNode     = mutable.Map.empty[String, Any]
  protected val methodFullNameToNode   = mutable.Map.empty[String, Any]
  protected val namespaceNameToNode    = mutable.Map.empty[String, Any]

  /** Runs linkers for AST node relations and calls.
    */
  def buildInterproceduralEdges(): Unit = {
    initMaps()
    astLinker()
    staticCallLinker()
    dynamicCallLinker()
    clearMaps()
  }

  /** Create REF edges from TYPE nodes to TYPE_DECL, EVAL_TYPE edges from nodes of various types to TYPE, REF edges from
    * METHOD_REFs to METHOD, INHERITS_FROM nodes from TYPE_DECL nodes to TYPE, and ALIAS_OF edges from TYPE_DECL nodes
    * to TYPE.
    *
    * Requires [[initMaps]]
    */
  protected def astLinker(): Unit = {
    // Link NAMESPACE and NAMESPACE_BLOCK
    linkAstNodes(
      srcLabels = List(NodeTypes.NAMESPACE_BLOCK),
      edgeType = EdgeTypes.REF,
      dstNodeMap = namespaceNameToNode,
      dstFullNameKey = PropertyNames.NAME,
      dstNodeType = NodeTypes.NAMESPACE
    )
    // Create REF edges between TYPE and TYPE_DECL
    linkAstNodes(
      srcLabels = List(NodeTypes.TYPE),
      edgeType = EdgeTypes.REF,
      dstNodeMap = typeDeclFullNameToNode,
      dstFullNameKey = PropertyNames.FULL_NAME,
      dstNodeType = NodeTypes.TYPE_DECL
    )
    // Create EVAL_TYPE edges from nodes of various types
    // to TYPE
    linkAstNodes(
      srcLabels = List(
        NodeTypes.METHOD_PARAMETER_IN,
        NodeTypes.METHOD_PARAMETER_OUT,
        NodeTypes.METHOD_RETURN,
        NodeTypes.MEMBER,
        NodeTypes.LITERAL,
        NodeTypes.CALL,
        NodeTypes.LOCAL,
        NodeTypes.IDENTIFIER,
        NodeTypes.BLOCK,
        NodeTypes.METHOD_REF,
        NodeTypes.TYPE_REF,
        NodeTypes.UNKNOWN
      ),
      edgeType = EdgeTypes.EVAL_TYPE,
      dstNodeMap = typeFullNameToNode,
      dstFullNameKey = PropertyNames.TYPE_FULL_NAME,
      dstNodeType = NodeTypes.TYPE
    )
    // Create REF edges from METHOD_REFs to
    // METHOD
    linkAstNodes(
      srcLabels = List(NodeTypes.METHOD_REF),
      edgeType = EdgeTypes.REF,
      dstNodeMap = methodFullNameToNode,
      dstFullNameKey = PropertyNames.METHOD_FULL_NAME,
      dstNodeType = NodeTypes.METHOD
    )
    // Create INHERITS_FROM nodes from TYPE_DECL
    // nodes to TYPE
    linkAstNodes(
      srcLabels = List(NodeTypes.TYPE_DECL),
      edgeType = EdgeTypes.INHERITS_FROM,
      dstNodeMap = typeFullNameToNode,
      dstFullNameKey = PropertyNames.INHERITS_FROM_TYPE_FULL_NAME,
      dstNodeType = NodeTypes.TYPE
    )
    // Create ALIAS_OF edges from TYPE_DECL nodes to
    // TYPE
    linkAstNodes(
      srcLabels = List(NodeTypes.TYPE_DECL),
      edgeType = EdgeTypes.ALIAS_OF,
      dstNodeMap = typeFullNameToNode,
      dstFullNameKey = PropertyNames.ALIAS_TYPE_FULL_NAME,
      dstNodeType = NodeTypes.TYPE
    )
  }

  protected def initMaps(): Unit = {
    def initMap(k: String, p: String, map: mutable.Map[String, Any]): Unit = {
      propertyFromNodes(k, p).foreach { m =>
        val id = m.getOrElse("id", null)
        val fn = m.getOrElse(p, null)
        if (fn != null) {
          map += fn.toString -> id
        }
      }
    }
    initMap(NodeTypes.TYPE_DECL, PropertyNames.FULL_NAME, typeDeclFullNameToNode)
    initMap(NodeTypes.TYPE, PropertyNames.FULL_NAME, typeFullNameToNode)
    initMap(NodeTypes.METHOD, PropertyNames.FULL_NAME, methodFullNameToNode)
    initMap(NodeTypes.NAMESPACE, PropertyNames.NAME, namespaceNameToNode)
  }

  protected def clearMaps(): Unit = {
    typeDeclFullNameToNode.clear()
    typeFullNameToNode.clear()
    methodFullNameToNode.clear()
    namespaceNameToNode.clear()
  }

  /** Links nodes by their source label and destination full name key to their destination nodes by the
    * specified edge type using the destination node map as the lookup table. Source labels are assumed
    * to be non-empty.
    */
  def linkAstNodes(
      srcLabels: List[String],
      edgeType: String,
      dstNodeMap: mutable.Map[String, Any],
      dstFullNameKey: String,
      dstNodeType: String
  ): Unit

  /** Links all static dispatch calls to their methods.
    */
  protected def staticCallLinker(): Unit

  /** Links all dynamic dispatch calls to their methods.
    */
  protected def dynamicCallLinker(): Unit

  /** Provides the assigned ID for the given node using the given diff graph.
    */
  protected def id(node: AbstractNode, dg: AppliedDiffGraph): Any =
    node match {
      case n: NewNode    => dg.nodeToGraphId(n)
      case n: StoredNode => n.id()
      case _             => throw new RuntimeException(s"Unable to obtain ID for $node")
    }

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
  val STRING_DEFAULT: String = "<empty>"
  val INT_DEFAULT: Int       = -1
  val LONG_DEFAULT: Long     = -1L
  val BOOL_DEFAULT: Boolean  = false

  /** Given a property, returns its known default.
    */
  def getPropertyDefault(prop: String): Any = {
    import PropertyNames._
    prop match {
      case AST_PARENT_TYPE      => STRING_DEFAULT
      case AST_PARENT_FULL_NAME => STRING_DEFAULT
      case NAME                 => STRING_DEFAULT
      case CODE                 => STRING_DEFAULT
      case ORDER                => INT_DEFAULT
      case SIGNATURE            => ""
      case ARGUMENT_INDEX       => INT_DEFAULT
      case FULL_NAME            => STRING_DEFAULT
      case TYPE_FULL_NAME       => STRING_DEFAULT
      case TYPE_DECL_FULL_NAME  => STRING_DEFAULT
      case IS_EXTERNAL          => BOOL_DEFAULT
      case DISPATCH_TYPE        => STRING_DEFAULT
      case LINE_NUMBER          => INT_DEFAULT
      case COLUMN_NUMBER        => INT_DEFAULT
      case LINE_NUMBER_END      => INT_DEFAULT
      case COLUMN_NUMBER_END    => INT_DEFAULT
      case _                    => STRING_DEFAULT
    }
  }
}