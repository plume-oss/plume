package io.github.plume.oss.domain.mappers

import io.github.plume.oss.domain.enums.*
import io.github.plume.oss.domain.enums.VertexLabel.*
import io.github.plume.oss.domain.models.PlumeVertex
import io.github.plume.oss.domain.models.vertices.*

/**
 * Responsible for marshalling and unmarshalling vertex properties to and from [PlumeVertex] objects to [Map] objects.
 */
object VertexMapper {
    /**
     * Converts a [PlumeVertex]'s properties to a key-value [Map].
     *
     * @param v The vertex to serialize.
     * @return a [MutableMap] of the vertex's
     */
    fun vertexToMap(v: PlumeVertex): MutableMap<String, Any> {
        val properties = emptyMap<String, Any>().toMutableMap()
        when (v) {
            is ArrayInitializerVertex -> {
                properties["label"] = ArrayInitializerVertex.LABEL.name
                properties["order"] = v.order
            }
            is BindingVertex -> {
                properties["label"] = BindingVertex.LABEL.name
                properties["name"] = v.name
                properties["signature"] = v.signature
            }
            is BlockVertex -> {
                properties["label"] = BlockVertex.LABEL.name
                properties["typeFullName"] = v.typeFullName
                properties["order"] = v.order
                properties["argumentIndex"] = v.argumentIndex
                properties["code"] = v.code
                properties["columnNumber"] = v.columnNumber
                properties["lineNumber"] = v.lineNumber
            }
            is CallVertex -> {
                properties["label"] = CallVertex.LABEL.name
                properties["typeFullName"] = v.typeFullName
                properties["order"] = v.order
                properties["argumentIndex"] = v.argumentIndex
                properties["code"] = v.code
                properties["columnNumber"] = v.columnNumber
                properties["lineNumber"] = v.lineNumber
                properties["dispatchType"] = v.dispatchType.name
                properties["dynamicTypeHintFullName"] = v.dynamicTypeHintFullName
                properties["methodFullName"] = v.methodFullName
                properties["signature"] = v.signature
                properties["name"] = v.name

            }
            is ControlStructureVertex -> {
                properties["label"] = ControlStructureVertex.LABEL.name
                properties["order"] = v.order
                properties["argumentIndex"] = v.argumentIndex
                properties["code"] = v.code
                properties["columnNumber"] = v.columnNumber
                properties["lineNumber"] = v.lineNumber
            }
            is FieldIdentifierVertex -> {
                properties["label"] = FieldIdentifierVertex.LABEL.name
                properties["order"] = v.order
                properties["argumentIndex"] = v.argumentIndex
                properties["code"] = v.code
                properties["columnNumber"] = v.columnNumber
                properties["lineNumber"] = v.lineNumber
                properties["canonicalName"] = v.canonicalName
            }
            is FileVertex -> {
                properties["label"] = FileVertex.LABEL.name
                properties["order"] = v.order
                properties["name"] = v.name
                properties["hash"] = v.hash
            }
            is IdentifierVertex -> {
                properties["label"] = IdentifierVertex.LABEL.name
                properties["typeFullName"] = v.typeFullName
                properties["order"] = v.order
                properties["argumentIndex"] = v.argumentIndex
                properties["code"] = v.code
                properties["columnNumber"] = v.columnNumber
                properties["lineNumber"] = v.lineNumber
                properties["name"] = v.name
            }
            is JumpTargetVertex -> {
                properties["label"] = JumpTargetVertex.LABEL.name
                properties["order"] = v.order
                properties["argumentIndex"] = v.argumentIndex
                properties["code"] = v.code
                properties["columnNumber"] = v.columnNumber
                properties["lineNumber"] = v.lineNumber
                properties["name"] = v.name
            }
            is LiteralVertex -> {
                properties["label"] = LiteralVertex.LABEL.name
                properties["typeFullName"] = v.typeFullName
                properties["order"] = v.order
                properties["argumentIndex"] = v.argumentIndex
                properties["code"] = v.code
                properties["columnNumber"] = v.columnNumber
                properties["lineNumber"] = v.lineNumber
            }
            is LocalVertex -> {
                properties["label"] = LocalVertex.LABEL.name
                properties["typeFullName"] = v.typeFullName
                properties["order"] = v.order
                properties["code"] = v.code
                properties["columnNumber"] = v.columnNumber
                properties["lineNumber"] = v.lineNumber
                properties["name"] = v.name
            }
            is MemberVertex -> {
                properties["label"] = MemberVertex.LABEL.name
                properties["typeFullName"] = v.typeFullName
                properties["order"] = v.order
                properties["code"] = v.code
                properties["name"] = v.name
            }
            is MetaDataVertex -> {
                properties["label"] = MetaDataVertex.LABEL.name
                properties["language"] = v.language
                properties["version"] = v.language
            }
            is MethodParameterInVertex -> {
                properties["label"] = MethodParameterInVertex.LABEL.name
                properties["typeFullName"] = v.typeFullName
                properties["order"] = v.order
                properties["code"] = v.code
                properties["lineNumber"] = v.lineNumber
                properties["name"] = v.name
                properties["evaluationStrategy"] = v.evaluationStrategy.name
            }
            is MethodRefVertex -> {
                properties["label"] = MethodRefVertex.LABEL.name
                properties["order"] = v.order
                properties["argumentIndex"] = v.argumentIndex
                properties["code"] = v.code
                properties["columnNumber"] = v.columnNumber
                properties["lineNumber"] = v.lineNumber
                properties["methodFullName"] = v.methodFullName
                properties["methodInstFullName"] = v.methodInstFullName
            }
            is MethodReturnVertex -> {
                properties["label"] = MethodReturnVertex.LABEL.name
                properties["typeFullName"] = v.typeFullName
                properties["order"] = v.order
                properties["code"] = v.code
                properties["columnNumber"] = v.columnNumber
                properties["lineNumber"] = v.lineNumber
                properties["evaluationStrategy"] = v.evaluationStrategy.name
            }
            is MethodVertex -> {
                properties["label"] = MethodVertex.LABEL.name
                properties["order"] = v.order
                properties["code"] = v.code
                properties["columnNumber"] = v.columnNumber
                properties["lineNumber"] = v.lineNumber
                properties["signature"] = v.signature
                properties["name"] = v.name
                properties["fullName"] = v.fullName
            }
            is ModifierVertex -> {
                properties["label"] = ModifierVertex.LABEL.name
                properties["order"] = v.order
                properties["modifierType"] = v.modifierType.name
            }
            is NamespaceBlockVertex -> {
                properties["label"] = NamespaceBlockVertex.LABEL.name
                properties["order"] = v.order
                properties["name"] = v.name
                properties["fullName"] = v.fullName
            }
            is ReturnVertex -> {
                properties["label"] = ReturnVertex.LABEL.name
                properties["order"] = v.order
                properties["argumentIndex"] = v.argumentIndex
                properties["code"] = v.code
                properties["columnNumber"] = v.columnNumber
                properties["lineNumber"] = v.lineNumber
            }
            is TypeArgumentVertex -> {
                properties["label"] = TypeArgumentVertex.LABEL.name
                properties["order"] = v.order
            }
            is TypeDeclVertex -> {
                properties["label"] = TypeDeclVertex.LABEL.name
                properties["order"] = v.order
                properties["name"] = v.name
                properties["fullName"] = v.fullName
                properties["typeDeclFullName"] = v.typeDeclFullName
            }
            is TypeParameterVertex -> {
                properties["label"] = TypeParameterVertex.LABEL.name
                properties["order"] = v.order
                properties["name"] = v.name
            }
            is TypeRefVertex -> {
                properties["label"] = TypeRefVertex.LABEL.name
                properties["typeFullName"] = v.typeFullName
                properties["order"] = v.order
                properties["argumentIndex"] = v.argumentIndex
                properties["code"] = v.code
                properties["columnNumber"] = v.columnNumber
                properties["lineNumber"] = v.lineNumber
                properties["dynamicTypeFullName"] = v.dynamicTypeFullName
            }
            is TypeVertex -> {
                properties["label"] = TypeVertex.LABEL.name
                properties["name"] = v.name
                properties["fullName"] = v.fullName
                properties["typeDeclFullName"] = v.typeDeclFullName
            }
            is UnknownVertex -> {
                properties["label"] = UnknownVertex.LABEL.name
                properties["typeFullName"] = v.typeFullName
                properties["order"] = v.order
                properties["argumentIndex"] = v.argumentIndex
                properties["code"] = v.code
                properties["columnNumber"] = v.columnNumber
                properties["lineNumber"] = v.lineNumber
            }
        }
        return properties
    }

    /**
     * Converts a [Map] containing vertex properties to its respective [PlumeVertex] object.
     *
     * @param mapToConvert The [Map] to deserialize.
     * @return a [PlumeVertex] represented by the information in the given map.
     */
    fun mapToVertex(mapToConvert: Map<String, Any>): PlumeVertex {
        val map = HashMap<String, Any>()
        mapToConvert.keys.forEach {
            when (val value = mapToConvert[it]) {
                is Long -> map[it] = value.toInt()
                else -> map[it] = value as Any
            }
        }
        return when (valueOf(map["label"] as String)) {
            ARRAY_INITIALIZER -> ArrayInitializerVertex(order = map["order"] as Int)
            BINDING -> BindingVertex(
                name = map["name"] as String,
                signature = map["signature"] as String
            )
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