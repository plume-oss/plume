package io.github.plume.oss.util

import io.shiftleft.codepropertygraph.generated.NodeKeyNames

object ExtractorConst {
    const val LANGUAGE_FRONTEND = "Plume"
    const val LANGUAGE_FRONTEND_VERSION = "0.2.4"
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
}