package io.github.plume.oss.domain.mappers

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
        return when (map["label"] as String) {
            ArrayInitializer.Label() -> NewArrayInitializerBuilder()
                .order(map["ORDER"] as Int)
                .id(map["id"] as Long)
            Binding.Label() -> NewBindingBuilder()
                .name(map["NAME"] as String)
                .signature(map["SIGNATURE"] as String)
                .id(map["id"] as Long)
            MetaData.Label() -> NewMetaDataBuilder()
                .language(map["LANGUAGE"] as String)
                .version(map["VERSION"] as String)
                .id(map["id"] as Long)
            File.Label() -> NewFileBuilder()
                .name(map["NAME"] as String)
                .hash(Option.apply(map["HASH"] as String))
                .order(map["ORDER"] as Int)
                .id(map["id"] as Long)
            Method.Label() -> NewMethodBuilder()
                .astparentfullname(map["AST_PARENT_FULL_NAME"] as String)
                .astparenttype(map["AST_PARENT_TYPE"] as String)
                .name(map["NAME"] as String)
                .code(map["CODE"] as String)
                .fullname(map["FULL_NAME"] as String)
                .signature(map["SIGNATURE"] as String)
                .linenumber(Option.apply(map["LINE_NUMBER"] as Int))
                .columnnumber(Option.apply(map["COLUMN_NUMBER"] as Int))
                .order(map["ORDER"] as Int)
                .id(map["id"] as Long)
            MethodParameterIn.Label() -> NewMethodParameterInBuilder()
                .code(map["CODE"] as String)
                .name(map["NAME"] as String)
                .evaluationstrategy(map["EVALUATION_STRATEGY"] as String)
                .typefullname(map["TYPE_FULL_NAME"] as String)
                .linenumber(Option.apply(map["LINE_NUMBER"] as Int))
                .columnnumber(Option.apply(map["COLUMN_NUMBER"] as Int))
                .order(map["ORDER"] as Int)
                .id(map["id"] as Long)
            MethodReturn.Label() -> NewMethodReturnBuilder()
                .code(map["CODE"] as String)
                .evaluationstrategy(map["EVALUATION_STRATEGY"] as String)
                .typefullname(map["TYPE_FULL_NAME"] as String)
                .linenumber(Option.apply(map["LINE_NUMBER"] as Int))
                .columnnumber(Option.apply(map["COLUMN_NUMBER"] as Int))
                .order(map["ORDER"] as Int)
                .id(map["id"] as Long)
            Modifier.Label() -> NewModifierBuilder()
                .modifiertype(map["MODIFIER_TYPE"] as String)
                .order(map["ORDER"] as Int)
                .id(map["id"] as Long)
            Type.Label() -> NewTypeBuilder()
                .name(map["NAME"] as String)
                .fullname(map["FULL_NAME"] as String)
                .typedeclfullname(map["TYPE_DECL_FULL_NAME"] as String)
                .id(map["id"] as Long)
            TypeDecl.Label() -> NewTypeDeclBuilder()
                .astparentfullname(map["AST_PARENT_FULL_NAME"] as String)
                .astparenttype(map["AST_PARENT_TYPE"] as String)
                .name(map["NAME"] as String)
                .fullname(map["FULL_NAME"] as String)
                .order(map["ORDER"] as Int)
                .isexternal(map["IS_EXTERNAL"] as Boolean)
                .id(map["id"] as Long)
            TypeParameter.Label() -> NewTypeParameterBuilder()
                .name(map["NAME"] as String)
                .order(map["ORDER"] as Int)
                .id(map["id"] as Long)
            TypeArgument.Label() -> NewTypeArgumentBuilder()
                .order(map["ORDER"] as Int)
                .id(map["id"] as Long)
            Member.Label() -> NewMemberBuilder()
                .typefullname(map["TYPE_FULL_NAME"] as String)
                .code(map["CODE"] as String)
                .name(map["NAME"] as String)
                .order(map["ORDER"] as Int)
                .id(map["id"] as Long)
            NamespaceBlock.Label() -> NewNamespaceBlockBuilder()
                .fullname(map["FULL_NAME"] as String)
                .filename(map["FILENAME"] as String)
                .name(map["NAME"] as String)
                .order(map["ORDER"] as Int)
                .id(map["id"] as Long)
            Literal.Label() -> NewLiteralBuilder()
                .typefullname(map["TYPE_FULL_NAME"] as String)
                .code(map["CODE"] as String)
                .order(map["ORDER"] as Int)
                .linenumber(Option.apply(map["LINE_NUMBER"] as Int))
                .columnnumber(Option.apply(map["COLUMN_NUMBER"] as Int))
                .argumentindex(map["ARGUMENT_INDEX"] as Int)
                .id(map["id"] as Long)
            Call.Label() -> NewCallBuilder()
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
            Local.Label() -> NewLocalBuilder()
                .typefullname(map["TYPE_FULL_NAME"] as String)
                .code(map["CODE"] as String)
                .name(map["NAME"] as String)
                .order(map["ORDER"] as Int)
                .linenumber(Option.apply(map["LINE_NUMBER"] as Int))
                .columnnumber(Option.apply(map["COLUMN_NUMBER"] as Int))
                .id(map["id"] as Long)
            Identifier.Label() -> NewIdentifierBuilder()
                .typefullname(map["TYPE_FULL_NAME"] as String)
                .code(map["CODE"] as String)
                .name(map["NAME"] as String)
                .order(map["ORDER"] as Int)
                .linenumber(Option.apply(map["LINE_NUMBER"] as Int))
                .columnnumber(Option.apply(map["COLUMN_NUMBER"] as Int))
                .argumentindex(map["ARGUMENT_INDEX"] as Int)
                .id(map["id"] as Long)
            FieldIdentifier.Label() -> NewFieldIdentifierBuilder()
                .canonicalname(map["CANONICAL_NAME"] as String)
                .code(map["CODE"] as String)
                .order(map["ORDER"] as Int)
                .linenumber(Option.apply(map["LINE_NUMBER"] as Int))
                .columnnumber(Option.apply(map["COLUMN_NUMBER"] as Int))
                .argumentindex(map["ARGUMENT_INDEX"] as Int)
                .id(map["id"] as Long)
            Return.Label() -> NewReturnBuilder()
                .code(map["CODE"] as String)
                .order(map["ORDER"] as Int)
                .linenumber(Option.apply(map["LINE_NUMBER"] as Int))
                .columnnumber(Option.apply(map["COLUMN_NUMBER"] as Int))
                .argumentindex(map["ARGUMENT_INDEX"] as Int)
                .id(map["id"] as Long)
            Block.Label() -> NewBlockBuilder()
                .typefullname(map["TYPE_FULL_NAME"] as String)
                .code(map["CODE"] as String)
                .order(map["ORDER"] as Int)
                .linenumber(Option.apply(map["LINE_NUMBER"] as Int))
                .columnnumber(Option.apply(map["COLUMN_NUMBER"] as Int))
                .argumentindex(map["ARGUMENT_INDEX"] as Int)
                .id(map["id"] as Long)
            MethodRef.Label() -> NewMethodRefBuilder()
                .code(map["CODE"] as String)
                .order(map["ORDER"] as Int)
                .methodfullname(map["METHOD_FULL_NAME"] as String)
                .methodinstfullname(Option.apply(map["METHOD_INST_FULL_NAME"] as String))
                .linenumber(Option.apply(map["LINE_NUMBER"] as Int))
                .columnnumber(Option.apply(map["COLUMN_NUMBER"] as Int))
                .argumentindex(map["ARGUMENT_INDEX"] as Int)
                .id(map["id"] as Long)
            TypeRef.Label() -> NewTypeRefBuilder()
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
            JumpTarget.Label() -> NewJumpTargetBuilder()
                .code(map["CODE"] as String)
                .order(map["ORDER"] as Int)
                .linenumber(Option.apply(map["LINE_NUMBER"] as Int))
                .columnnumber(Option.apply(map["COLUMN_NUMBER"] as Int))
                .argumentindex(map["ARGUMENT_INDEX"] as Int)
                .name(map["NAME"] as String)
                .id(map["id"] as Long)
            ControlStructure.Label() -> NewControlStructureBuilder()
                .code(map["CODE"] as String)
                .order(map["ORDER"] as Int)
                .linenumber(Option.apply(map["LINE_NUMBER"] as Int))
                .columnnumber(Option.apply(map["COLUMN_NUMBER"] as Int))
                .argumentindex(map["ARGUMENT_INDEX"] as Int)
                .id(map["id"] as Long)
            else -> NewUnknownBuilder()
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
    fun checkSchemaConstraints(fromV: NewNodeBuilder, toV: NewNodeBuilder, edge: String) =
        checkSchemaConstraints(fromV.build().label(), edge, toV.build().label())

    /**
     * Checks if the given edge complies with the CPG schema given the from and two vertices.
     *
     * @param fromLabel The vertex label from which the edge connects from.
     * @param toLabel The vertex label to which the edge connects to.
     * @param edge the edge label between the two vertices.
     * @return true if the edge complies with the CPG schema, false if otherwise.
     */
    fun checkSchemaConstraints(fromLabel: String, edge: String, toLabel: String): Boolean {
        val outRule = when (fromLabel) {
            ArrayInitializer.Label() -> ArrayInitializer.`Edges$`.`MODULE$`.Out().contains(edge)
            Binding.Label() -> Binding.`Edges$`.`MODULE$`.Out().contains(edge)
            MetaData.Label() -> MetaData.`Edges$`.`MODULE$`.Out().contains(edge)
            File.Label() -> File.`Edges$`.`MODULE$`.Out().contains(edge)
            Method.Label() -> Method.`Edges$`.`MODULE$`.Out().contains(edge)
            MethodParameterIn.Label() -> MethodParameterIn.`Edges$`.`MODULE$`.Out().contains(edge)
            MethodReturn.Label() -> MethodReturn.`Edges$`.`MODULE$`.Out().contains(edge)
            Modifier.Label() -> Modifier.`Edges$`.`MODULE$`.Out().contains(edge)
            Type.Label() -> Type.`Edges$`.`MODULE$`.Out().contains(edge)
            TypeDecl.Label() -> TypeDecl.`Edges$`.`MODULE$`.Out().contains(edge)
            TypeParameter.Label() -> TypeParameter.`Edges$`.`MODULE$`.Out().contains(edge)
            TypeArgument.Label() -> TypeArgument.`Edges$`.`MODULE$`.Out().contains(edge)
            Member.Label() -> Member.`Edges$`.`MODULE$`.Out().contains(edge)
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
            else -> Unknown.`Edges$`.`MODULE$`.Out().contains(edge)
        }
        val toRule = when (toLabel) {
            ArrayInitializer.Label() -> ArrayInitializer.`Edges$`.`MODULE$`.In().contains(edge)
            Binding.Label() -> Binding.`Edges$`.`MODULE$`.In().contains(edge)
            MetaData.Label() -> MetaData.`Edges$`.`MODULE$`.In().contains(edge)
            File.Label() -> File.`Edges$`.`MODULE$`.In().contains(edge)
            Method.Label() -> Method.`Edges$`.`MODULE$`.In().contains(edge)
            MethodParameterIn.Label() -> MethodParameterIn.`Edges$`.`MODULE$`.In().contains(edge)
            MethodReturn.Label() -> MethodReturn.`Edges$`.`MODULE$`.In().contains(edge)
            Modifier.Label() -> Modifier.`Edges$`.`MODULE$`.In().contains(edge)
            Type.Label() -> Type.`Edges$`.`MODULE$`.In().contains(edge)
            TypeDecl.Label() -> TypeDecl.`Edges$`.`MODULE$`.In().contains(edge)
            TypeParameter.Label() -> TypeParameter.`Edges$`.`MODULE$`.In().contains(edge)
            TypeArgument.Label() -> TypeArgument.`Edges$`.`MODULE$`.In().contains(edge)
            Member.Label() -> Member.`Edges$`.`MODULE$`.In().contains(edge)
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
            else -> Unknown.`Edges$`.`MODULE$`.In().contains(edge)
        }
        return outRule && toRule
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
                "POLICY_DIRECTORIES" -> Optional.empty()
                "INHERITS_FROM_TYPE_FULL_NAME" -> Optional.empty()
                "OVERLAYS" -> Optional.empty()
                else -> Optional.of(it.key)
            }
            if (key.isPresent) attributes[key.get()] = it.value
        }
        return attributes
    }
}