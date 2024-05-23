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
    val fromCheck = from match {
      case MetaData.Label                  => MetaData.Edges.Out.contains(edge)
      case File.Label                      => File.Edges.Out.contains(edge)
      case Method.Label                    => Method.Edges.Out.contains(edge)
      case MethodParameterIn.Label         => MethodParameterIn.Edges.Out.contains(edge)
      case MethodParameterOut.Label        => MethodParameterOut.Edges.Out.contains(edge)
      case MethodReturn.Label              => MethodReturn.Edges.Out.contains(edge)
      case Modifier.Label                  => Modifier.Edges.Out.contains(edge)
      case Type.Label                      => Type.Edges.Out.contains(edge)
      case TypeDecl.Label                  => TypeDecl.Edges.Out.contains(edge)
      case TypeParameter.Label             => TypeParameter.Edges.Out.contains(edge)
      case TypeArgument.Label              => TypeArgument.Edges.Out.contains(edge)
      case Member.Label                    => Member.Edges.Out.contains(edge)
      case Namespace.Label                 => Namespace.Edges.Out.contains(edge)
      case NamespaceBlock.Label            => NamespaceBlock.Edges.Out.contains(edge)
      case Literal.Label                   => Literal.Edges.Out.contains(edge)
      case Call.Label                      => Call.Edges.Out.contains(edge)
      case ClosureBinding.Label            => ClosureBinding.Edges.Out.contains(edge)
      case Local.Label                     => Local.Edges.Out.contains(edge)
      case Identifier.Label                => Identifier.Edges.Out.contains(edge)
      case FieldIdentifier.Label           => FieldIdentifier.Edges.Out.contains(edge)
      case Return.Label                    => Return.Edges.Out.contains(edge)
      case Block.Label                     => Block.Edges.Out.contains(edge)
      case MethodRef.Label                 => MethodRef.Edges.Out.contains(edge)
      case TypeRef.Label                   => TypeRef.Edges.Out.contains(edge)
      case JumpTarget.Label                => JumpTarget.Edges.Out.contains(edge)
      case ControlStructure.Label          => ControlStructure.Edges.Out.contains(edge)
      case Annotation.Label                => Annotation.Edges.Out.contains(edge)
      case AnnotationLiteral.Label         => AnnotationLiteral.Edges.Out.contains(edge)
      case AnnotationParameter.Label       => AnnotationParameter.Edges.Out.contains(edge)
      case AnnotationParameterAssign.Label => AnnotationParameterAssign.Edges.Out.contains(edge)
      case Unknown.Label                   => Unknown.Edges.Out.contains(edge)
      case x =>
        logger.warn(s"Unhandled node type '$x'")
        false
    }
    val toCheck = to match {
      case MetaData.Label                  => MetaData.Edges.In.contains(edge)
      case File.Label                      => File.Edges.In.contains(edge)
      case Method.Label                    => Method.Edges.In.contains(edge)
      case MethodParameterIn.Label         => MethodParameterIn.Edges.In.contains(edge)
      case MethodParameterOut.Label        => MethodParameterOut.Edges.In.contains(edge)
      case MethodReturn.Label              => MethodReturn.Edges.In.contains(edge)
      case Modifier.Label                  => Modifier.Edges.In.contains(edge)
      case Type.Label                      => Type.Edges.In.contains(edge)
      case TypeDecl.Label                  => TypeDecl.Edges.In.contains(edge)
      case TypeParameter.Label             => TypeParameter.Edges.In.contains(edge)
      case TypeArgument.Label              => TypeArgument.Edges.In.contains(edge)
      case Member.Label                    => Member.Edges.In.contains(edge)
      case Namespace.Label                 => Namespace.Edges.In.contains(edge)
      case NamespaceBlock.Label            => NamespaceBlock.Edges.In.contains(edge)
      case Literal.Label                   => Literal.Edges.In.contains(edge)
      case Call.Label                      => Call.Edges.In.contains(edge)
      case ClosureBinding.Label            => ClosureBinding.Edges.Out.contains(edge)
      case Local.Label                     => Local.Edges.In.contains(edge)
      case Identifier.Label                => Identifier.Edges.In.contains(edge)
      case FieldIdentifier.Label           => FieldIdentifier.Edges.In.contains(edge)
      case Return.Label                    => Return.Edges.In.contains(edge)
      case Block.Label                     => Block.Edges.In.contains(edge)
      case MethodRef.Label                 => MethodRef.Edges.In.contains(edge)
      case TypeRef.Label                   => TypeRef.Edges.In.contains(edge)
      case JumpTarget.Label                => JumpTarget.Edges.In.contains(edge)
      case ControlStructure.Label          => ControlStructure.Edges.In.contains(edge)
      case Annotation.Label                => Annotation.Edges.In.contains(edge)
      case AnnotationLiteral.Label         => AnnotationLiteral.Edges.In.contains(edge)
      case AnnotationParameter.Label       => AnnotationParameter.Edges.In.contains(edge)
      case AnnotationParameterAssign.Label => AnnotationParameterAssign.Edges.In.contains(edge)
      case Unknown.Label                   => Unknown.Edges.In.contains(edge)
      case x =>
        logger.warn(s"Unhandled node type '$x'")
        false
    }

    fromCheck && toCheck
  }

  def allProperties: Set[String] = NodeToProperties.flatMap(_._2).toSet

  val NodeToProperties: Map[String, Set[String]] = Map(
    MetaData.Label                  -> MetaData.PropertyNames.all,
    File.Label                      -> File.PropertyNames.all,
    Method.Label                    -> Method.PropertyNames.all,
    MethodParameterIn.Label         -> MethodParameterIn.PropertyNames.all,
    MethodParameterOut.Label        -> MethodParameterOut.PropertyNames.all,
    MethodReturn.Label              -> MethodReturn.PropertyNames.all,
    Modifier.Label                  -> Modifier.PropertyNames.all,
    Type.Label                      -> Type.PropertyNames.all,
    TypeDecl.Label                  -> TypeDecl.PropertyNames.all,
    TypeParameter.Label             -> TypeParameter.PropertyNames.all,
    TypeArgument.Label              -> TypeArgument.PropertyNames.all,
    Member.Label                    -> Member.PropertyNames.all,
    Namespace.Label                 -> Namespace.PropertyNames.all,
    NamespaceBlock.Label            -> NamespaceBlock.PropertyNames.all,
    Literal.Label                   -> Literal.PropertyNames.all,
    Call.Label                      -> Call.PropertyNames.all,
    Local.Label                     -> Local.PropertyNames.all,
    Identifier.Label                -> Identifier.PropertyNames.all,
    FieldIdentifier.Label           -> FieldIdentifier.PropertyNames.all,
    Return.Label                    -> Return.PropertyNames.all,
    Block.Label                     -> Block.PropertyNames.all,
    MethodRef.Label                 -> MethodRef.PropertyNames.all,
    TypeRef.Label                   -> TypeRef.PropertyNames.all,
    JumpTarget.Label                -> JumpTarget.PropertyNames.all,
    ControlStructure.Label          -> ControlStructure.PropertyNames.all,
    Annotation.Label                -> Annotation.PropertyNames.all,
    AnnotationLiteral.Label         -> AnnotationLiteral.PropertyNames.all,
    AnnotationParameter.Label       -> AnnotationParameter.PropertyNames.all,
    AnnotationParameterAssign.Label -> AnnotationParameterAssign.PropertyNames.all,
    Unknown.Label                   -> Unknown.PropertyNames.all
  )

}
