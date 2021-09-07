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
                .name(map.getOrDefault(NAME, getPropertyDefault(NAME)) as String)
                .hash(Option.apply(map.getOrDefault(HASH, UNKNOWN) as String))
                .order(map.getOrDefault(ORDER, getPropertyDefault(ORDER)) as Int)
            Method.Label() -> NewMethodBuilder()
                .astParentFullName(map.getOrDefault(AST_PARENT_FULL_NAME, getPropertyDefault(AST_PARENT_FULL_NAME)) as String)
                .astParentType(map.getOrDefault(AST_PARENT_TYPE, getPropertyDefault(AST_PARENT_TYPE)) as String)
                .name(map.getOrDefault(NAME, getPropertyDefault(NAME)) as String)
                .code(map.getOrDefault(CODE, getPropertyDefault(CODE)) as String)
                .isExternal(map.getOrDefault(IS_EXTERNAL, getPropertyDefault(IS_EXTERNAL)) as Boolean)
                .fullName(map.getOrDefault(FULL_NAME, getPropertyDefault(NAME)) as String)
                .filename(map.getOrDefault(FILENAME, getPropertyDefault(NAME)) as String)
                .signature(map.getOrDefault(SIGNATURE, "") as String)
                .lineNumber(Option.apply(map[LINE_NUMBER] as Int))
                .columnNumber(Option.apply(map[COLUMN_NUMBER] as Int))
                .order(map.getOrDefault(ORDER, getPropertyDefault(ORDER)) as Int)
                .hash(Option.apply(map[HASH] as String))
            MethodParameterIn.Label() -> NewMethodParameterInBuilder()
                .code(map.getOrDefault(CODE, getPropertyDefault(CODE)) as String)
                .name(map.getOrDefault(NAME, getPropertyDefault(NAME)) as String)
                .evaluationStrategy(map[EVALUATION_STRATEGY] as String)
                .typeFullName(map.getOrDefault(TYPE_FULL_NAME, getPropertyDefault(TYPE_FULL_NAME)) as String)
                .lineNumber(Option.apply(map[LINE_NUMBER] as Int))
                .columnNumber(Option.apply(map[COLUMN_NUMBER] as Int))
                .order(map.getOrDefault(ORDER, getPropertyDefault(ORDER)) as Int)
            MethodParameterOut.Label() -> NewMethodParameterOutBuilder()
                .code(map.getOrDefault(CODE, getPropertyDefault(CODE)) as String)
                .name(map.getOrDefault(NAME, getPropertyDefault(NAME)) as String)
                .evaluationStrategy(map[EVALUATION_STRATEGY] as String)
                .typeFullName(map.getOrDefault(TYPE_FULL_NAME, getPropertyDefault(TYPE_FULL_NAME)) as String)
                .lineNumber(Option.apply(map[LINE_NUMBER] as Int))
                .columnNumber(Option.apply(map[COLUMN_NUMBER] as Int))
                .order(map.getOrDefault(ORDER, getPropertyDefault(ORDER)) as Int)
            MethodReturn.Label() -> NewMethodReturnBuilder()
                .code(map.getOrDefault(CODE, getPropertyDefault(CODE)) as String)
                .evaluationStrategy(map[EVALUATION_STRATEGY] as String)
                .typeFullName(map.getOrDefault(TYPE_FULL_NAME, getPropertyDefault(TYPE_FULL_NAME)) as String)
                .lineNumber(Option.apply(map[LINE_NUMBER] as Int))
                .columnNumber(Option.apply(map[COLUMN_NUMBER] as Int))
                .order(map.getOrDefault(ORDER, getPropertyDefault(ORDER)) as Int)
            Modifier.Label() -> NewModifierBuilder()
                .modifierType(map[MODIFIER_TYPE] as String)
                .order(map.getOrDefault(ORDER, getPropertyDefault(ORDER)) as Int)
            Type.Label() -> NewTypeBuilder()
                .name(map.getOrDefault(NAME, getPropertyDefault(NAME)) as String)
                .fullName(map.getOrDefault(FULL_NAME, getPropertyDefault(FULL_NAME)) as String)
                .typeDeclFullName(map.getOrDefault(TYPE_DECL_FULL_NAME, getPropertyDefault(TYPE_DECL_FULL_NAME)) as String)
            TypeDecl.Label() -> NewTypeDeclBuilder()
                .astParentFullName(map.getOrDefault(AST_PARENT_FULL_NAME, getPropertyDefault(AST_PARENT_FULL_NAME)) as String)
                .astParentType(map.getOrDefault(AST_PARENT_TYPE, getPropertyDefault(AST_PARENT_TYPE)) as String)
                .name(map.getOrDefault(NAME, getPropertyDefault(NAME)) as String)
                .filename(map.getOrDefault(FILENAME, getPropertyDefault(FILENAME)) as String)
                .fullName(map.getOrDefault(FULL_NAME, getPropertyDefault(FULL_NAME)) as String)
                .order(map.getOrDefault(ORDER, getPropertyDefault(ORDER)) as Int)
                .isExternal(map.getOrDefault(IS_EXTERNAL, getPropertyDefault(IS_EXTERNAL)) as Boolean)
            TypeParameter.Label() -> NewTypeParameterBuilder()
                .name(map.getOrDefault(NAME, getPropertyDefault(NAME)) as String)
                .order(map.getOrDefault(ORDER, getPropertyDefault(ORDER)) as Int)
            TypeArgument.Label() -> NewTypeArgumentBuilder()
                .order(map.getOrDefault(ORDER, getPropertyDefault(ORDER)) as Int)
            Member.Label() -> NewMemberBuilder()
                .typeFullName(map.getOrDefault(TYPE_FULL_NAME, getPropertyDefault(TYPE_FULL_NAME)) as String)
                .code(map.getOrDefault(CODE, getPropertyDefault(CODE)) as String)
                .name(map.getOrDefault(NAME, getPropertyDefault(NAME)) as String)
                .order(map.getOrDefault(ORDER, getPropertyDefault(ORDER)) as Int)
                .lineNumber(Option.apply(map[LINE_NUMBER] as Int))
                .columnNumber(Option.apply(map[COLUMN_NUMBER] as Int))
            Namespace.Label() -> NewNamespaceBuilder()
                .order(map.getOrDefault(ORDER, getPropertyDefault(ORDER)) as Int)
                .name(map.getOrDefault(NAME, getPropertyDefault(NAME)) as String)
            NamespaceBlock.Label() -> NewNamespaceBlockBuilder()
                .fullName(map.getOrDefault(FULL_NAME, getPropertyDefault(FULL_NAME)) as String)
                .filename(map.getOrDefault(FILENAME, getPropertyDefault(FILENAME)) as String)
                .name(map.getOrDefault(NAME, getPropertyDefault(NAME)) as String)
                .order(map.getOrDefault(ORDER, getPropertyDefault(ORDER)) as Int)
            Literal.Label() -> NewLiteralBuilder()
                .typeFullName(map.getOrDefault(TYPE_FULL_NAME, getPropertyDefault(TYPE_FULL_NAME)) as String)
                .code(map.getOrDefault(CODE, getPropertyDefault(CODE)) as String)
                .order(map.getOrDefault(ORDER, getPropertyDefault(ORDER)) as Int)
                .lineNumber(Option.apply(map[LINE_NUMBER] as Int))
                .columnNumber(Option.apply(map[COLUMN_NUMBER] as Int))
                .argumentIndex(map.getOrDefault(ARGUMENT_INDEX, getPropertyDefault(ARGUMENT_INDEX)) as Int)
            Call.Label() -> NewCallBuilder()
                .code(map.getOrDefault(CODE, getPropertyDefault(CODE)) as String)
                .name(map.getOrDefault(NAME, getPropertyDefault(NAME)) as String)
                .order(map.getOrDefault(ORDER, getPropertyDefault(ORDER)) as Int)
                .lineNumber(Option.apply(map[LINE_NUMBER] as Int))
                .columnNumber(Option.apply(map[COLUMN_NUMBER] as Int))
                .argumentIndex(map.getOrDefault(ARGUMENT_INDEX, getPropertyDefault(ARGUMENT_INDEX)) as Int)
                .signature(map.getOrDefault(SIGNATURE, "") as String)
                .dispatchType(map.getOrDefault(DISPATCH_TYPE, getPropertyDefault(DISPATCH_TYPE)) as String)
                .methodFullName(map.getOrDefault(METHOD_FULL_NAME, getPropertyDefault(METHOD_FULL_NAME)) as String)
            Local.Label() -> NewLocalBuilder()
                .typeFullName(map.getOrDefault(TYPE_FULL_NAME, getPropertyDefault(TYPE_FULL_NAME)) as String)
                .code(map.getOrDefault(CODE, getPropertyDefault(CODE)) as String)
                .name(map.getOrDefault(NAME, getPropertyDefault(NAME)) as String)
                .order(map.getOrDefault(ORDER, getPropertyDefault(ORDER)) as Int)
                .lineNumber(Option.apply(map[LINE_NUMBER] as Int))
                .columnNumber(Option.apply(map[COLUMN_NUMBER] as Int))
            Identifier.Label() -> NewIdentifierBuilder()
                .typeFullName(map.getOrDefault(TYPE_FULL_NAME, getPropertyDefault(TYPE_FULL_NAME)) as String)
                .code(map.getOrDefault(CODE, getPropertyDefault(CODE)) as String)
                .name(map.getOrDefault(NAME, getPropertyDefault(NAME)) as String)
                .order(map.getOrDefault(ORDER, getPropertyDefault(ORDER)) as Int)
                .lineNumber(Option.apply(map[LINE_NUMBER] as Int))
                .columnNumber(Option.apply(map[COLUMN_NUMBER] as Int))
                .argumentIndex(map.getOrDefault(ARGUMENT_INDEX, getPropertyDefault(ARGUMENT_INDEX)) as Int)
            FieldIdentifier.Label() -> NewFieldIdentifierBuilder()
                .canonicalName(map[CANONICAL_NAME] as String)
                .code(map.getOrDefault(CODE, getPropertyDefault(CODE)) as String)
                .order(map.getOrDefault(ORDER, getPropertyDefault(ORDER)) as Int)
                .lineNumber(Option.apply(map[LINE_NUMBER] as Int))
                .columnNumber(Option.apply(map[COLUMN_NUMBER] as Int))
                .argumentIndex(map.getOrDefault(ARGUMENT_INDEX, getPropertyDefault(ARGUMENT_INDEX)) as Int)
            Return.Label() -> NewReturnBuilder()
                .code(map.getOrDefault(CODE, getPropertyDefault(CODE)) as String)
                .order(map.getOrDefault(ORDER, getPropertyDefault(ORDER)) as Int)
                .lineNumber(Option.apply(map[LINE_NUMBER] as Int))
                .columnNumber(Option.apply(map[COLUMN_NUMBER] as Int))
                .argumentIndex(map.getOrDefault(ARGUMENT_INDEX, getPropertyDefault(ARGUMENT_INDEX)) as Int)
            Block.Label() -> NewBlockBuilder()
                .typeFullName(map.getOrDefault(TYPE_FULL_NAME, getPropertyDefault(TYPE_FULL_NAME)) as String)
                .code(map.getOrDefault(CODE, getPropertyDefault(CODE)) as String)
                .order(map.getOrDefault(ORDER, getPropertyDefault(ORDER)) as Int)
                .lineNumber(Option.apply(map[LINE_NUMBER] as Int))
                .columnNumber(Option.apply(map[COLUMN_NUMBER] as Int))
                .argumentIndex(map.getOrDefault(ARGUMENT_INDEX, getPropertyDefault(ARGUMENT_INDEX)) as Int)
            MethodRef.Label() -> NewMethodRefBuilder()
                .code(map.getOrDefault(CODE, getPropertyDefault(CODE)) as String)
                .order(map.getOrDefault(ORDER, getPropertyDefault(ORDER)) as Int)
                .methodFullName(map.getOrDefault(METHOD_FULL_NAME, getPropertyDefault(METHOD_FULL_NAME)) as String)
                .lineNumber(Option.apply(map[LINE_NUMBER] as Int))
                .columnNumber(Option.apply(map[COLUMN_NUMBER] as Int))
                .argumentIndex(map.getOrDefault(ARGUMENT_INDEX, getPropertyDefault(ARGUMENT_INDEX)) as Int)
            TypeRef.Label() -> NewTypeRefBuilder()
                .typeFullName(map.getOrDefault(TYPE_FULL_NAME, getPropertyDefault(TYPE_FULL_NAME)) as String)
                .code(map.getOrDefault(CODE, getPropertyDefault(CODE)) as String)
                .order(map.getOrDefault(ORDER, getPropertyDefault(ORDER)) as Int)
                .lineNumber(Option.apply(map[LINE_NUMBER] as Int))
                .columnNumber(Option.apply(map[COLUMN_NUMBER] as Int))
                .argumentIndex(map.getOrDefault(ARGUMENT_INDEX, getPropertyDefault(ARGUMENT_INDEX)) as Int)
            JumpTarget.Label() -> NewJumpTargetBuilder()
                .code(map.getOrDefault(CODE, getPropertyDefault(CODE)) as String)
                .order(map.getOrDefault(ORDER, getPropertyDefault(ORDER)) as Int)
                .lineNumber(Option.apply(map[LINE_NUMBER] as Int))
                .columnNumber(Option.apply(map[COLUMN_NUMBER] as Int))
                .argumentIndex(map.getOrDefault(ARGUMENT_INDEX, getPropertyDefault(ARGUMENT_INDEX)) as Int)
                .name(map.getOrDefault(NAME, getPropertyDefault(NAME)) as String)
            ControlStructure.Label() -> NewControlStructureBuilder()
                .controlStructureType(map[CONTROL_STRUCTURE_TYPE] as String)
                .code(map.getOrDefault(CODE, getPropertyDefault(CODE)) as String)
                .order(map.getOrDefault(ORDER, getPropertyDefault(ORDER)) as Int)
                .lineNumber(Option.apply(map[LINE_NUMBER] as Int))
                .columnNumber(Option.apply(map[COLUMN_NUMBER] as Int))
                .argumentIndex(map.getOrDefault(ARGUMENT_INDEX, getPropertyDefault(ARGUMENT_INDEX)) as Int)
            else -> NewUnknownBuilder()
                .code(map.getOrDefault(CODE, getPropertyDefault(CODE)) as String)
                .order(map.getOrDefault(ORDER, getPropertyDefault(ORDER)) as Int)
                .argumentIndex(map.getOrDefault(ARGUMENT_INDEX, getPropertyDefault(ARGUMENT_INDEX)) as Int)
                .typeFullName(map.getOrDefault(TYPE_FULL_NAME, getPropertyDefault(TYPE_FULL_NAME)) as String)
                .lineNumber(Option.apply(map[LINE_NUMBER] as Int))
                .columnNumber(Option.apply(map[COLUMN_NUMBER] as Int))
        }.apply { if (map.containsKey("id")) this.id(map["id"] as Long) }
    }

    /**
     * Given a property, returns its known default.
     */
    private fun getPropertyDefault(prop: String): Comparable<*> {
        val strDefault = "<empty>"
        val intDefault = -1
        val boolDefault = false
        return when (prop) {
            AST_PARENT_TYPE -> strDefault
            AST_PARENT_FULL_NAME -> strDefault
            NAME -> strDefault
            CODE -> strDefault
            ORDER -> intDefault
            SIGNATURE -> ""
            ARGUMENT_INDEX -> intDefault
            FULL_NAME -> strDefault
            TYPE_FULL_NAME -> strDefault
            TYPE_DECL_FULL_NAME -> strDefault
            TYPE_DECL -> strDefault
            IS_EXTERNAL -> boolDefault
            DISPATCH_TYPE -> strDefault
            LINE_NUMBER -> intDefault
            COLUMN_NUMBER -> intDefault
            LINE_NUMBER_END -> intDefault
            COLUMN_NUMBER_END -> intDefault
            else -> strDefault
        }
    }

    /**
     * Removes properties not used by Plume and explicitly adds defaults.
     */
    fun handleProperties(label: String, map: MutableMap<String, Any>): MutableMap<String, Any> {
        return when (label) {
            META_DATA -> map.apply {
                MetaData.`PropertyNames$`.`MODULE$`.allAsJava().forEach { prop ->
                    if (!this.containsKey(prop)) this[prop] = getPropertyDefault(prop)
                }
                remove(OVERLAYS)
            }
            FILE -> map.apply {
                File.`PropertyNames$`.`MODULE$`.allAsJava().forEach { prop ->
                    if (!this.containsKey(prop)) this[prop] = getPropertyDefault(prop)
                }
                remove(LINE_NUMBER)
                remove(COLUMN_NUMBER)
            }
            METHOD -> map.apply {
                Method.`PropertyNames$`.`MODULE$`.allAsJava().forEach { prop ->
                    if (!this.containsKey(prop)) this[prop] = getPropertyDefault(prop)
                }
                remove(IS_VARIADIC)
            }
            METHOD_PARAMETER_IN -> map.apply {
                MethodParameterIn.`PropertyNames$`.`MODULE$`.allAsJava().forEach { prop ->
                    if (!this.containsKey(prop)) this[prop] = getPropertyDefault(prop)
                }
                remove(IS_VARIADIC)
            }
            METHOD_PARAMETER_OUT -> map.apply {
                MethodParameterOut.`PropertyNames$`.`MODULE$`.allAsJava().forEach { prop ->
                    if (!this.containsKey(prop)) this[prop] = getPropertyDefault(prop)
                }
                remove(IS_VARIADIC)
            }
            METHOD_RETURN -> map.apply {
                MethodReturn.`PropertyNames$`.`MODULE$`.allAsJava().forEach { prop ->
                    if (!this.containsKey(prop)) this[prop] = getPropertyDefault(prop)
                }
            }
            MODIFIER -> map.apply {
                Modifier.`PropertyNames$`.`MODULE$`.allAsJava().forEach { prop ->
                    if (!this.containsKey(prop)) this[prop] = getPropertyDefault(prop)
                }
                remove(LINE_NUMBER)
                remove(LINE_NUMBER)
            }
            TYPE -> map.apply {
                Type.`PropertyNames$`.`MODULE$`.allAsJava().forEach { prop ->
                    if (!this.containsKey(prop)) this[prop] = getPropertyDefault(prop)
                }
            }
            TYPE_DECL -> map.apply {
                TypeDecl.`PropertyNames$`.`MODULE$`.allAsJava().forEach { prop ->
                    if (!this.containsKey(prop)) this[prop] = getPropertyDefault(prop)
                }
                remove(LINE_NUMBER)
                remove(COLUMN_NUMBER)
            }
            TYPE_PARAMETER -> map.apply {
                TypeParameter.`PropertyNames$`.`MODULE$`.allAsJava().forEach { prop ->
                    if (!this.containsKey(prop)) this[prop] = getPropertyDefault(prop)
                }
                remove(LINE_NUMBER)
                remove(COLUMN_NUMBER)
            }
            TYPE_ARGUMENT -> map.apply {
                TypeArgument.`PropertyNames$`.`MODULE$`.allAsJava().forEach { prop ->
                    if (!this.containsKey(prop)) this[prop] = getPropertyDefault(prop)
                }
                remove(LINE_NUMBER)
                remove(COLUMN_NUMBER)
            }
            MEMBER -> map.apply {
                Member.`PropertyNames$`.`MODULE$`.allAsJava().forEach { prop ->
                    if (!this.containsKey(prop)) this[prop] = getPropertyDefault(prop)
                }
            }
            NAMESPACE -> map.apply {
                Namespace.`PropertyNames$`.`MODULE$`.allAsJava().forEach { prop ->
                    if (!this.containsKey(prop)) this[prop] = getPropertyDefault(prop)
                }
                remove(LINE_NUMBER)
                remove(COLUMN_NUMBER)
            }
            NAMESPACE_BLOCK -> map.apply {
                NamespaceBlock.`PropertyNames$`.`MODULE$`.allAsJava().forEach { prop ->
                    if (!this.containsKey(prop)) this[prop] = getPropertyDefault(prop)
                }
                remove(LINE_NUMBER)
                remove(COLUMN_NUMBER)
            }
            LITERAL -> map.apply {
                Literal.`PropertyNames$`.`MODULE$`.allAsJava().forEach { prop ->
                    if (!this.containsKey(prop)) this[prop] = getPropertyDefault(prop)
                }
            }
            CALL -> map.apply {
                Call.`PropertyNames$`.`MODULE$`.allAsJava().forEach { prop ->
                    if (!this.containsKey(prop)) this[prop] = getPropertyDefault(prop)
                }
            }
            LOCAL -> map.apply {
                Local.`PropertyNames$`.`MODULE$`.allAsJava().forEach { prop ->
                    if (!this.containsKey(prop)) this[prop] = getPropertyDefault(prop)
                }
            }
            IDENTIFIER -> map.apply {
                Identifier.`PropertyNames$`.`MODULE$`.allAsJava().forEach { prop ->
                    if (!this.containsKey(prop)) this[prop] = getPropertyDefault(prop)
                }
            }
            FIELD_IDENTIFIER -> map.apply {
                FieldIdentifier.`PropertyNames$`.`MODULE$`.allAsJava().forEach { prop ->
                    if (!this.containsKey(prop)) this[prop] = getPropertyDefault(prop)
                }
            }
            RETURN -> map.apply {
                Return.`PropertyNames$`.`MODULE$`.allAsJava().forEach { prop ->
                    if (!this.containsKey(prop)) this[prop] = getPropertyDefault(prop)
                }
            }
            BLOCK -> map.apply {
                Block.`PropertyNames$`.`MODULE$`.allAsJava().forEach { prop ->
                    if (!this.containsKey(prop)) this[prop] = getPropertyDefault(prop)
                }
            }
            METHOD_REF -> map.apply {
                MethodRef.`PropertyNames$`.`MODULE$`.allAsJava().forEach { prop ->
                    if (!this.containsKey(prop)) this[prop] = getPropertyDefault(prop)
                }
                remove(TYPE_FULL_NAME)
            }
            TYPE_REF -> map.apply {
                TypeRef.`PropertyNames$`.`MODULE$`.allAsJava().forEach { prop ->
                    if (!this.containsKey(prop)) this[prop] = getPropertyDefault(prop)
                }
            }
            JUMP_TARGET -> map.apply {
                JumpTarget.`PropertyNames$`.`MODULE$`.allAsJava().forEach { prop ->
                    if (!this.containsKey(prop)) this[prop] = getPropertyDefault(prop)
                }
                remove(PARSER_TYPE_NAME)
            }
            CONTROL_STRUCTURE -> map.apply {
                ControlStructure.`PropertyNames$`.`MODULE$`.allAsJava().forEach { prop ->
                    if (!this.containsKey(prop)) this[prop] = getPropertyDefault(prop)
                }
                remove(PARSER_TYPE_NAME)
            }
            NodeTypes.UNKNOWN -> map.apply {
                Unknown.`PropertyNames$`.`MODULE$`.allAsJava().forEach { prop ->
                    if (!this.containsKey(prop)) this[prop] = getPropertyDefault(prop)
                }
                remove(CONTAINED_REF)
                remove(PARSER_TYPE_NAME)
            }
            else -> map
        }.apply {
            remove(LINE_NUMBER_END)
            remove(COLUMN_NUMBER_END)
            remove(DYNAMIC_TYPE_HINT_FULL_NAME)
            remove(INHERITS_FROM_TYPE_FULL_NAME)
            remove(ALIAS_TYPE_FULL_NAME)
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