package io.github.plume.oss.drivers

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
        case is: Iterable[String] => k -> is.head
        case _                    => k -> v
      }
    }
  }

  /** Returns all the taken IDs between the two boundaries (inclusive).
    */
  def idInterval(lower: Long, upper: Long): Set[Long]

  protected val typeDeclFullNameToNode = mutable.Map.empty[String, Long]
  protected val typeFullNameToNode     = mutable.Map.empty[String, Long]
  protected val methodFullNameToNode   = mutable.Map.empty[String, Long]
  protected val namespaceNameToNode    = mutable.Map.empty[String, Long]

  /** Create REF edges from TYPE nodes to TYPE_DECL, EVAL_TYPE edges from nodes of various types to TYPE, REF edges from
    * METHOD_REFs to METHOD, INHERITS_FROM nodes from TYPE_DECL nodes to TYPE, and ALIAS_OF edges from TYPE_DECL nodes
    * to TYPE.
    */
  def astLinker(): Unit = {
    initMaps()
    // Link NAMESPACE and NAMESPACE_BLOCK
    linkAstNodes(
      srcLabels = List(NodeTypes.NAMESPACE_BLOCK),
      edgeType = EdgeTypes.REF,
      dstNodeMap = namespaceNameToNode,
      dstFullNameKey = PropertyNames.NAME
    )
    // Create REF edges between TYPE and TYPE_DECL
    linkAstNodes(
      srcLabels = List(NodeTypes.TYPE),
      edgeType = EdgeTypes.REF,
      dstNodeMap = typeDeclFullNameToNode,
      dstFullNameKey = PropertyNames.FULL_NAME
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
      dstFullNameKey = PropertyNames.TYPE_FULL_NAME
    )
    // Create REF edges from METHOD_REFs to
    // METHOD
    linkAstNodes(
      srcLabels = List(NodeTypes.METHOD_REF),
      edgeType = EdgeTypes.REF,
      dstNodeMap = methodFullNameToNode,
      dstFullNameKey = PropertyNames.METHOD_FULL_NAME
    )
    // Create INHERITS_FROM nodes from TYPE_DECL
    // nodes to TYPE
    linkAstNodes(
      srcLabels = List(NodeTypes.TYPE_DECL),
      edgeType = EdgeTypes.INHERITS_FROM,
      dstNodeMap = typeFullNameToNode,
      dstFullNameKey = PropertyNames.INHERITS_FROM_TYPE_FULL_NAME
    )
    // Create ALIAS_OF edges from TYPE_DECL nodes to
    // TYPE
    linkAstNodes(
      srcLabels = List(NodeTypes.TYPE_DECL),
      edgeType = EdgeTypes.ALIAS_OF,
      dstNodeMap = typeFullNameToNode,
      dstFullNameKey = PropertyNames.ALIAS_TYPE_FULL_NAME
    )
    clearMaps()
  }

  protected def initMaps(): Unit = {
    def initMap(k: String, p: String, map: mutable.Map[String, Long]): Unit = {
      propertyFromNodes(k, p).foreach { m =>
        val id = m.getOrElse("id", null).asInstanceOf[Long]
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
  }

  /** Links nodes by their source label and destination full name key to their destination nodes by the
    * specified edge type using the destination node map as the lookup table. Source labels are assumed
    * to be non-empty.
    */
  def linkAstNodes(
      srcLabels: List[String],
      edgeType: String,
      dstNodeMap: mutable.Map[String, Long],
      dstFullNameKey: String
  ): Unit

  /** Given a property, returns its known default.
    */
  protected def getPropertyDefault(prop: String): Any = {
    import PropertyNames._
    val strDefault  = "<empty>"
    val intDefault  = -1
    val boolDefault = false
    prop match {
      case AST_PARENT_TYPE      => strDefault
      case AST_PARENT_FULL_NAME => strDefault
      case NAME                 => strDefault
      case CODE                 => strDefault
      case ORDER                => intDefault
      case SIGNATURE            => ""
      case ARGUMENT_INDEX       => intDefault
      case FULL_NAME            => strDefault
      case TYPE_FULL_NAME       => strDefault
      case TYPE_DECL_FULL_NAME  => strDefault
      case IS_EXTERNAL          => boolDefault
      case DISPATCH_TYPE        => strDefault
      case LINE_NUMBER          => intDefault
      case COLUMN_NUMBER        => intDefault
      case LINE_NUMBER_END      => intDefault
      case COLUMN_NUMBER_END    => intDefault
      case _                    => strDefault
    }
  }

  /** Provides the assigned ID for the given node using the given diff graph.
    */
  protected def id(node: AbstractNode, dg: AppliedDiffGraph): Long =
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
