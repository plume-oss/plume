package io.github.plume.oss.domain.mappers

import io.github.plume.oss.util.SootToPlumeUtil.createScalaList
import io.shiftleft.codepropertygraph.generated.NodeKeyNames.*
import io.shiftleft.codepropertygraph.generated.nodes.*
import overflowdb.Node
import scala.Option
import scala.collection.immutable.`$colon$colon`
import scala.collection.immutable.`Nil$`
import scala.jdk.CollectionConverters
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
     * Converts a [NewNode] to its respective [NewNodeBuilder] object.
     *
     * @param v The [NewNode] to deserialize.
     * @return a [NewNodeBuilder] represented by the information in the givennode.
     */
    fun mapToVertex(v: NewNode): NewNodeBuilder {
        val map = CollectionConverters.MapHasAsJava(v.properties()).asJava() + mapOf<String, Any>("label" to v.label())
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
        // Only ID should be left as Long
        mapToConvert.keys.forEach {
            when (val value = mapToConvert[it]) {
                is Long -> if (it != "id") map[it] = value.toInt() else map[it] = value as Any
                else -> map[it] = value as Any
            }
        }
        return when (map["label"] as String) {
            ArrayInitializer.Label() -> NewArrayInitializerBuilder()
                .order(map[ORDER] as Int)
            Binding.Label() -> NewBindingBuilder()
                .name(map[NAME] as String)
                .signature(map[SIGNATURE] as String)
            MetaData.Label() -> NewMetaDataBuilder()
                .language(map[LANGUAGE] as String)
                .version(map[VERSION] as String)
            File.Label() -> NewFileBuilder()
                .name(map[NAME] as String)
                .hash(Option.apply(map[HASH] as String))
                .order(map[ORDER] as Int)
            Method.Label() -> NewMethodBuilder()
                .astParentFullName(map[AST_PARENT_FULL_NAME] as String)
                .astParentType(map[AST_PARENT_TYPE] as String)
                .name(map[NAME] as String)
                .code(map[CODE] as String)
                .fullName(map[FULL_NAME] as String)
                .signature(map[SIGNATURE] as String)
                .lineNumber(Option.apply(map[LINE_NUMBER] as Int))
                .columnNumber(Option.apply(map[COLUMN_NUMBER] as Int))
                .order(map[ORDER] as Int)
            MethodParameterIn.Label() -> NewMethodParameterInBuilder()
                .code(map[CODE] as String)
                .name(map[NAME] as String)
                .evaluationStrategy(map[EVALUATION_STRATEGY] as String)
                .typeFullName(map[TYPE_FULL_NAME] as String)
                .lineNumber(Option.apply(map[LINE_NUMBER] as Int))
                .columnNumber(Option.apply(map[COLUMN_NUMBER] as Int))
                .order(map[ORDER] as Int)
            MethodReturn.Label() -> NewMethodReturnBuilder()
                .code(map[CODE] as String)
                .evaluationStrategy(map[EVALUATION_STRATEGY] as String)
                .typeFullName(map[TYPE_FULL_NAME] as String)
                .lineNumber(Option.apply(map[LINE_NUMBER] as Int))
                .columnNumber(Option.apply(map[COLUMN_NUMBER] as Int))
                .order(map[ORDER] as Int)
            Modifier.Label() -> NewModifierBuilder()
                .modifierType(map[MODIFIER_TYPE] as String)
                .order(map[ORDER] as Int)
            Type.Label() -> NewTypeBuilder()
                .name(map[NAME] as String)
                .fullName(map[FULL_NAME] as String)
                .typeDeclFullName(map[TYPE_DECL_FULL_NAME] as String)
            TypeDecl.Label() -> NewTypeDeclBuilder()
                .astParentFullName(map[AST_PARENT_FULL_NAME] as String)
                .astParentType(map[AST_PARENT_TYPE] as String)
                .name(map[NAME] as String)
                .fullName(map[FULL_NAME] as String)
                .order(map[ORDER] as Int)
                .isExternal(map[IS_EXTERNAL] as Boolean)
            TypeParameter.Label() -> NewTypeParameterBuilder()
                .name(map[NAME] as String)
                .order(map[ORDER] as Int)
            TypeArgument.Label() -> NewTypeArgumentBuilder()
                .order(map[ORDER] as Int)
            Member.Label() -> NewMemberBuilder()
                .typeFullName(map[TYPE_FULL_NAME] as String)
                .code(map[CODE] as String)
                .name(map[NAME] as String)
                .order(map[ORDER] as Int)
            Namespace.Label() -> NewNamespaceBuilder()
                .order(map[ORDER] as Int)
                .name(map[NAME] as String)
            NamespaceBlock.Label() -> NewNamespaceBlockBuilder()
                .fullName(map[FULL_NAME] as String)
                .filename(map[FILENAME] as String)
                .name(map[NAME] as String)
                .order(map[ORDER] as Int)
            Literal.Label() -> NewLiteralBuilder()
                .typeFullName(map[TYPE_FULL_NAME] as String)
                .code(map[CODE] as String)
                .order(map[ORDER] as Int)
                .lineNumber(Option.apply(map[LINE_NUMBER] as Int))
                .columnNumber(Option.apply(map[COLUMN_NUMBER] as Int))
                .argumentIndex(map[ARGUMENT_INDEX] as Int)
            Call.Label() -> NewCallBuilder()
                .typeFullName(map[TYPE_FULL_NAME] as String)
                .code(map[CODE] as String)
                .name(map[NAME] as String)
                .order(map[ORDER] as Int)
                .lineNumber(Option.apply(map[LINE_NUMBER] as Int))
                .columnNumber(Option.apply(map[COLUMN_NUMBER] as Int))
                .argumentIndex(map[ARGUMENT_INDEX] as Int)
                .signature(map[SIGNATURE] as String)
                .dispatchType(map[DISPATCH_TYPE] as String)
                .methodFullName(map[METHOD_FULL_NAME] as String)
                .dynamicTypeHintFullName(
                    when (map[DYNAMIC_TYPE_HINT_FULL_NAME]) {
                        is String -> createScalaList(map[DYNAMIC_TYPE_HINT_FULL_NAME] as String)
                        is `$colon$colon`<*> -> createScalaList((map[DYNAMIC_TYPE_HINT_FULL_NAME] as `$colon$colon`<*>).head() as String)
                        else -> createScalaList("")
                    }
                )
            Local.Label() -> NewLocalBuilder()
                .typeFullName(map[TYPE_FULL_NAME] as String)
                .code(map[CODE] as String)
                .name(map[NAME] as String)
                .order(map[ORDER] as Int)
                .lineNumber(Option.apply(map[LINE_NUMBER] as Int))
                .columnNumber(Option.apply(map[COLUMN_NUMBER] as Int))
            Identifier.Label() -> NewIdentifierBuilder()
                .typeFullName(map[TYPE_FULL_NAME] as String)
                .code(map[CODE] as String)
                .name(map[NAME] as String)
                .order(map[ORDER] as Int)
                .lineNumber(Option.apply(map[LINE_NUMBER] as Int))
                .columnNumber(Option.apply(map[COLUMN_NUMBER] as Int))
                .argumentIndex(map[ARGUMENT_INDEX] as Int)
            FieldIdentifier.Label() -> NewFieldIdentifierBuilder()
                .canonicalName(map[CANONICAL_NAME] as String)
                .code(map[CODE] as String)
                .order(map[ORDER] as Int)
                .lineNumber(Option.apply(map[LINE_NUMBER] as Int))
                .columnNumber(Option.apply(map[COLUMN_NUMBER] as Int))
                .argumentIndex(map[ARGUMENT_INDEX] as Int)
            Return.Label() -> NewReturnBuilder()
                .code(map[CODE] as String)
                .order(map[ORDER] as Int)
                .lineNumber(Option.apply(map[LINE_NUMBER] as Int))
                .columnNumber(Option.apply(map[COLUMN_NUMBER] as Int))
                .argumentIndex(map[ARGUMENT_INDEX] as Int)
            Block.Label() -> NewBlockBuilder()
                .typeFullName(map[TYPE_FULL_NAME] as String)
                .code(map[CODE] as String)
                .order(map[ORDER] as Int)
                .lineNumber(Option.apply(map[LINE_NUMBER] as Int))
                .columnNumber(Option.apply(map[COLUMN_NUMBER] as Int))
                .argumentIndex(map[ARGUMENT_INDEX] as Int)
            MethodRef.Label() -> NewMethodRefBuilder()
                .code(map[CODE] as String)
                .order(map[ORDER] as Int)
                .methodFullName(map[METHOD_FULL_NAME] as String)
                .methodInstFullName(Option.apply(map[METHOD_INST_FULL_NAME] as String))
                .lineNumber(Option.apply(map[LINE_NUMBER] as Int))
                .columnNumber(Option.apply(map[COLUMN_NUMBER] as Int))
                .argumentIndex(map[ARGUMENT_INDEX] as Int)
            TypeRef.Label() -> NewTypeRefBuilder()
                .typeFullName(map[TYPE_FULL_NAME] as String)
                .dynamicTypeHintFullName(
                    when (map[DYNAMIC_TYPE_HINT_FULL_NAME]) {
                        is String -> createScalaList(map[DYNAMIC_TYPE_HINT_FULL_NAME] as String)
                        is `$colon$colon`<*> -> createScalaList((map[DYNAMIC_TYPE_HINT_FULL_NAME] as `$colon$colon`<*>).head() as String)
                        else -> createScalaList("")
                    }
                )
                .code(map[CODE] as String)
                .order(map[ORDER] as Int)
                .lineNumber(Option.apply(map[LINE_NUMBER] as Int))
                .columnNumber(Option.apply(map[COLUMN_NUMBER] as Int))
                .argumentIndex(map[ARGUMENT_INDEX] as Int)
            JumpTarget.Label() -> NewJumpTargetBuilder()
                .code(map[CODE] as String)
                .order(map[ORDER] as Int)
                .lineNumber(Option.apply(map[LINE_NUMBER] as Int))
                .columnNumber(Option.apply(map[COLUMN_NUMBER] as Int))
                .argumentIndex(map[ARGUMENT_INDEX] as Int)
                .name(map[NAME] as String)
            ControlStructure.Label() -> NewControlStructureBuilder()
                .code(map[CODE] as String)
                .order(map[ORDER] as Int)
                .lineNumber(Option.apply(map[LINE_NUMBER] as Int))
                .columnNumber(Option.apply(map[COLUMN_NUMBER] as Int))
                .argumentIndex(map[ARGUMENT_INDEX] as Int)
            else -> NewUnknownBuilder()
                .code(map[CODE] as String)
                .order(map[ORDER] as Int)
                .argumentIndex(map[ARGUMENT_INDEX] as Int)
                .typeFullName(map[TYPE_FULL_NAME] as String)
                .lineNumber(Option.apply(map[LINE_NUMBER] as Int))
                .columnNumber(Option.apply(map[COLUMN_NUMBER] as Int))
        }.apply { if (map.containsKey("id")) this.id(map["id"] as Long) }
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
            else -> Unknown.`Edges$`.`MODULE$`.In().contains(edge)
        }
        return outRule && toRule
    }

    fun extractAttributesFromMap(propertyMap: MutableMap<String, Any>): MutableMap<String, Any> {
        val attributes = mutableMapOf<String, Any>()
        propertyMap.computeIfPresent(DYNAMIC_TYPE_HINT_FULL_NAME) { _, value ->
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