package io.github.plume.oss.domain.mappers

import io.github.plume.oss.domain.enums.*
import io.github.plume.oss.domain.enums.VertexLabel.*
import io.shiftleft.codepropertygraph.generated.nodes.*
import scala.Array
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
    fun vertexToMap(v: NewNode): MutableMap<String, Any> {
        val properties = emptyMap<String, Any>().toMutableMap()
        when (v) {
            is NewArrayInitializer -> {
                properties["label"] = ArrayInitializer.Label()
                properties["order"] = v.order()
            }
            is NewBinding -> {
                properties["label"] = Binding.Label()
                properties["name"] = v.name()
                properties["signature"] = v.signature()
            }
            is NewBlock -> {
                properties["label"] = Block.Label()
                properties["typeFullName"] = v.typeFullName()
                properties["order"] = v.order()
                properties["argumentIndex"] = v.argumentIndex()
                properties["code"] = v.code()
                properties["columnNumber"] = v.columnNumber().get()
                properties["lineNumber"] = v.lineNumber().get()
            }
            is NewCall -> {
                properties["label"] = Call.Label()
                properties["typeFullName"] = v.typeFullName()
                properties["order"] = v.order()
                properties["argumentIndex"] = v.argumentIndex()
                properties["code"] = v.code()
                properties["columnNumber"] = v.columnNumber().get()
                properties["lineNumber"] = v.lineNumber().get()
                properties["dispatchType"] = v.dispatchType()
//                properties["dynamicTypeHintFullName"] = v.dynamicTypeHintFullName
                properties["methodFullName"] = v.methodFullName()
                properties["signature"] = v.signature()
                properties["name"] = v.name()
            }
            is NewControlStructure -> {
                properties["label"] = ControlStructure.Label()
                properties["order"] = v.order()
                properties["argumentIndex"] = v.argumentIndex()
                properties["code"] = v.code()
                properties["columnNumber"] = v.columnNumber().get()
                properties["lineNumber"] = v.lineNumber().get()
            }
            is NewFieldIdentifier -> {
                properties["label"] = FieldIdentifier.Label()
                properties["order"] = v.order()
                properties["argumentIndex"] = v.argumentIndex()
                properties["code"] = v.code()
                properties["columnNumber"] = v.columnNumber().get()
                properties["lineNumber"] = v.lineNumber().get()
                properties["canonicalName"] = v.canonicalName()
            }
            is NewFile -> {
                properties["label"] = File.Label()
                properties["order"] = v.order()
                properties["name"] = v.name()
                properties["hash"] = v.hash().get()
            }
            is NewIdentifier -> {
                properties["label"] = Identifier.Label()
                properties["typeFullName"] = v.typeFullName()
                properties["order"] = v.order()
                properties["argumentIndex"] = v.argumentIndex()
                properties["code"] = v.code()
                properties["columnNumber"] = v.columnNumber().get()
                properties["lineNumber"] = v.lineNumber().get()
                properties["name"] = v.name()
            }
            is NewJumpTarget -> {
                properties["label"] = JumpTarget.Label()
                properties["order"] = v.order()
                properties["argumentIndex"] = v.argumentIndex()
                properties["code"] = v.code()
                properties["columnNumber"] = v.columnNumber().get()
                properties["lineNumber"] = v.lineNumber().get()
                properties["name"] = v.name()
            }
            is NewLiteral -> {
                properties["label"] = Literal.Label()
                properties["typeFullName"] = v.typeFullName()
                properties["order"] = v.order()
                properties["argumentIndex"] = v.argumentIndex()
                properties["code"] = v.code()
                properties["columnNumber"] = v.columnNumber().get()
                properties["lineNumber"] = v.lineNumber().get()
            }
            is NewLocal -> {
                properties["label"] = Local.Label()
                properties["typeFullName"] = v.typeFullName()
                properties["order"] = v.order()
                properties["code"] = v.code()
                properties["columnNumber"] = v.columnNumber().get()
                properties["lineNumber"] = v.lineNumber().get()
                properties["name"] = v.name()
            }
            is NewMember -> {
                properties["label"] = Member.Label()
                properties["typeFullName"] = v.typeFullName()
                properties["order"] = v.order()
                properties["code"] = v.code()
                properties["name"] = v.name()
            }
            is NewMetaData -> {
                properties["label"] = MetaData.Label()
                properties["language"] = v.language()
                properties["version"] = v.version()
            }
            is NewMethodParameterIn -> {
                properties["label"] = MethodParameterIn.Label()
                properties["typeFullName"] = v.typeFullName()
                properties["order"] = v.order()
                properties["code"] = v.code()
                properties["lineNumber"] = v.lineNumber().get()
                properties["columnNumber"] = v.columnNumber().get()
                properties["name"] = v.name()
                properties["evaluationStrategy"] = v.evaluationStrategy()
            }
            is NewMethodRef -> {
                properties["label"] = MethodRef.Label()
                properties["order"] = v.order()
                properties["argumentIndex"] = v.argumentIndex()
                properties["code"] = v.code()
                properties["columnNumber"] = v.columnNumber().get()
                properties["lineNumber"] = v.lineNumber().get()
                properties["methodFullName"] = v.methodFullName()
                properties["methodInstFullName"] = v.methodInstFullName()
            }
            is NewMethodReturn -> {
                properties["label"] = MethodReturn.Label()
                properties["typeFullName"] = v.typeFullName()
                properties["order"] = v.order()
                properties["code"] = v.code()
                properties["columnNumber"] = v.columnNumber().get()
                properties["lineNumber"] = v.lineNumber().get()
                properties["evaluationStrategy"] = v.evaluationStrategy()
            }
            is NewMethod -> {
                properties["label"] = Method.Label()
                properties["order"] = v.order()
                properties["code"] = v.code()
                properties["columnNumber"] = v.columnNumber().get()
                properties["lineNumber"] = v.lineNumber().get()
                properties["signature"] = v.signature()
                properties["name"] = v.name()
                properties["fullName"] = v.fullName()
            }
            is NewModifier -> {
                properties["label"] = Modifier.Label()
                properties["order"] = v.order()
                properties["modifierType"] = v.modifierType()
            }
            is NewNamespaceBlock -> {
                properties["label"] = NamespaceBlock.Label()
                properties["order"] = v.order()
                properties["name"] = v.name()
                properties["fullName"] = v.fullName()
            }
            is NewReturn -> {
                properties["label"] = Return.Label()
                properties["order"] = v.order()
                properties["argumentIndex"] = v.argumentIndex()
                properties["code"] = v.code()
                properties["columnNumber"] = v.columnNumber().get()
                properties["lineNumber"] = v.lineNumber().get()
            }
            is NewTypeArgument -> {
                properties["label"] = TypeArgument.Label()
                properties["order"] = v.order()
            }
            is NewTypeDecl -> {
                properties["label"] = TypeDecl.Label()
                properties["order"] = v.order()
                properties["name"] = v.name()
                properties["fullName"] = v.fullName()
//                properties["typeDeclFullName"] = v.typeDeclFullName
            }
            is NewTypeParameter -> {
                properties["label"] = TypeParameter.Label()
                properties["order"] = v.order()
                properties["name"] = v.name()
            }
            is NewTypeRef -> {
                properties["label"] = TypeRef.Label()
                properties["typeFullName"] = v.typeFullName()
                properties["order"] = v.order()
                properties["argumentIndex"] = v.argumentIndex()
                properties["code"] = v.code()
                properties["columnNumber"] = v.columnNumber().get()
                properties["lineNumber"] = v.lineNumber().get()
//                properties["dynamicTypeFullName"] = v.dynamicTypeFullName
            }
            is NewType -> {
                properties["label"] = Type.Label()
                properties["name"] = v.name()
                properties["fullName"] = v.fullName()
                properties["typeDeclFullName"] = v.typeDeclFullName()
            }
            is NewUnknown -> {
                properties["label"] = Unknown.Label()
                properties["typeFullName"] = v.typeFullName()
                properties["order"] = v.order()
                properties["argumentIndex"] = v.argumentIndex()
                properties["code"] = v.code()
                properties["columnNumber"] = v.columnNumber().get()
                properties["lineNumber"] = v.lineNumber().get()
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
    fun mapToVertex(mapToConvert: Map<String, Any>): NewNode {
        val map = HashMap<String, Any>()
        mapToConvert.keys.forEach {
            when (val value = mapToConvert[it]) {
                is Long -> map[it] = value.toInt()
                else -> map[it] = value as Any
            }
        }
        return when (valueOf(map["label"] as String)) {
            ARRAY_INITIALIZER -> NewArrayInitializerBuilder()
                .order(map["order"] as Int)
                .build()
            BINDING -> NewBindingBuilder()
                .name(map["name"] as String)
                .signature(map["signature"] as String)
                .build()
            META_DATA -> NewMetaDataBuilder()
                .language(map["language"] as String)
                .version(map["version"] as String)
                .build()
            FILE -> NewFileBuilder()
                .name(map["name"] as String)
                .hash(Option.apply(map["hash"] as String))
                .order(map["order"] as Int)
                .build()
            METHOD -> NewMethodBuilder()
                .name(map["name"] as String)
                .code(map["code"] as String)
                .fullname(map["fullName"] as String)
                .signature(map["signature"] as String)
                .linenumber(Option.apply(map["lineNumber"] as Int))
                .columnnumber(Option.apply(map["columnNumber"] as Int))
                .order(map["order"] as Int)
                .build()
            METHOD_PARAMETER_IN -> NewMethodParameterInBuilder()
                .code(map["code"] as String)
                .name(map["name"] as String)
                .evaluationstrategy(map["evaluationStrategy"] as String)
                .typefullname(map["typeFullName"] as String)
                .linenumber(Option.apply(map["lineNumber"] as Int))
                .columnnumber(Option.apply(map["columnNumber"] as Int))
                .order(map["order"] as Int)
                .build()
            METHOD_RETURN -> NewMethodReturnBuilder()
                .code(map["code"] as String)
                .evaluationstrategy(map["evaluationStrategy"] as String)
                .typefullname(map["typeFullName"] as String)
                .linenumber(Option.apply(map["lineNumber"] as Int))
                .columnnumber(Option.apply(map["columnNumber"] as Int))
                .order(map["order"] as Int)
                .build()
            MODIFIER -> NewModifierBuilder()
                .modifiertype(map["modifierType"] as String)
                .order(map["order"] as Int)
                .build()
            TYPE -> NewTypeBuilder()
                .name(map["name"] as String)
                .fullname(map["fullName"] as String)
                .typedeclfullname(map["typeDeclFullName"] as String)
                .build()
            TYPE_DECL -> NewTypeDeclBuilder()
                .name(map["name"] as String)
                .fullname(map["fullName"] as String)
                .order(map["order"] as Int)
                .build()
            TYPE_PARAMETER -> NewTypeParameterBuilder()
                .name(map["name"] as String)
                .order(map["order"] as Int)
                .build()
            TYPE_ARGUMENT -> NewTypeArgumentBuilder()
                .order(map["order"] as Int)
                .build()
            MEMBER -> NewMemberBuilder()
                .typefullname(map["fullName"] as String)
                .code(map["code"] as String)
                .name(map["name"] as String)
                .order(map["order"] as Int)
                .build()
            NAMESPACE_BLOCK -> NewNamespaceBlockBuilder()
                .fullname(map["fullName"] as String)
                .name(map["name"] as String)
                .order(map["order"] as Int)
                .build()
            LITERAL -> NewLiteralBuilder()
                .typefullname(map["fullName"] as String)
                .code(map["code"] as String)
                .order(map["order"] as Int)
                .linenumber(Option.apply(map["lineNumber"] as Int))
                .columnnumber(Option.apply(map["columnNumber"] as Int))
                .argumentindex(map["argumentIndex"] as Int)
                .build()
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
                .build()
            LOCAL -> NewLocalBuilder()
                .typefullname(map["fullName"] as String)
                .code(map["code"] as String)
                .name(map["name"] as String)
                .order(map["order"] as Int)
                .linenumber(Option.apply(map["lineNumber"] as Int))
                .columnnumber(Option.apply(map["columnNumber"] as Int))
                .build()
            IDENTIFIER -> NewIdentifierBuilder()
                .typefullname(map["fullName"] as String)
                .code(map["code"] as String)
                .name(map["name"] as String)
                .order(map["order"] as Int)
                .linenumber(Option.apply(map["lineNumber"] as Int))
                .columnnumber(Option.apply(map["columnNumber"] as Int))
                .argumentindex(map["argumentIndex"] as Int)
                .build()
            FIELD_IDENTIFIER -> NewFieldIdentifierBuilder()
                .canonicalname(map["canonicalName"] as String)
                .code(map["code"] as String)
                .order(map["order"] as Int)
                .linenumber(Option.apply(map["lineNumber"] as Int))
                .columnnumber(Option.apply(map["columnNumber"] as Int))
                .argumentindex(map["argumentIndex"] as Int)
                .build()
            RETURN -> NewReturnBuilder()
                .code(map["code"] as String)
                .order(map["order"] as Int)
                .linenumber(Option.apply(map["lineNumber"] as Int))
                .columnnumber(Option.apply(map["columnNumber"] as Int))
                .argumentindex(map["argumentIndex"] as Int)
                .build()
            BLOCK -> NewBlockBuilder()
                .typefullname(map["fullName"] as String)
                .code(map["code"] as String)
                .order(map["order"] as Int)
                .linenumber(Option.apply(map["lineNumber"] as Int))
                .columnnumber(Option.apply(map["columnNumber"] as Int))
                .argumentindex(map["argumentIndex"] as Int)
                .build()
            METHOD_REF -> NewMethodRefBuilder()
                .code(map["code"] as String)
                .order(map["order"] as Int)
                .methodfullname(map["methodFullName"] as String)
                .methodinstfullname(Option.apply(map["methodInstFullName"] as String))
                .linenumber(Option.apply(map["lineNumber"] as Int))
                .columnnumber(Option.apply(map["columnNumber"] as Int))
                .argumentindex(map["argumentIndex"] as Int)
                .build()
            TYPE_REF -> NewTypeRefBuilder()
                .typefullname(map["fullName"] as String)
                .code(map["code"] as String)
                .order(map["order"] as Int)
                .linenumber(Option.apply(map["lineNumber"] as Int))
                .columnnumber(Option.apply(map["columnNumber"] as Int))
                .argumentindex(map["argumentIndex"] as Int)
                .build()
            JUMP_TARGET -> NewJumpTargetBuilder()
                .code(map["code"] as String)
                .order(map["order"] as Int)
                .linenumber(Option.apply(map["lineNumber"] as Int))
                .columnnumber(Option.apply(map["columnNumber"] as Int))
                .argumentindex(map["argumentIndex"] as Int)
                .name(map["name"] as String)
                .build()
            CONTROL_STRUCTURE -> NewControlStructureBuilder()
                .code(map["code"] as String)
                .order(map["order"] as Int)
                .linenumber(Option.apply(map["lineNumber"] as Int))
                .columnnumber(Option.apply(map["columnNumber"] as Int))
                .argumentindex(map["argumentIndex"] as Int)
                .build()
            UNKNOWN -> NewUnknownBuilder()
                .code(map["code"] as String)
                .order(map["order"] as Int)
                .typefullname(map["typeFullName"] as String)
                .linenumber(Option.apply(map["lineNumber"] as Int))
                .columnnumber(Option.apply(map["columnNumber"] as Int))
                .build()
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
    fun checkSchemaConstraints(fromV: NewNode, toV: NewNode, edge: EdgeLabel): Boolean {
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