/*
 * Copyright 2021 Plume Authors
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.plume.oss.util

import io.shiftleft.codepropertygraph.generated.EdgeTypes
import io.shiftleft.codepropertygraph.generated.NodeKeyNames
import io.shiftleft.codepropertygraph.generated.NodeTypes

object ExtractorConst {
    const val LANGUAGE_FRONTEND = "Plume"
    val plumeVersion: String by lazy { javaClass.`package`.implementationVersion ?: "X.X.X" }
    const val ENTRYPOINT = "BODY"
    const val TRUE_TARGET = "TRUE"
    const val FALSE_TARGET = "FALSE"
    const val UNKNOWN = "<unknown>"
    const val GLOBAL = "<global>"
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