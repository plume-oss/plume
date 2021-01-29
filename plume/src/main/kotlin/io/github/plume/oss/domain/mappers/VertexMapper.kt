package io.github.plume.oss.domain.mappers

import io.github.plume.oss.domain.enums.EdgeLabel
import io.github.plume.oss.domain.enums.VertexLabel
import io.github.plume.oss.domain.enums.VertexLabel.*
import io.github.plume.oss.util.SootToPlumeUtil.createScalaList
import io.shiftleft.codepropertygraph.generated.nodes.*
import overflowdb.Node
import scala.Option
import scala.collection.immutable.`$colon$colon`
import scala.collection.immutable.`Nil$`
import java.util.*
import kotlin.collections.HashMap


/**
 * Responsible for marshalling and unmarshalling vertex properties to and from [NewNode] objects to [Map] objects.
 */
object VertexMapper {

    /**
     * Converts a [Node] to its respective [NewNodeBuilder] object.
     *
     * @param v The [Node] to deserialize.
     * @return a [NewNodeBuilder] represented by the information in the givennode.
     */
    fun mapToVertex(v: Node): NewNodeBuilder {
        val map = v.propertyMap() + mapOf<String, Any>("id" to v.id(), "label" to v.label())
        return mapToVertex(map)
    }

    /**
     * Converts a [Map] containing vertex properties to its respective [NewNodeBuilder] object.
     *
     * @param mapToConvert The [Map] to deserialize.
     * @return a [NewNodeBuilder] represented by the information in the given map.
     */
    fun mapToVertex(mapToConvert: Map<String, Any>): NewNodeBuilder {
        val map = HashMap<String, Any>()
        mapToConvert.keys.forEach {
            when (val value = mapToConvert[it]) {
                is Long -> map[it] = value.toInt()
                else -> map[it] = value as Any
            }
        }
        map.computeIfPresent("id") { _, v -> v.toString().toLong() }
        return when (valueOf(map["label"] as String)) {
            ARRAY_INITIALIZER -> NewArrayInitializerBuilder()
                .order(map["ORDER"] as Int)
                .id(map["id"] as Long)
            BINDING -> NewBindingBuilder()
                .name(map["NAME"] as String)
                .signature(map["SIGNATURE"] as String)
                .id(map["id"] as Long)
            META_DATA -> NewMetaDataBuilder()
                .language(map["LANGUAGE"] as String)
                .version(map["VERSION"] as String)
                .id(map["id"] as Long)
            FILE -> NewFileBuilder()
                .name(map["NAME"] as String)
                .hash(Option.apply(map["HASH"] as String))
                .order(map["ORDER"] as Int)
                .id(map["id"] as Long)
            METHOD -> NewMethodBuilder()
                .name(map["NAME"] as String)
                .code(map["CODE"] as String)
                .fullname(map["FULL_NAME"] as String)
                .signature(map["SIGNATURE"] as String)
                .linenumber(Option.apply(map["LINE_NUMBER"] as Int))
                .columnnumber(Option.apply(map["COLUMN_NUMBER"] as Int))
                .order(map["ORDER"] as Int)
                .id(map["id"] as Long)
            METHOD_PARAMETER_IN -> NewMethodParameterInBuilder()
                .code(map["CODE"] as String)
                .name(map["NAME"] as String)
                .evaluationstrategy(map["EVALUATION_STRATEGY"] as String)
                .typefullname(map["TYPE_FULL_NAME"] as String)
                .linenumber(Option.apply(map["LINE_NUMBER"] as Int))
                .columnnumber(Option.apply(map["COLUMN_NUMBER"] as Int))
                .order(map["ORDER"] as Int)
                .id(map["id"] as Long)
            METHOD_RETURN -> NewMethodReturnBuilder()
                .code(map["CODE"] as String)
                .evaluationstrategy(map["EVALUATION_STRATEGY"] as String)
                .typefullname(map["TYPE_FULL_NAME"] as String)
                .linenumber(Option.apply(map["LINE_NUMBER"] as Int))
                .columnnumber(Option.apply(map["COLUMN_NUMBER"] as Int))
                .order(map["ORDER"] as Int)
                .id(map["id"] as Long)
            MODIFIER -> NewModifierBuilder()
                .modifiertype(map["MODIFIER_TYPE"] as String)
                .order(map["ORDER"] as Int)
                .id(map["id"] as Long)
            TYPE -> NewTypeBuilder()
                .name(map["NAME"] as String)
                .fullname(map["FULL_NAME"] as String)
                .typedeclfullname(map["TYPE_DECL_FULL_NAME"] as String)
                .id(map["id"] as Long)
            TYPE_DECL -> NewTypeDeclBuilder()
                .name(map["NAME"] as String)
                .fullname(map["FULL_NAME"] as String)
                .order(map["ORDER"] as Int)
                .id(map["id"] as Long)
            TYPE_PARAMETER -> NewTypeParameterBuilder()
                .name(map["NAME"] as String)
                .order(map["ORDER"] as Int)
                .id(map["id"] as Long)
            TYPE_ARGUMENT -> NewTypeArgumentBuilder()
                .order(map["ORDER"] as Int)
                .id(map["id"] as Long)
            MEMBER -> NewMemberBuilder()
                .typefullname(map["TYPE_FULL_NAME"] as String)
                .code(map["CODE"] as String)
                .name(map["NAME"] as String)
                .order(map["ORDER"] as Int)
                .id(map["id"] as Long)
            NAMESPACE_BLOCK -> NewNamespaceBlockBuilder()
                .fullname(map["FULL_NAME"] as String)
                .name(map["NAME"] as String)
                .order(map["ORDER"] as Int)
                .id(map["id"] as Long)
            LITERAL -> NewLiteralBuilder()
                .typefullname(map["TYPE_FULL_NAME"] as String)
                .code(map["CODE"] as String)
                .order(map["ORDER"] as Int)
                .linenumber(Option.apply(map["LINE_NUMBER"] as Int))
                .columnnumber(Option.apply(map["COLUMN_NUMBER"] as Int))
                .argumentindex(map["ARGUMENT_INDEX"] as Int)
                .id(map["id"] as Long)
            CALL -> NewCallBuilder()
                .typefullname(map["TYPE_FULL_NAME"] as String)
                .code(map["CODE"] as String)
                .name(map["NAME"] as String)
                .order(map["ORDER"] as Int)
                .linenumber(Option.apply(map["LINE_NUMBER"] as Int))
                .columnnumber(Option.apply(map["COLUMN_NUMBER"] as Int))
                .argumentindex(map["ARGUMENT_INDEX"] as Int)
                .signature(map["SIGNATURE"] as String)
                .dispatchtype(map["DISPATCH_TYPE"] as String)
                .methodfullname(map["METHOD_FULL_NAME"] as String)
                .dynamictypehintfullname(
                    when (map["DYNAMIC_TYPE_HINT_FULL_NAME"]) {
                        is String -> createScalaList(map["DYNAMIC_TYPE_HINT_FULL_NAME"] as String)
                        is `$colon$colon`<*> -> createScalaList((map["DYNAMIC_TYPE_HINT_FULL_NAME"] as `$colon$colon`<*>).head() as String)
                        else -> createScalaList("")
                    }
                )
                .id(map["id"] as Long)
            LOCAL -> NewLocalBuilder()
                .typefullname(map["TYPE_FULL_NAME"] as String)
                .code(map["CODE"] as String)
                .name(map["NAME"] as String)
                .order(map["ORDER"] as Int)
                .linenumber(Option.apply(map["LINE_NUMBER"] as Int))
                .columnnumber(Option.apply(map["COLUMN_NUMBER"] as Int))
                .id(map["id"] as Long)
            IDENTIFIER -> NewIdentifierBuilder()
                .typefullname(map["TYPE_FULL_NAME"] as String)
                .code(map["CODE"] as String)
                .name(map["NAME"] as String)
                .order(map["ORDER"] as Int)
                .linenumber(Option.apply(map["LINE_NUMBER"] as Int))
                .columnnumber(Option.apply(map["COLUMN_NUMBER"] as Int))
                .argumentindex(map["ARGUMENT_INDEX"] as Int)
                .id(map["id"] as Long)
            FIELD_IDENTIFIER -> NewFieldIdentifierBuilder()
                .canonicalname(map["CANONICAL_NAME"] as String)
                .code(map["CODE"] as String)
                .order(map["ORDER"] as Int)
                .linenumber(Option.apply(map["LINE_NUMBER"] as Int))
                .columnnumber(Option.apply(map["COLUMN_NUMBER"] as Int))
                .argumentindex(map["ARGUMENT_INDEX"] as Int)
                .id(map["id"] as Long)
            RETURN -> NewReturnBuilder()
                .code(map["CODE"] as String)
                .order(map["ORDER"] as Int)
                .linenumber(Option.apply(map["LINE_NUMBER"] as Int))
                .columnnumber(Option.apply(map["COLUMN_NUMBER"] as Int))
                .argumentindex(map["ARGUMENT_INDEX"] as Int)
                .id(map["id"] as Long)
            BLOCK -> NewBlockBuilder()
                .typefullname(map["TYPE_FULL_NAME"] as String)
                .code(map["CODE"] as String)
                .order(map["ORDER"] as Int)
                .linenumber(Option.apply(map["LINE_NUMBER"] as Int))
                .columnnumber(Option.apply(map["COLUMN_NUMBER"] as Int))
                .argumentindex(map["ARGUMENT_INDEX"] as Int)
                .id(map["id"] as Long)
            METHOD_REF -> NewMethodRefBuilder()
                .code(map["CODE"] as String)
                .order(map["ORDER"] as Int)
                .methodfullname(map["METHOD_FULL_NAME"] as String)
                .methodinstfullname(Option.apply(map["METHOD_INST_FULL_NAME"] as String))
                .linenumber(Option.apply(map["LINE_NUMBER"] as Int))
                .columnnumber(Option.apply(map["COLUMN_NUMBER"] as Int))
                .argumentindex(map["ARGUMENT_INDEX"] as Int)
                .id(map["id"] as Long)
            TYPE_REF -> NewTypeRefBuilder()
                .typefullname(map["TYPE_FULL_NAME"] as String)
                .dynamictypehintfullname(
                    when (map["DYNAMIC_TYPE_HINT_FULL_NAME"]) {
                        is String -> createScalaList(map["DYNAMIC_TYPE_HINT_FULL_NAME"] as String)
                        is `$colon$colon`<*> -> createScalaList((map["DYNAMIC_TYPE_HINT_FULL_NAME"] as `$colon$colon`<*>).head() as String)
                        else -> createScalaList("")
                    }
                )
                .code(map["CODE"] as String)
                .order(map["ORDER"] as Int)
                .linenumber(Option.apply(map["LINE_NUMBER"] as Int))
                .columnnumber(Option.apply(map["COLUMN_NUMBER"] as Int))
                .argumentindex(map["ARGUMENT_INDEX"] as Int)
                .id(map["id"] as Long)
            JUMP_TARGET -> NewJumpTargetBuilder()
                .code(map["CODE"] as String)
                .order(map["ORDER"] as Int)
                .linenumber(Option.apply(map["LINE_NUMBER"] as Int))
                .columnnumber(Option.apply(map["COLUMN_NUMBER"] as Int))
                .argumentindex(map["ARGUMENT_INDEX"] as Int)
                .name(map["NAME"] as String)
                .id(map["id"] as Long)
            CONTROL_STRUCTURE -> NewControlStructureBuilder()
                .code(map["CODE"] as String)
                .order(map["ORDER"] as Int)
                .linenumber(Option.apply(map["LINE_NUMBER"] as Int))
                .columnnumber(Option.apply(map["COLUMN_NUMBER"] as Int))
                .argumentindex(map["ARGUMENT_INDEX"] as Int)
                .id(map["id"] as Long)
            UNKNOWN -> NewUnknownBuilder()
                .code(map["CODE"] as String)
                .order(map["ORDER"] as Int)
                .argumentindex(map["ARGUMENT_INDEX"] as Int)
                .typefullname(map["TYPE_FULL_NAME"] as String)
                .linenumber(Option.apply(map["LINE_NUMBER"] as Int))
                .columnnumber(Option.apply(map["COLUMN_NUMBER"] as Int))
                .id(map["id"] as Long)
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
    fun checkSchemaConstraints(fromV: NewNodeBuilder, toV: NewNodeBuilder, edge: EdgeLabel): Boolean {
        val fromLabel: VertexLabel = valueOf(fromV.build().label())
        val toLabel: VertexLabel = valueOf(toV.build().label())
        return checkSchemaConstraints(fromLabel, edge, toLabel)
    }

    /**
     * Checks if the given edge complies with the CPG schema given the from and two vertices.
     *
     * @param fromLabel The vertex label from which the edge connects from.
     * @param toLabel The vertex label to which the edge connects to.
     * @param edge the edge label between the two vertices.
     * @return true if the edge complies with the CPG schema, false if otherwise.
     */
    fun checkSchemaConstraints(fromLabel: VertexLabel, edge: EdgeLabel, toLabel: VertexLabel): Boolean {
        // TODO: Check toLabel
        return when (fromLabel) {
            ARRAY_INITIALIZER -> ArrayInitializer.`Edges$`.`MODULE$`.Out().contains(edge.name)
            BINDING -> Binding.`Edges$`.`MODULE$`.Out().contains(edge.name)
            META_DATA -> MetaData.`Edges$`.`MODULE$`.Out().contains(edge.name)
            FILE -> File.`Edges$`.`MODULE$`.Out().contains(edge.name)
            METHOD -> Method.`Edges$`.`MODULE$`.Out().contains(edge.name)
            METHOD_PARAMETER_IN -> MethodParameterIn.`Edges$`.`MODULE$`.Out().contains(edge.name)
            METHOD_RETURN -> MethodReturn.`Edges$`.`MODULE$`.Out().contains(edge.name)
            MODIFIER -> Modifier.`Edges$`.`MODULE$`.Out().contains(edge.name)
            TYPE -> Type.`Edges$`.`MODULE$`.Out().contains(edge.name)
            TYPE_DECL -> TypeDecl.`Edges$`.`MODULE$`.Out().contains(edge.name)
            TYPE_PARAMETER -> TypeParameter.`Edges$`.`MODULE$`.Out().contains(edge.name)
            TYPE_ARGUMENT -> TypeArgument.`Edges$`.`MODULE$`.Out().contains(edge.name)
            MEMBER -> Member.`Edges$`.`MODULE$`.Out().contains(edge.name)
            NAMESPACE_BLOCK -> NamespaceBlock.`Edges$`.`MODULE$`.Out().contains(edge.name)
            LITERAL -> Literal.`Edges$`.`MODULE$`.Out().contains(edge.name)
            CALL -> Call.`Edges$`.`MODULE$`.Out().contains(edge.name)
            LOCAL -> Local.`Edges$`.`MODULE$`.Out().contains(edge.name)
            IDENTIFIER -> Identifier.`Edges$`.`MODULE$`.Out().contains(edge.name)
            FIELD_IDENTIFIER -> FieldIdentifier.`Edges$`.`MODULE$`.Out().contains(edge.name)
            RETURN -> Return.`Edges$`.`MODULE$`.Out().contains(edge.name)
            BLOCK -> Block.`Edges$`.`MODULE$`.Out().contains(edge.name)
            METHOD_REF -> MethodRef.`Edges$`.`MODULE$`.Out().contains(edge.name)
            TYPE_REF -> TypeRef.`Edges$`.`MODULE$`.Out().contains(edge.name)
            JUMP_TARGET -> JumpTarget.`Edges$`.`MODULE$`.Out().contains(edge.name)
            CONTROL_STRUCTURE -> ControlStructure.`Edges$`.`MODULE$`.Out().contains(edge.name)
            UNKNOWN -> Unknown.`Edges$`.`MODULE$`.Out().contains(edge.name)
        }
    }

    fun extractAttributesFromMap(propertyMap: MutableMap<String, Any>): MutableMap<String, Any> {
        val attributes = mutableMapOf<String, Any>()
        propertyMap.computeIfPresent("DYNAMIC_TYPE_HINT_FULL_NAME") { _, value ->
            when (value) {
                is `$colon$colon`<*> -> value.head()
                is `Nil$` -> ""
                else -> value
            }
        }
        propertyMap.forEach {
            val key: Optional<String> = when (it.key) {
                "PARSER_TYPE_NAME" -> Optional.empty()
                "AST_PARENT_TYPE" -> Optional.empty()
                "AST_PARENT_FULL_NAME" -> Optional.empty()
                "POLICY_DIRECTORIES" -> Optional.empty()
                "INHERITS_FROM_TYPE_FULL_NAME" -> Optional.empty()
                "OVERLAYS" -> Optional.empty()
                "FILENAME" -> Optional.empty()
                "IS_EXTERNAL" -> Optional.empty()
                else -> Optional.of(it.key)
            }
            if (key.isPresent) attributes[key.get()] = it.value
        }
        return attributes
    }
}