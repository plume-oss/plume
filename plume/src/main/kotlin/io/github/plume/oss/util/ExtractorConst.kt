package io.github.plume.oss.util

import io.shiftleft.codepropertygraph.generated.EdgeTypes
import io.shiftleft.codepropertygraph.generated.NodeKeyNames
import io.shiftleft.codepropertygraph.generated.NodeTypes

object ExtractorConst {
    const val LANGUAGE_FRONTEND = "Plume"
    val plumeVersion: String by lazy { javaClass.`package`.implementationVersion ?: "X.X.X" }
    val PRIMITIVES = listOf("boolean", "byte", "char", "double", "float", "int", "long", "null", "short", "void")
    // block bodies
    const val ENTRYPOINT = "BODY"
    const val TRUE_TARGET = "TRUE"
    const val FALSE_TARGET = "FALSE"
    val BOOLEAN_TYPES = setOf(
        NodeKeyNames.HAS_MAPPING,
        NodeKeyNames.IS_METHOD_NEVER_OVERRIDDEN,
        NodeKeyNames.IS_EXTERNAL
    )
    val INT_TYPES = setOf(
        NodeKeyNames.COLUMN_NUMBER,
        NodeKeyNames.DEPTH_FIRST_ORDER,
        NodeKeyNames.ARGUMENT_INDEX,
        NodeKeyNames.ORDER,
        NodeKeyNames.LINE_NUMBER,
        NodeKeyNames.LINE_NUMBER_END,
        NodeKeyNames.INTERNAL_FLAGS,
        NodeKeyNames.COLUMN_NUMBER_END
    )
    val TYPE_REFERENCED_NODES = arrayOf(
        NodeTypes.TYPE_DECL,
        NodeTypes.TYPE,
        NodeTypes.TYPE_ARGUMENT,
        NodeTypes.TYPE_PARAMETER,
        NodeTypes.METHOD,
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
        NodeTypes.NAMESPACE_BLOCK,
        NodeTypes.UNKNOWN
    )
    val TYPE_REFERENCED_EDGES = arrayOf(EdgeTypes.AST, EdgeTypes.REF, EdgeTypes.ALIAS_OF, EdgeTypes.INHERITS_FROM)
}