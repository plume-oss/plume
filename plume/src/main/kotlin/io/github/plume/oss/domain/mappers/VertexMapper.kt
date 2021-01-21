package io.github.plume.oss.domain.mappers

import io.github.plume.oss.domain.enums.EdgeLabel
import io.github.plume.oss.domain.enums.VertexLabel
import io.github.plume.oss.domain.enums.VertexLabel.*
import io.github.plume.oss.util.SootToPlumeUtil.createScalaList
import io.shiftleft.codepropertygraph.generated.nodes.*
import scala.Option
import scala.Some

/**
 * Responsible for marshalling and unmarshalling vertex properties to and from [NewNode] objects to [Map] objects.
 */
object VertexMapper {
    /**
     * Converts a [NewNode]'s properties to a key-value [Map].
     *
     * @param v The vertex to serialize.
     * @return a [MutableMap] of the vertex's
     */
    fun vertexToMap(v: NewNodeBuilder): MutableMap<String, Any> {
        val properties = mutableMapOf<String, Any>("id" to v.id())
        when (val node = v.build()) {
            is NewArrayInitializer -> {
                properties["label"] = ArrayInitializer.Label()
                properties["order"] = node.order()
            }
            is NewBinding -> {
                properties["label"] = Binding.Label()
                properties["name"] = node.name()
                properties["signature"] = node.signature()
            }
            is NewBlock -> {
                properties["label"] = Block.Label()
                properties["typeFullName"] = node.typeFullName()
                properties["order"] = node.order()
                properties["argumentIndex"] = node.argumentIndex()
                properties["code"] = node.code()
                properties["columnNumber"] = node.columnNumber().get()
                properties["lineNumber"] = node.lineNumber().get()
            }
            is NewCall -> {
                properties["label"] = Call.Label()
                properties["typeFullName"] = node.typeFullName()
                properties["order"] = node.order()
                properties["argumentIndex"] = node.argumentIndex()
                properties["code"] = node.code()
                properties["columnNumber"] = node.columnNumber().get()
                properties["lineNumber"] = node.lineNumber().get()
                properties["dispatchType"] = node.dispatchType()
                properties["dynamicTypeHintFullName"] = node.dynamicTypeHintFullName().head()
                properties["methodFullName"] = node.methodFullName()
                properties["signature"] = node.signature()
                properties["name"] = node.name()
            }
            is NewControlStructure -> {
                properties["label"] = ControlStructure.Label()
                properties["order"] = node.order()
                properties["argumentIndex"] = node.argumentIndex()
                properties["code"] = node.code()
                properties["columnNumber"] = node.columnNumber().get()
                properties["lineNumber"] = node.lineNumber().get()
            }
            is NewFieldIdentifier -> {
                properties["label"] = FieldIdentifier.Label()
                properties["order"] = node.order()
                properties["argumentIndex"] = node.argumentIndex()
                properties["code"] = node.code()
                properties["columnNumber"] = node.columnNumber().get()
                properties["lineNumber"] = node.lineNumber().get()
                properties["canonicalName"] = node.canonicalName()
            }
            is NewFile -> {
                properties["label"] = File.Label()
                properties["order"] = node.order()
                properties["name"] = node.name()
                properties["hash"] = node.hash().get()
            }
            is NewIdentifier -> {
                properties["label"] = Identifier.Label()
                properties["typeFullName"] = node.typeFullName()
                properties["order"] = node.order()
                properties["argumentIndex"] = node.argumentIndex()
                properties["code"] = node.code()
                properties["columnNumber"] = node.columnNumber().get()
                properties["lineNumber"] = node.lineNumber().get()
                properties["name"] = node.name()
            }
            is NewJumpTarget -> {
                properties["label"] = JumpTarget.Label()
                properties["order"] = node.order()
                properties["argumentIndex"] = node.argumentIndex()
                properties["code"] = node.code()
                properties["columnNumber"] = node.columnNumber().get()
                properties["lineNumber"] = node.lineNumber().get()
                properties["name"] = node.name()
            }
            is NewLiteral -> {
                properties["label"] = Literal.Label()
                properties["typeFullName"] = node.typeFullName()
                properties["order"] = node.order()
                properties["argumentIndex"] = node.argumentIndex()
                properties["code"] = node.code()
                properties["columnNumber"] = node.columnNumber().get()
                properties["lineNumber"] = node.lineNumber().get()
            }
            is NewLocal -> {
                properties["label"] = Local.Label()
                properties["typeFullName"] = node.typeFullName()
                properties["order"] = node.order()
                properties["code"] = node.code()
                properties["columnNumber"] = node.columnNumber().get()
                properties["lineNumber"] = node.lineNumber().get()
                properties["name"] = node.name()
            }
            is NewMember -> {
                properties["label"] = Member.Label()
                properties["typeFullName"] = node.typeFullName()
                properties["order"] = node.order()
                properties["code"] = node.code()
                properties["name"] = node.name()
            }
            is NewMetaData -> {
                properties["label"] = MetaData.Label()
                properties["language"] = node.language()
                properties["version"] = node.version()
            }
            is NewMethodParameterIn -> {
                properties["label"] = MethodParameterIn.Label()
                properties["typeFullName"] = node.typeFullName()
                properties["order"] = node.order()
                properties["code"] = node.code()
                properties["lineNumber"] = node.lineNumber().get()
                properties["columnNumber"] = node.columnNumber().get()
                properties["name"] = node.name()
                properties["evaluationStrategy"] = node.evaluationStrategy()
            }
            is NewMethodRef -> {
                properties["label"] = MethodRef.Label()
                properties["order"] = node.order()
                properties["argumentIndex"] = node.argumentIndex()
                properties["code"] = node.code()
                properties["columnNumber"] = node.columnNumber().get()
                properties["lineNumber"] = node.lineNumber().get()
                properties["methodFullName"] = node.methodFullName()
                properties["methodInstFullName"] = node.methodInstFullName().get()
            }
            is NewMethodReturn -> {
                properties["label"] = MethodReturn.Label()
                properties["typeFullName"] = node.typeFullName()
                properties["order"] = node.order()
                properties["code"] = node.code()
                properties["columnNumber"] = node.columnNumber().get()
                properties["lineNumber"] = node.lineNumber().get()
                properties["evaluationStrategy"] = node.evaluationStrategy()
            }
            is NewMethod -> {
                properties["label"] = Method.Label()
                properties["order"] = node.order()
                properties["code"] = node.code()
                properties["columnNumber"] = node.columnNumber().get()
                properties["lineNumber"] = node.lineNumber().get()
                properties["signature"] = node.signature()
                properties["name"] = node.name()
                properties["fullName"] = node.fullName()
            }
            is NewModifier -> {
                properties["label"] = Modifier.Label()
                properties["order"] = node.order()
                properties["modifierType"] = node.modifierType()
            }
            is NewNamespaceBlock -> {
                properties["label"] = NamespaceBlock.Label()
                properties["order"] = node.order()
                properties["name"] = node.name()
                properties["fullName"] = node.fullName()
            }
            is NewReturn -> {
                properties["label"] = Return.Label()
                properties["order"] = node.order()
                properties["argumentIndex"] = node.argumentIndex()
                properties["code"] = node.code()
                properties["columnNumber"] = node.columnNumber().get()
                properties["lineNumber"] = node.lineNumber().get()
            }
            is NewTypeArgument -> {
                properties["label"] = TypeArgument.Label()
                properties["order"] = node.order()
            }
            is NewTypeDecl -> {
                properties["label"] = TypeDecl.Label()
                properties["order"] = node.order()
                properties["name"] = node.name()
                properties["fullName"] = node.fullName()
            }
            is NewTypeParameter -> {
                properties["label"] = TypeParameter.Label()
                properties["order"] = node.order()
                properties["name"] = node.name()
            }
            is NewTypeRef -> {
                properties["label"] = TypeRef.Label()
                properties["typeFullName"] = node.typeFullName()
                properties["order"] = node.order()
                properties["argumentIndex"] = node.argumentIndex()
                properties["code"] = node.code()
                properties["columnNumber"] = node.columnNumber().get()
                properties["lineNumber"] = node.lineNumber().get()
                properties["dynamicTypeHintFullName"] = node.dynamicTypeHintFullName().head()
            }
            is NewType -> {
                properties["label"] = Type.Label()
                properties["name"] = node.name()
                properties["fullName"] = node.fullName()
                properties["typeDeclFullName"] = node.typeDeclFullName()
            }
            is NewUnknown -> {
                properties["label"] = Unknown.Label()
                properties["typeFullName"] = node.typeFullName()
                properties["order"] = node.order()
                properties["argumentIndex"] = node.argumentIndex()
                properties["code"] = node.code()
                properties["columnNumber"] = node.columnNumber().get()
                properties["lineNumber"] = node.lineNumber().get()
            }
        }
        return properties
    }

    /**
     * Converts a [Map] containing vertex properties to its respective [NewNode] object.
     *
     * @param mapToConvert The [Map] to deserialize.
     * @return a [NewNode] represented by the information in the given map.
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
                .order(map["order"] as Int)
                .id(map["id"] as Long)
            BINDING -> NewBindingBuilder()
                .name(map["name"] as String)
                .signature(map["signature"] as String)
                .id(map["id"] as Long)
            META_DATA -> NewMetaDataBuilder()
                .language(map["language"] as String)
                .version(map["version"] as String)
                .id(map["id"] as Long)
            FILE -> NewFileBuilder()
                .name(map["name"] as String)
                .hash(Option.apply(map["hash"] as String))
                .order(map["order"] as Int)
                .id(map["id"] as Long)
            METHOD -> NewMethodBuilder()
                .name(map["name"] as String)
                .code(map["code"] as String)
                .fullname(map["fullName"] as String)
                .signature(map["signature"] as String)
                .linenumber(Option.apply(map["lineNumber"] as Int))
                .columnnumber(Option.apply(map["columnNumber"] as Int))
                .order(map["order"] as Int)
                .id(map["id"] as Long)
            METHOD_PARAMETER_IN -> NewMethodParameterInBuilder()
                .code(map["code"] as String)
                .name(map["name"] as String)
                .evaluationstrategy(map["evaluationStrategy"] as String)
                .typefullname(map["typeFullName"] as String)
                .linenumber(Option.apply(map["lineNumber"] as Int))
                .columnnumber(Option.apply(map["columnNumber"] as Int))
                .order(map["order"] as Int)
                .id(map["id"] as Long)
            METHOD_RETURN -> NewMethodReturnBuilder()
                .code(map["code"] as String)
                .evaluationstrategy(map["evaluationStrategy"] as String)
                .typefullname(map["typeFullName"] as String)
                .linenumber(Option.apply(map["lineNumber"] as Int))
                .columnnumber(Option.apply(map["columnNumber"] as Int))
                .order(map["order"] as Int)
                .id(map["id"] as Long)
            MODIFIER -> NewModifierBuilder()
                .modifiertype(map["modifierType"] as String)
                .order(map["order"] as Int)
                .id(map["id"] as Long)
            TYPE -> NewTypeBuilder()
                .name(map["name"] as String)
                .fullname(map["fullName"] as String)
                .typedeclfullname(map["typeDeclFullName"] as String)
                .id(map["id"] as Long)
            TYPE_DECL -> NewTypeDeclBuilder()
                .name(map["name"] as String)
                .fullname(map["fullName"] as String)
                .order(map["order"] as Int)
                .id(map["id"] as Long)
            TYPE_PARAMETER -> NewTypeParameterBuilder()
                .name(map["name"] as String)
                .order(map["order"] as Int)
                .id(map["id"] as Long)
            TYPE_ARGUMENT -> NewTypeArgumentBuilder()
                .order(map["order"] as Int)
                .id(map["id"] as Long)
            MEMBER -> NewMemberBuilder()
                .typefullname(map["typeFullName"] as String)
                .code(map["code"] as String)
                .name(map["name"] as String)
                .order(map["order"] as Int)
                .id(map["id"] as Long)
            NAMESPACE_BLOCK -> NewNamespaceBlockBuilder()
                .fullname(map["fullName"] as String)
                .name(map["name"] as String)
                .order(map["order"] as Int)
                .id(map["id"] as Long)
            LITERAL -> NewLiteralBuilder()
                .typefullname(map["typeFullName"] as String)
                .code(map["code"] as String)
                .order(map["order"] as Int)
                .linenumber(Option.apply(map["lineNumber"] as Int))
                .columnnumber(Option.apply(map["columnNumber"] as Int))
                .argumentindex(map["argumentIndex"] as Int)
                .id(map["id"] as Long)
            CALL -> NewCallBuilder()
                .typefullname(map["typeFullName"] as String)
                .code(map["code"] as String)
                .name(map["name"] as String)
                .order(map["order"] as Int)
                .linenumber(Option.apply(map["lineNumber"] as Int))
                .columnnumber(Option.apply(map["columnNumber"] as Int))
                .argumentindex(map["argumentIndex"] as Int)
                .signature(map["signature"] as String)
                .dispatchtype(map["dispatchType"] as String)
                .methodfullname(map["methodFullName"] as String)
                .dynamictypehintfullname(createScalaList((map["dynamicTypeHintFullName"] as String)))
                .id(map["id"] as Long)
            LOCAL -> NewLocalBuilder()
                .typefullname(map["typeFullName"] as String)
                .code(map["code"] as String)
                .name(map["name"] as String)
                .order(map["order"] as Int)
                .linenumber(Option.apply(map["lineNumber"] as Int))
                .columnnumber(Option.apply(map["columnNumber"] as Int))
                .id(map["id"] as Long)
            IDENTIFIER -> NewIdentifierBuilder()
                .typefullname(map["typeFullName"] as String)
                .code(map["code"] as String)
                .name(map["name"] as String)
                .order(map["order"] as Int)
                .linenumber(Option.apply(map["lineNumber"] as Int))
                .columnnumber(Option.apply(map["columnNumber"] as Int))
                .argumentindex(map["argumentIndex"] as Int)
                .id(map["id"] as Long)
            FIELD_IDENTIFIER -> NewFieldIdentifierBuilder()
                .canonicalname(map["canonicalName"] as String)
                .code(map["code"] as String)
                .order(map["order"] as Int)
                .linenumber(Option.apply(map["lineNumber"] as Int))
                .columnnumber(Option.apply(map["columnNumber"] as Int))
                .argumentindex(map["argumentIndex"] as Int)
                .id(map["id"] as Long)
            RETURN -> NewReturnBuilder()
                .code(map["code"] as String)
                .order(map["order"] as Int)
                .linenumber(Option.apply(map["lineNumber"] as Int))
                .columnnumber(Option.apply(map["columnNumber"] as Int))
                .argumentindex(map["argumentIndex"] as Int)
                .id(map["id"] as Long)
            BLOCK -> NewBlockBuilder()
                .typefullname(map["typeFullName"] as String)
                .code(map["code"] as String)
                .order(map["order"] as Int)
                .linenumber(Option.apply(map["lineNumber"] as Int))
                .columnnumber(Option.apply(map["columnNumber"] as Int))
                .argumentindex(map["argumentIndex"] as Int)
                .id(map["id"] as Long)
            METHOD_REF -> NewMethodRefBuilder()
                .code(map["code"] as String)
                .order(map["order"] as Int)
                .methodfullname(map["methodFullName"] as String)
                .methodinstfullname(Option.apply(map["methodInstFullName"] as  String))
                .linenumber(Option.apply(map["lineNumber"] as Int))
                .columnnumber(Option.apply(map["columnNumber"] as Int))
                .argumentindex(map["argumentIndex"] as Int)
                .id(map["id"] as Long)
            TYPE_REF -> NewTypeRefBuilder()
                .typefullname(map["typeFullName"] as String)
                .dynamictypehintfullname(createScalaList((map["dynamicTypeHintFullName"] as String)))
                .code(map["code"] as String)
                .order(map["order"] as Int)
                .linenumber(Option.apply(map["lineNumber"] as Int))
                .columnnumber(Option.apply(map["columnNumber"] as Int))
                .argumentindex(map["argumentIndex"] as Int)
                .id(map["id"] as Long)
            JUMP_TARGET -> NewJumpTargetBuilder()
                .code(map["code"] as String)
                .order(map["order"] as Int)
                .linenumber(Option.apply(map["lineNumber"] as Int))
                .columnnumber(Option.apply(map["columnNumber"] as Int))
                .argumentindex(map["argumentIndex"] as Int)
                .name(map["name"] as String)
                .id(map["id"] as Long)
            CONTROL_STRUCTURE -> NewControlStructureBuilder()
                .code(map["code"] as String)
                .order(map["order"] as Int)
                .linenumber(Option.apply(map["lineNumber"] as Int))
                .columnnumber(Option.apply(map["columnNumber"] as Int))
                .argumentindex(map["argumentIndex"] as Int)
                .id(map["id"] as Long)
            UNKNOWN -> NewUnknownBuilder()
                .code(map["code"] as String)
                .order(map["order"] as Int)
                .argumentindex(map["argumentIndex"] as Int)
                .typefullname(map["typeFullName"] as String)
                .linenumber(Option.apply(map["lineNumber"] as Int))
                .columnnumber(Option.apply(map["columnNumber"] as Int))
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
        val fromLabel: VertexLabel = valueOf(vertexToMap(fromV).remove("label")!! as String)
        val toLabel: VertexLabel = valueOf(vertexToMap(toV).remove("label")!! as String)
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
}