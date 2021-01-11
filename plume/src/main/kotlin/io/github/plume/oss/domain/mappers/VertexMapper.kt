package io.github.plume.oss.domain.mappers

import io.github.plume.oss.domain.enums.*
import io.github.plume.oss.domain.enums.VertexLabel.*
import io.shiftleft.codepropertygraph.generated.nodes.*

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
            META_DATA -> MetaDataVertex(
                language = map["language"] as String,
                version = map["version"] as String
            )
            FILE -> FileVertex(
                name = map["name"] as String,
                hash = map["hash"] as String,
                order = map["order"] as Int
            )
            METHOD -> MethodVertex(
                name = map["name"] as String,
                code = map["code"] as String,
                fullName = map["fullName"] as String,
                signature = map["signature"] as String,
                lineNumber = map["lineNumber"] as Int,
                columnNumber = map["columnNumber"] as Int,
                order = map["order"] as Int
            )
            METHOD_PARAMETER_IN -> MethodParameterInVertex(
                code = map["code"] as String,
                name = map["name"] as String,
                evaluationStrategy = EvaluationStrategy.valueOf(map["evaluationStrategy"] as String),
                typeFullName = map["typeFullName"] as String,
                lineNumber = map["lineNumber"] as Int,
                order = map["order"] as Int
            )
            METHOD_RETURN -> MethodReturnVertex(
                code = map["code"] as String,
                evaluationStrategy = EvaluationStrategy.valueOf(map["evaluationStrategy"] as String),
                typeFullName = map["typeFullName"] as String,
                lineNumber = map["lineNumber"] as Int,
                columnNumber = map["columnNumber"] as Int,
                order = map["order"] as Int
            )
            MODIFIER -> ModifierVertex(
                modifierType = ModifierType.valueOf(map["modifierType"] as String),
                order = map["order"] as Int
            )
            TYPE -> TypeVertex(
                name = map["name"] as String,
                fullName = map["fullName"] as String,
                typeDeclFullName = map["typeDeclFullName"] as String
            )
            TYPE_DECL -> TypeDeclVertex(
                name = map["name"] as String,
                order = map["order"] as Int,
                fullName = map["fullName"] as String,
                typeDeclFullName = map["typeDeclFullName"] as String
            )
            TYPE_PARAMETER -> TypeParameterVertex(
                name = map["name"] as String,
                order = map["order"] as Int
            )
            TYPE_ARGUMENT -> TypeArgumentVertex(
                order = map["order"] as Int
            )
            MEMBER -> MemberVertex(
                code = map["code"] as String,
                name = map["name"] as String,
                typeFullName = map["typeFullName"] as String,
                order = map["order"] as Int
            )
            NAMESPACE_BLOCK -> NamespaceBlockVertex(
                name = map["name"] as String,
                fullName = map["fullName"] as String,
                order = map["order"] as Int
            )
            LITERAL -> LiteralVertex(
                code = map["code"] as String,
                typeFullName = map["typeFullName"] as String,
                lineNumber = map["lineNumber"] as Int,
                columnNumber = map["columnNumber"] as Int,
                order = map["order"] as Int,
                argumentIndex = map["argumentIndex"] as Int
            )
            CALL -> CallVertex(
                code = map["code"] as String,
                name = map["name"] as String,
                typeFullName = map["typeFullName"] as String,
                dynamicTypeHintFullName = map["dynamicTypeHintFullName"] as String,
                order = map["order"] as Int,
                argumentIndex = map["argumentIndex"] as Int,
                methodFullName = map["methodFullName"] as String,
                lineNumber = map["lineNumber"] as Int,
                columnNumber = map["columnNumber"] as Int,
                signature = map["signature"] as String,
                dispatchType = DispatchType.valueOf(map["dispatchType"] as String)
            )
            LOCAL -> LocalVertex(
                code = map["code"] as String,
                name = map["name"] as String,
                typeFullName = map["typeFullName"] as String,
                lineNumber = map["lineNumber"] as Int,
                columnNumber = map["columnNumber"] as Int,
                order = map["order"] as Int
            )
            IDENTIFIER -> IdentifierVertex(
                code = map["code"] as String,
                name = map["name"] as String,
                typeFullName = map["typeFullName"] as String,
                order = map["order"] as Int,
                argumentIndex = map["argumentIndex"] as Int,
                lineNumber = map["lineNumber"] as Int,
                columnNumber = map["columnNumber"] as Int
            )
            FIELD_IDENTIFIER -> FieldIdentifierVertex(
                code = map["code"] as String,
                canonicalName = map["canonicalName"] as String,
                order = map["order"] as Int,
                argumentIndex = map["argumentIndex"] as Int,
                lineNumber = map["lineNumber"] as Int,
                columnNumber = map["columnNumber"] as Int
            )
            RETURN -> ReturnVertex(
                code = map["code"] as String,
                order = map["order"] as Int,
                argumentIndex = map["argumentIndex"] as Int,
                lineNumber = map["lineNumber"] as Int,
                columnNumber = map["columnNumber"] as Int
            )
            BLOCK -> BlockVertex(
                code = map["code"] as String,
                typeFullName = map["typeFullName"] as String,
                order = map["order"] as Int,
                argumentIndex = map["argumentIndex"] as Int,
                lineNumber = map["lineNumber"] as Int,
                columnNumber = map["columnNumber"] as Int
            )
            METHOD_REF -> MethodRefVertex(
                code = map["code"] as String,
                order = map["order"] as Int,
                argumentIndex = map["argumentIndex"] as Int,
                methodFullName = map["methodFullName"] as String,
                methodInstFullName = map["methodInstFullName"] as String,
                lineNumber = map["lineNumber"] as Int,
                columnNumber = map["columnNumber"] as Int
            )
            TYPE_REF -> TypeRefVertex(
                code = map["code"] as String,
                order = map["order"] as Int,
                typeFullName = map["typeFullName"] as String,
                dynamicTypeFullName = map["dynamicTypeFullName"] as String,
                argumentIndex = map["argumentIndex"] as Int,
                lineNumber = map["lineNumber"] as Int,
                columnNumber = map["columnNumber"] as Int
            )
            JUMP_TARGET -> JumpTargetVertex(
                name = map["name"] as String,
                code = map["code"] as String,
                order = map["order"] as Int,
                argumentIndex = map["argumentIndex"] as Int,
                lineNumber = map["lineNumber"] as Int,
                columnNumber = map["columnNumber"] as Int
            )
            CONTROL_STRUCTURE -> ControlStructureVertex(
                code = map["code"] as String,
                order = map["order"] as Int,
                argumentIndex = map["argumentIndex"] as Int,
                lineNumber = map["lineNumber"] as Int,
                columnNumber = map["columnNumber"] as Int
            )
            UNKNOWN -> UnknownVertex(
                code = map["code"] as String,
                typeFullName = map["typeFullName"] as String,
                order = map["order"] as Int,
                argumentIndex = map["argumentIndex"] as Int,
                lineNumber = map["lineNumber"] as Int,
                columnNumber = map["columnNumber"] as Int
            )
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
    fun checkSchemaConstraints(fromV: PlumeVertex, toV: PlumeVertex, edge: EdgeLabel): Boolean {
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
            ARRAY_INITIALIZER -> ArrayInitializerVertex.VALID_OUT_EDGES[edge]?.contains(toLabel) ?: false
            BINDING -> BindingVertex.VALID_OUT_EDGES[edge]?.contains(toLabel) ?: false
            META_DATA -> MetaDataVertex.VALID_OUT_EDGES[edge]?.contains(toLabel) ?: false
            FILE -> FileVertex.VALID_OUT_EDGES[edge]?.contains(toLabel) ?: false
            METHOD -> MethodVertex.VALID_OUT_EDGES[edge]?.contains(toLabel) ?: false
            METHOD_PARAMETER_IN -> MethodParameterInVertex.VALID_OUT_EDGES[edge]?.contains(toLabel) ?: false
            METHOD_RETURN -> MethodReturnVertex.VALID_OUT_EDGES[edge]?.contains(toLabel) ?: false
            MODIFIER -> ModifierVertex.VALID_OUT_EDGES[edge]?.contains(toLabel) ?: false
            TYPE -> TypeVertex.VALID_OUT_EDGES[edge]?.contains(toLabel) ?: false
            TYPE_DECL -> TypeDeclVertex.VALID_OUT_EDGES[edge]?.contains(toLabel) ?: false
            TYPE_PARAMETER -> TypeParameterVertex.VALID_OUT_EDGES[edge]?.contains(toLabel) ?: false
            TYPE_ARGUMENT -> TypeArgumentVertex.VALID_OUT_EDGES[edge]?.contains(toLabel) ?: false
            MEMBER -> MemberVertex.VALID_OUT_EDGES[edge]?.contains(toLabel) ?: false
            NAMESPACE_BLOCK -> NamespaceBlockVertex.VALID_OUT_EDGES[edge]?.contains(toLabel) ?: false
            LITERAL -> LiteralVertex.VALID_OUT_EDGES[edge]?.contains(toLabel) ?: false
            CALL -> CallVertex.VALID_OUT_EDGES[edge]?.contains(toLabel) ?: false
            LOCAL -> LocalVertex.VALID_OUT_EDGES[edge]?.contains(toLabel) ?: false
            IDENTIFIER -> IdentifierVertex.VALID_OUT_EDGES[edge]?.contains(toLabel) ?: false
            FIELD_IDENTIFIER -> FieldIdentifierVertex.VALID_OUT_EDGES[edge]?.contains(toLabel) ?: false
            RETURN -> ReturnVertex.VALID_OUT_EDGES[edge]?.contains(toLabel) ?: false
            BLOCK -> BlockVertex.VALID_OUT_EDGES[edge]?.contains(toLabel) ?: false
            METHOD_REF -> MethodRefVertex.VALID_OUT_EDGES[edge]?.contains(toLabel) ?: false
            TYPE_REF -> TypeRefVertex.VALID_OUT_EDGES[edge]?.contains(toLabel) ?: false
            JUMP_TARGET -> JumpTargetVertex.VALID_OUT_EDGES[edge]?.contains(toLabel) ?: false
            CONTROL_STRUCTURE -> ControlStructureVertex.VALID_OUT_EDGES[edge]?.contains(toLabel) ?: false
            UNKNOWN -> UnknownVertex.VALID_OUT_EDGES[edge]?.contains(toLabel) ?: false
        }
    }
}