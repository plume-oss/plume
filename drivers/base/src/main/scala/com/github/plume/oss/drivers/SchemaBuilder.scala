package com.github.plume.oss.drivers

import io.shiftleft.codepropertygraph.generated.EdgeTypes
import io.shiftleft.codepropertygraph.generated.nodes.*
import org.slf4j.LoggerFactory

/** A single utility to build out the CPG schema in other databases.
  */
object SchemaBuilder {

  private val logger = LoggerFactory.getLogger(getClass)

  val STRING_DEFAULT: String    = "<empty>"
  val INT_DEFAULT: Int          = -1
  val LONG_DEFAULT: Long        = -1L
  val BOOL_DEFAULT: Boolean     = false
  val LIST_DEFAULT: Seq[String] = Seq.empty[String]

  /** Given a property, returns its known default.
    */
  def getPropertyDefault(prop: String): Any = {
    import io.shiftleft.codepropertygraph.generated.PropertyNames.*
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
      case POSSIBLE_TYPES               => LIST_DEFAULT
      case _                            => STRING_DEFAULT
    }
  }

  /** Edges that should be specified as being between any kind of vertex.
    */
  val WILDCARD_EDGE_LABELS: Set[String] =
    Set(EdgeTypes.EVAL_TYPE, EdgeTypes.REF, EdgeTypes.INHERITS_FROM, EdgeTypes.ALIAS_OF)

  /** Determines if an edge type between two node types is valid.
    */
  def checkEdgeConstraint(from: String, to: String, edge: String): Boolean = {
    def makeNewNode(label: String): NewNode = label match {
      case MetaData.Label                  => NewMetaData()
      case File.Label                      => NewFile()
      case Method.Label                    => NewMethod()
      case MethodParameterIn.Label         => NewMethodParameterIn()
      case MethodParameterOut.Label        => NewMethodParameterOut()
      case MethodReturn.Label              => NewMethodReturn()
      case Modifier.Label                  => NewModifier()
      case Type.Label                      => NewType()
      case TypeDecl.Label                  => NewTypeDecl()
      case TypeParameter.Label             => NewTypeParameter()
      case TypeArgument.Label              => NewTypeArgument()
      case Member.Label                    => NewMember()
      case Namespace.Label                 => NewNamespace()
      case NamespaceBlock.Label            => NewNamespaceBlock()
      case Literal.Label                   => NewLiteral()
      case Call.Label                      => NewCall()
      case ClosureBinding.Label            => NewClosureBinding()
      case Local.Label                     => NewLocal()
      case Identifier.Label                => NewIdentifier()
      case FieldIdentifier.Label           => NewFieldIdentifier()
      case Return.Label                    => NewReturn()
      case Block.Label                     => NewBlock()
      case MethodRef.Label                 => NewMethodRef()
      case TypeRef.Label                   => NewTypeRef()
      case JumpTarget.Label                => NewJumpTarget()
      case ControlStructure.Label          => NewControlStructure()
      case Annotation.Label                => NewAnnotation()
      case AnnotationLiteral.Label         => NewAnnotationLiteral()
      case AnnotationParameter.Label       => NewAnnotationParameter()
      case AnnotationParameterAssign.Label => NewAnnotationParameterAssign()
      case Unknown.Label                   => NewUnknown()
      case x =>
        logger.warn(s"Unhandled node type '$x'")
        NewUnknown()
    }
    val fromNode = makeNewNode(from)
    val toNode   = makeNewNode(to)
    fromNode.isValidOutNeighbor(edge, toNode) && toNode.isValidInNeighbor(edge, fromNode)
  }

  def allProperties: Set[String] = NodeToProperties.flatMap(_._2).toSet

  val NodeToProperties: Map[String, Set[String]] = Map(
    MetaData.Label                  -> NewMetaData().properties.keySet,
    File.Label                      -> NewFile().properties.keySet,
    Method.Label                    -> NewMethod().properties.keySet,
    MethodParameterIn.Label         -> NewMethodParameterIn().properties.keySet,
    MethodParameterOut.Label        -> NewMethodParameterOut().properties.keySet,
    MethodReturn.Label              -> NewMethodReturn().properties.keySet,
    Modifier.Label                  -> NewModifier().properties.keySet,
    Type.Label                      -> NewType().properties.keySet,
    TypeDecl.Label                  -> NewTypeDecl().properties.keySet,
    TypeParameter.Label             -> NewTypeParameter().properties.keySet,
    TypeArgument.Label              -> NewTypeArgument().properties.keySet,
    Member.Label                    -> NewMember().properties.keySet,
    Namespace.Label                 -> NewNamespace().properties.keySet,
    NamespaceBlock.Label            -> NewNamespaceBlock().properties.keySet,
    Literal.Label                   -> NewLiteral().properties.keySet,
    Call.Label                      -> NewCall().properties.keySet,
    Local.Label                     -> NewLocal().properties.keySet,
    Identifier.Label                -> NewIdentifier().properties.keySet,
    FieldIdentifier.Label           -> NewFieldIdentifier().properties.keySet,
    Return.Label                    -> NewReturn().properties.keySet,
    Block.Label                     -> NewBlock().properties.keySet,
    MethodRef.Label                 -> NewMethodRef().properties.keySet,
    TypeRef.Label                   -> NewTypeRef().properties.keySet,
    JumpTarget.Label                -> NewJumpTarget().properties.keySet,
    ControlStructure.Label          -> NewControlStructure().properties.keySet,
    Annotation.Label                -> NewAnnotation().properties.keySet,
    AnnotationLiteral.Label         -> NewAnnotationLiteral().properties.keySet,
    AnnotationParameter.Label       -> NewAnnotationParameter().properties.keySet,
    AnnotationParameterAssign.Label -> NewAnnotationParameterAssign().properties.keySet,
    Unknown.Label                   -> NewUnknown().properties.keySet
  )

}
