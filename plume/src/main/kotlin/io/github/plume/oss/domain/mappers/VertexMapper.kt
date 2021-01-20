package io.github.plume.oss.domain.mappers

import io.github.plume.oss.domain.enums.EdgeLabel
import io.github.plume.oss.domain.enums.VertexLabel
import io.github.plume.oss.domain.enums.VertexLabel.*
import io.github.plume.oss.util.SootToPlumeUtil
import io.shiftleft.codepropertygraph.generated.nodes.*
import scala.Option

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
                properties["dynamicTypeHintFullName"] =
                    SootToPlumeUtil.createSingleItemScalaList(node.dynamicTypeHintFullName())
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
                properties["methodInstFullName"] = node.methodInstFullName()
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
//                properties["typeDeclFullName"] = v.typeDeclFullName
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
//                properties["dynamicTypeFullName"] = v.dynamicTypeFullName
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
                is Long -> map[it] = if (it == "id") value.toLong() else value.toInt()
                else -> map[it] = value as Any
            }
        }
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
                .typefullname(map["fullName"] as String)
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
                .typefullname(map["fullName"] as String)
                .code(map["code"] as String)
                .order(map["order"] as Int)
                .linenumber(Option.apply(map["lineNumber"] as Int))
                .columnnumber(Option.apply(map["columnNumber"] as Int))
                .argumentindex(map["argumentIndex"] as Int)
                .id(map["id"] as Long)
            CALL -> NewCallBuilder()
                .typefullname(map["fullName"] as String)
                .code(map["code"] as String)
                .name(map["name"] as String)
                .order(map["order"] as Int)
                .linenumber(Option.apply(map["lineNumber"] as Int))
                .columnnumber(Option.apply(map["columnNumber"] as Int))
                .argumentindex(map["argumentIndex"] as Int)
                .signature(map["signature"] as String)
                .dispatchtype(map["dispatchType"] as String)
                .methodfullname(map["methodFullName"] as String)
                .id(map["id"] as Long)
            LOCAL -> NewLocalBuilder()
                .typefullname(map["fullName"] as String)
                .code(map["code"] as String)
                .name(map["name"] as String)
                .order(map["order"] as Int)
                .linenumber(Option.apply(map["lineNumber"] as Int))
                .columnnumber(Option.apply(map["columnNumber"] as Int))
                .id(map["id"] as Long)
            IDENTIFIER -> NewIdentifierBuilder()
                .typefullname(map["fullName"] as String)
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
                .typefullname(map["fullName"] as String)
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
                .methodinstfullname(Option.apply(map["methodInstFullName"] as String))
                .linenumber(Option.apply(map["lineNumber"] as Int))
                .columnnumber(Option.apply(map["columnNumber"] as Int))
                .argumentindex(map["argumentIndex"] as Int)
                .id(map["id"] as Long)
            TYPE_REF -> NewTypeRefBuilder()
                .typefullname(map["fullName"] as String)
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
            ARRAY_INITIALIZER -> ArrayInitializer.Edges.Out().contains(toLabel.name)
            BINDING -> Binding.Edges.Out().contains(toLabel.name)
            META_DATA -> MetaData.Edges.Out().contains(toLabel.name)
            FILE -> File.Edges.Out().contains(toLabel.name)
            METHOD -> Method.Edges.Out().contains(toLabel.name)
            METHOD_PARAMETER_IN -> MethodParameterIn.Edges.Out().contains(toLabel.name)
            METHOD_RETURN -> MethodReturn.Edges.Out().contains(toLabel.name)
            MODIFIER -> Modifier.Edges.Out().contains(toLabel.name)
            TYPE -> Type.Edges.Out().contains(toLabel.name)
            TYPE_DECL -> TypeDecl.Edges.Out().contains(toLabel.name)
            TYPE_PARAMETER -> TypeParameter.Edges.Out().contains(toLabel.name)
            TYPE_ARGUMENT -> TypeArgument.Edges.Out().contains(toLabel.name)
            MEMBER -> Member.Edges.Out().contains(toLabel.name)
            NAMESPACE_BLOCK -> NamespaceBlock.Edges.Out().contains(toLabel.name)
            LITERAL -> Literal.Edges.Out().contains(toLabel.name)
            CALL -> Call.Edges.Out().contains(toLabel.name)
            LOCAL -> Local.Edges.Out().contains(toLabel.name)
            IDENTIFIER -> Identifier.Edges.Out().contains(toLabel.name)
            FIELD_IDENTIFIER -> FieldIdentifier.Edges.Out().contains(toLabel.name)
            RETURN -> Return.Edges.Out().contains(toLabel.name)
            BLOCK -> Block.Edges.Out().contains(toLabel.name)
            METHOD_REF -> MethodRef.Edges.Out().contains(toLabel.name)
            TYPE_REF -> TypeRef.Edges.Out().contains(toLabel.name)
            JUMP_TARGET -> JumpTarget.Edges.Out().contains(toLabel.name)
            CONTROL_STRUCTURE -> ControlStructure.Edges.Out().contains(toLabel.name)
            UNKNOWN -> Unknown.Edges.Out().contains(toLabel.name)
        }
    }
}