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
package io.github.plume.oss.domain.mappers

import io.github.plume.oss.util.ExtractorConst.UNKNOWN
import io.shiftleft.codepropertygraph.generated.NodeTypes
import io.shiftleft.codepropertygraph.generated.NodeTypes.*
import io.shiftleft.codepropertygraph.generated.PropertyNames.*
import io.shiftleft.codepropertygraph.generated.nodes.*
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import overflowdb.Node
import scala.Option
import scala.collection.immutable.`$colon$colon`
import scala.collection.immutable.`Nil$`
import scala.jdk.CollectionConverters


/**
 * Responsible for marshalling and unmarshalling vertex properties to and from [NewNode] objects to [Map] objects.
 */
object VertexMapper {
    private val logger: Logger = LogManager.getLogger(VertexMapper::javaClass)

    /**
     * Converts a [Node] to its respective [NewNodeBuilder] object.
     *
     * @param v The [Node] to deserialize.
     * @return a [NewNodeBuilder] represented by the information in the givennode.
     */
    fun mapToVertex(v: Node): NewNodeBuilder<out NewNode> {
        val map = prepareListsInMap(v.propertiesMap()) + mapOf<String, Any>("id" to v.id(), "label" to v.label())
        return mapToVertex(map)
    }

    /**
     * Converts a [NewNode] to its respective [NewNodeBuilder] object.
     *
     * @param v The [NewNode] to deserialize.
     * @return a [NewNodeBuilder] represented by the information in the givennode.
     */
    fun mapToVertex(v: NewNode): NewNodeBuilder<out NewNode> {
        val map = prepareListsInMap(CollectionConverters.MapHasAsJava(v.properties()).asJava()) +
                mapOf<String, Any>("label" to v.label())
        return mapToVertex(map)
    }

    /**
     * Converts a [Map] containing vertex properties to its respective [NewNodeBuilder] object.
     *
     * @param mapToConvert The [Map] to deserialize.
     * @return a [NewNodeBuilder] represented by the information in the given map.
     */
    fun mapToVertex(mapToConvert: Map<String, Any>): NewNodeBuilder<out NewNode> {
        val map = HashMap<String, Any>()
        // Only ID should be left as Long
        mapToConvert.keys.forEach {
            when (val value = mapToConvert[it]) {
                is Long -> if (it != "id") map[it] = value.toInt() else map[it] = value as Any
                else -> map[it] = value as Any
            }
        }
        return when (map["label"] as String) {
            MetaData.Label() -> NewMetaDataBuilder()
                .language(map[LANGUAGE] as String)
                .version(map[VERSION] as String)
                .hash(Option.apply(map.getOrDefault(HASH, UNKNOWN) as String))
            File.Label() -> NewFileBuilder()
                .name(map.getOrDefault(NAME, "<empty>") as String)
                .hash(Option.apply(map.getOrDefault(HASH, UNKNOWN) as String))
                .order(map.getOrDefault(ORDER, -1) as Int)
            Method.Label() -> NewMethodBuilder()
                .astParentFullName(map.getOrDefault(AST_PARENT_FULL_NAME, "<empty>") as String)
                .astParentType(map.getOrDefault(AST_PARENT_TYPE, "<empty>") as String)
                .name(map.getOrDefault(NAME, "<empty>") as String)
                .code(map.getOrDefault(CODE, "<empty>") as String)
                .isExternal(map.getOrDefault(IS_EXTERNAL, false) as Boolean)
                .fullName(map.getOrDefault(FULL_NAME, "<empty>") as String)
                .filename(map.getOrDefault(FILENAME, "<empty>") as String)
                .signature(map.getOrDefault(SIGNATURE, "") as String)
                .lineNumber(Option.apply(map[LINE_NUMBER] as Int))
                .columnNumber(Option.apply(map[COLUMN_NUMBER] as Int))
                .order(map.getOrDefault(ORDER, -1) as Int)
                .hash(Option.apply(map[HASH] as String))
            MethodParameterIn.Label() -> NewMethodParameterInBuilder()
                .code(map.getOrDefault(CODE, "<empty>") as String)
                .name(map.getOrDefault(NAME, "<empty>") as String)
                .evaluationStrategy(map[EVALUATION_STRATEGY] as String)
                .typeFullName(map[TYPE_FULL_NAME] as String)
                .lineNumber(Option.apply(map[LINE_NUMBER] as Int))
                .columnNumber(Option.apply(map[COLUMN_NUMBER] as Int))
                .order(map.getOrDefault(ORDER, -1) as Int)
            MethodParameterOut.Label() -> NewMethodParameterOutBuilder()
                .code(map.getOrDefault(CODE, "<empty>") as String)
                .name(map.getOrDefault(NAME, "<empty>") as String)
                .evaluationStrategy(map[EVALUATION_STRATEGY] as String)
                .typeFullName(map[TYPE_FULL_NAME] as String)
                .lineNumber(Option.apply(map[LINE_NUMBER] as Int))
                .columnNumber(Option.apply(map[COLUMN_NUMBER] as Int))
                .order(map.getOrDefault(ORDER, -1) as Int)
            MethodReturn.Label() -> NewMethodReturnBuilder()
                .code(map.getOrDefault(CODE, "<empty>") as String)
                .evaluationStrategy(map[EVALUATION_STRATEGY] as String)
                .typeFullName(map[TYPE_FULL_NAME] as String)
                .lineNumber(Option.apply(map[LINE_NUMBER] as Int))
                .columnNumber(Option.apply(map[COLUMN_NUMBER] as Int))
                .order(map.getOrDefault(ORDER, -1) as Int)
            Modifier.Label() -> NewModifierBuilder()
                .modifierType(map[MODIFIER_TYPE] as String)
                .order(map.getOrDefault(ORDER, -1) as Int)
            Type.Label() -> NewTypeBuilder()
                .name(map.getOrDefault(NAME, "<empty>") as String)
                .fullName(map.getOrDefault(FULL_NAME, "<empty>") as String)
                .typeDeclFullName(map[TYPE_DECL_FULL_NAME] as String)
            TypeDecl.Label() -> NewTypeDeclBuilder()
                .astParentFullName(map.getOrDefault(AST_PARENT_FULL_NAME, "<empty>") as String)
                .astParentType(map.getOrDefault(AST_PARENT_TYPE, "<empty>") as String)
                .name(map.getOrDefault(NAME, "<empty>") as String)
                .filename(map.getOrDefault(FILENAME, "<empty>") as String)
                .fullName(map.getOrDefault(FULL_NAME, "<empty>") as String)
                .order(map.getOrDefault(ORDER, -1) as Int)
                .isExternal(map.getOrDefault(IS_EXTERNAL, false) as Boolean)
            TypeParameter.Label() -> NewTypeParameterBuilder()
                .name(map.getOrDefault(NAME, "<empty>") as String)
                .order(map.getOrDefault(ORDER, -1) as Int)
            TypeArgument.Label() -> NewTypeArgumentBuilder()
                .order(map.getOrDefault(ORDER, -1) as Int)
            Member.Label() -> NewMemberBuilder()
                .typeFullName(map.getOrDefault(TYPE_FULL_NAME, "<empty>") as String)
                .code(map.getOrDefault(CODE, "<empty>") as String)
                .name(map.getOrDefault(NAME, "<empty>") as String)
                .order(map.getOrDefault(ORDER, -1) as Int)
            Namespace.Label() -> NewNamespaceBuilder()
                .order(map.getOrDefault(ORDER, -1) as Int)
                .name(map.getOrDefault(NAME, "<empty>") as String)
            NamespaceBlock.Label() -> NewNamespaceBlockBuilder()
                .fullName(map.getOrDefault(FULL_NAME, "<empty>") as String)
                .filename(map.getOrDefault(FILENAME, "<empty>") as String)
                .name(map.getOrDefault(NAME, "<empty>") as String)
                .order(map.getOrDefault(ORDER, -1) as Int)
            Literal.Label() -> NewLiteralBuilder()
                .typeFullName(map.getOrDefault(TYPE_FULL_NAME, "<empty>") as String)
                .code(map.getOrDefault(CODE, "<empty>") as String)
                .order(map.getOrDefault(ORDER, -1) as Int)
                .lineNumber(Option.apply(map[LINE_NUMBER] as Int))
                .columnNumber(Option.apply(map[COLUMN_NUMBER] as Int))
                .argumentIndex(map.getOrDefault(ARGUMENT_INDEX, -1) as Int)
            Call.Label() -> NewCallBuilder()
                .code(map.getOrDefault(CODE, "<empty>") as String)
                .name(map.getOrDefault(NAME, "<empty>") as String)
                .order(map.getOrDefault(ORDER, -1) as Int)
                .lineNumber(Option.apply(map[LINE_NUMBER] as Int))
                .columnNumber(Option.apply(map[COLUMN_NUMBER] as Int))
                .argumentIndex(map.getOrDefault(ARGUMENT_INDEX, -1) as Int)
                .signature(map.getOrDefault(SIGNATURE, "") as String)
                .dispatchType(map.getOrDefault(DISPATCH_TYPE, "<empty>") as String)
                .methodFullName(map.getOrDefault(METHOD_FULL_NAME, "<empty>") as String)
            Local.Label() -> NewLocalBuilder()
                .typeFullName(map.getOrDefault(TYPE_FULL_NAME, "<empty>") as String)
                .code(map.getOrDefault(CODE, "<empty>") as String)
                .name(map.getOrDefault(NAME, "<empty>") as String)
                .order(map.getOrDefault(ORDER, -1) as Int)
                .lineNumber(Option.apply(map[LINE_NUMBER] as Int))
                .columnNumber(Option.apply(map[COLUMN_NUMBER] as Int))
            Identifier.Label() -> NewIdentifierBuilder()
                .typeFullName(map.getOrDefault(TYPE_FULL_NAME, "<empty>") as String)
                .code(map.getOrDefault(CODE, "<empty>") as String)
                .name(map.getOrDefault(NAME, "<empty>") as String)
                .order(map.getOrDefault(ORDER, -1) as Int)
                .lineNumber(Option.apply(map[LINE_NUMBER] as Int))
                .columnNumber(Option.apply(map[COLUMN_NUMBER] as Int))
                .argumentIndex(map.getOrDefault(ARGUMENT_INDEX, -1) as Int)
            FieldIdentifier.Label() -> NewFieldIdentifierBuilder()
                .canonicalName(map[CANONICAL_NAME] as String)
                .code(map.getOrDefault(CODE, "<empty>") as String)
                .order(map.getOrDefault(ORDER, -1) as Int)
                .lineNumber(Option.apply(map[LINE_NUMBER] as Int))
                .columnNumber(Option.apply(map[COLUMN_NUMBER] as Int))
                .argumentIndex(map.getOrDefault(ARGUMENT_INDEX, -1) as Int)
            Return.Label() -> NewReturnBuilder()
                .code(map.getOrDefault(CODE, "<empty>") as String)
                .order(map.getOrDefault(ORDER, -1) as Int)
                .lineNumber(Option.apply(map[LINE_NUMBER] as Int))
                .columnNumber(Option.apply(map[COLUMN_NUMBER] as Int))
                .argumentIndex(map.getOrDefault(ARGUMENT_INDEX, -1) as Int)
            Block.Label() -> NewBlockBuilder()
                .typeFullName(map.getOrDefault(TYPE_FULL_NAME, "<empty>") as String)
                .code(map.getOrDefault(CODE, "<empty>") as String)
                .order(map.getOrDefault(ORDER, -1) as Int)
                .lineNumber(Option.apply(map[LINE_NUMBER] as Int))
                .columnNumber(Option.apply(map[COLUMN_NUMBER] as Int))
                .argumentIndex(map.getOrDefault(ARGUMENT_INDEX, -1) as Int)
            MethodRef.Label() -> NewMethodRefBuilder()
                .code(map.getOrDefault(CODE, "<empty>") as String)
                .order(map.getOrDefault(ORDER, -1) as Int)
                .methodFullName(map.getOrDefault(METHOD_FULL_NAME, "<empty>") as String)
                .lineNumber(Option.apply(map[LINE_NUMBER] as Int))
                .columnNumber(Option.apply(map[COLUMN_NUMBER] as Int))
                .argumentIndex(map.getOrDefault(ARGUMENT_INDEX, -1) as Int)
            TypeRef.Label() -> NewTypeRefBuilder()
                .typeFullName(map.getOrDefault(TYPE_FULL_NAME, "<empty>") as String)
                .code(map.getOrDefault(CODE, "<empty>") as String)
                .order(map.getOrDefault(ORDER, -1) as Int)
                .lineNumber(Option.apply(map[LINE_NUMBER] as Int))
                .columnNumber(Option.apply(map[COLUMN_NUMBER] as Int))
                .argumentIndex(map.getOrDefault(ARGUMENT_INDEX, -1) as Int)
            JumpTarget.Label() -> NewJumpTargetBuilder()
                .code(map.getOrDefault(CODE, "<empty>") as String)
                .order(map.getOrDefault(ORDER, -1) as Int)
                .lineNumber(Option.apply(map[LINE_NUMBER] as Int))
                .columnNumber(Option.apply(map[COLUMN_NUMBER] as Int))
                .argumentIndex(map.getOrDefault(ARGUMENT_INDEX, -1) as Int)
                .name(map.getOrDefault(NAME, "<empty>") as String)
            ControlStructure.Label() -> NewControlStructureBuilder()
                .controlStructureType(map[CONTROL_STRUCTURE_TYPE] as String)
                .code(map.getOrDefault(CODE, "<empty>") as String)
                .order(map.getOrDefault(ORDER, -1) as Int)
                .lineNumber(Option.apply(map[LINE_NUMBER] as Int))
                .columnNumber(Option.apply(map[COLUMN_NUMBER] as Int))
                .argumentIndex(map.getOrDefault(ARGUMENT_INDEX, -1) as Int)
            else -> NewUnknownBuilder()
                .code(map.getOrDefault(CODE, "<empty>") as String)
                .order(map.getOrDefault(ORDER, -1) as Int)
                .argumentIndex(map.getOrDefault(ARGUMENT_INDEX, -1) as Int)
                .typeFullName(map.getOrDefault(TYPE_FULL_NAME, "<empty>") as String)
                .lineNumber(Option.apply(map[LINE_NUMBER] as Int))
                .columnNumber(Option.apply(map[COLUMN_NUMBER] as Int))
        }.apply { if (map.containsKey("id")) this.id(map["id"] as Long) }
    }

    /**
     * Removes properties not used by Plume.
     */
    fun stripUnusedProperties(label: String, map: MutableMap<String, Any>): MutableMap<String, Any> {
        return when (label) {
            CONTROL_STRUCTURE -> map.apply { remove(PARSER_TYPE_NAME) }
            JUMP_TARGET -> map.apply { remove(PARSER_TYPE_NAME) }
            METHOD_REF -> map.apply { remove(TYPE_FULL_NAME) }
            METHOD -> map.apply { remove(IS_VARIADIC) }
            METHOD_PARAMETER_IN -> map.apply { remove(IS_VARIADIC) }
            METHOD_PARAMETER_OUT -> map.apply { remove(IS_VARIADIC) }
            NodeTypes.UNKNOWN -> map.apply { remove(CONTAINED_REF); remove(PARSER_TYPE_NAME) }
            else -> map
        }
    }

    /**
     * Checks if the given edge complies with the CPG schema given the from and two vertices.
     *
     * @param fromV The vertex from which the edge connects from.
     * @param toV The vertex to which the edge connects to.
     * @param edge the edge label between the two vertices.
     * @return true if the edge complies with the CPG schema, false if otherwise.
     */
    fun checkSchemaConstraints(fromV: NewNodeBuilder<out NewNode>, toV: NewNodeBuilder<out NewNode>, edge: String) =
        checkSchemaConstraints(fromV.build().label(), toV.build().label(), edge)

    /**
     * Checks if the given edge complies with the CPG schema given the from and two vertices.
     *
     * @param fromLabel The vertex label from which the edge connects from.
     * @param toLabel The vertex label to which the edge connects to.
     * @param edge The edge label between the two vertices.
     * @param silent If true, will not log any warnings.
     * @return true if the edge complies with the CPG schema, false if otherwise.
     */
    fun checkSchemaConstraints(fromLabel: String, toLabel: String, edge: String, silent: Boolean = false): Boolean {
        val outRule = when (fromLabel) {
            MetaData.Label() -> MetaData.`Edges$`.`MODULE$`.Out().contains(edge)
            File.Label() -> File.`Edges$`.`MODULE$`.Out().contains(edge)
            Method.Label() -> Method.`Edges$`.`MODULE$`.Out().contains(edge)
            MethodParameterIn.Label() -> MethodParameterIn.`Edges$`.`MODULE$`.Out().contains(edge)
            MethodParameterOut.Label() -> MethodParameterOut.`Edges$`.`MODULE$`.Out().contains(edge)
            MethodReturn.Label() -> MethodReturn.`Edges$`.`MODULE$`.Out().contains(edge)
            Modifier.Label() -> Modifier.`Edges$`.`MODULE$`.Out().contains(edge)
            Type.Label() -> Type.`Edges$`.`MODULE$`.Out().contains(edge)
            TypeDecl.Label() -> TypeDecl.`Edges$`.`MODULE$`.Out().contains(edge)
            TypeParameter.Label() -> TypeParameter.`Edges$`.`MODULE$`.Out().contains(edge)
            TypeArgument.Label() -> TypeArgument.`Edges$`.`MODULE$`.Out().contains(edge)
            Member.Label() -> Member.`Edges$`.`MODULE$`.Out().contains(edge)
            Namespace.Label() -> Namespace.`Edges$`.`MODULE$`.Out().contains(edge)
            NamespaceBlock.Label() -> NamespaceBlock.`Edges$`.`MODULE$`.Out().contains(edge)
            Literal.Label() -> Literal.`Edges$`.`MODULE$`.Out().contains(edge)
            Call.Label() -> Call.`Edges$`.`MODULE$`.Out().contains(edge)
            Local.Label() -> Local.`Edges$`.`MODULE$`.Out().contains(edge)
            Identifier.Label() -> Identifier.`Edges$`.`MODULE$`.Out().contains(edge)
            FieldIdentifier.Label() -> FieldIdentifier.`Edges$`.`MODULE$`.Out().contains(edge)
            Return.Label() -> Return.`Edges$`.`MODULE$`.Out().contains(edge)
            Block.Label() -> Block.`Edges$`.`MODULE$`.Out().contains(edge)
            MethodRef.Label() -> MethodRef.`Edges$`.`MODULE$`.Out().contains(edge)
            TypeRef.Label() -> TypeRef.`Edges$`.`MODULE$`.Out().contains(edge)
            JumpTarget.Label() -> JumpTarget.`Edges$`.`MODULE$`.Out().contains(edge)
            ControlStructure.Label() -> ControlStructure.`Edges$`.`MODULE$`.Out().contains(edge)
            Unknown.Label() -> Unknown.`Edges$`.`MODULE$`.Out().contains(edge)
            else -> {
                if (!silent) logger.warn("Unknown node label $fromLabel"); false
            }
        }
        val toRule = when (toLabel) {
            MetaData.Label() -> MetaData.`Edges$`.`MODULE$`.In().contains(edge)
            File.Label() -> File.`Edges$`.`MODULE$`.In().contains(edge)
            Method.Label() -> Method.`Edges$`.`MODULE$`.In().contains(edge)
            MethodParameterIn.Label() -> MethodParameterIn.`Edges$`.`MODULE$`.In().contains(edge)
            MethodParameterOut.Label() -> MethodParameterOut.`Edges$`.`MODULE$`.In().contains(edge)
            MethodReturn.Label() -> MethodReturn.`Edges$`.`MODULE$`.In().contains(edge)
            Modifier.Label() -> Modifier.`Edges$`.`MODULE$`.In().contains(edge)
            Type.Label() -> Type.`Edges$`.`MODULE$`.In().contains(edge)
            TypeDecl.Label() -> TypeDecl.`Edges$`.`MODULE$`.In().contains(edge)
            TypeParameter.Label() -> TypeParameter.`Edges$`.`MODULE$`.In().contains(edge)
            TypeArgument.Label() -> TypeArgument.`Edges$`.`MODULE$`.In().contains(edge)
            Member.Label() -> Member.`Edges$`.`MODULE$`.In().contains(edge)
            Namespace.Label() -> Namespace.`Edges$`.`MODULE$`.In().contains(edge)
            NamespaceBlock.Label() -> NamespaceBlock.`Edges$`.`MODULE$`.In().contains(edge)
            Literal.Label() -> Literal.`Edges$`.`MODULE$`.In().contains(edge)
            Call.Label() -> Call.`Edges$`.`MODULE$`.In().contains(edge)
            Local.Label() -> Local.`Edges$`.`MODULE$`.In().contains(edge)
            Identifier.Label() -> Identifier.`Edges$`.`MODULE$`.In().contains(edge)
            FieldIdentifier.Label() -> FieldIdentifier.`Edges$`.`MODULE$`.In().contains(edge)
            Return.Label() -> Return.`Edges$`.`MODULE$`.In().contains(edge)
            Block.Label() -> Block.`Edges$`.`MODULE$`.In().contains(edge)
            MethodRef.Label() -> MethodRef.`Edges$`.`MODULE$`.In().contains(edge)
            TypeRef.Label() -> TypeRef.`Edges$`.`MODULE$`.In().contains(edge)
            JumpTarget.Label() -> JumpTarget.`Edges$`.`MODULE$`.In().contains(edge)
            ControlStructure.Label() -> ControlStructure.`Edges$`.`MODULE$`.In().contains(edge)
            Unknown.Label() -> Unknown.`Edges$`.`MODULE$`.In().contains(edge)
            else -> {
                if (!silent) logger.warn("Unknown node label $fromLabel"); false
            }
        }
        return outRule && toRule
    }

    /**
     * All Scala sequences or lists are converted to strings in this map.
     */
    fun prepareListsInMap(propertyMap: MutableMap<String, Any>): MutableMap<String, Any> {
        val attributes = mutableMapOf<String, Any>()
        propertyMap.forEach { (key, value) ->
            when (value) {
                is `$colon$colon`<*> -> attributes[key] = ListMapper.scalaListToString(value)
                is `Nil$` -> attributes[key] = ListMapper.scalaListToString(value)
                else -> attributes[key] = value
            }
        }
        return attributes
    }
}