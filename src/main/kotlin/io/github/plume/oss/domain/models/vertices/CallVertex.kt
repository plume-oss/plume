package io.github.plume.oss.domain.models.vertices

import io.github.plume.oss.domain.enums.DispatchType
import io.github.plume.oss.domain.enums.EdgeLabel
import io.github.plume.oss.domain.enums.VertexBaseTrait
import io.github.plume.oss.domain.enums.VertexLabel
import io.github.plume.oss.domain.models.CallReprVertex
import java.util.*

/**
 * A (method)-call.
 */
class CallVertex(
        val methodFullName: String,
        val argumentIndex: Int,
        val dispatchType: DispatchType,
        val typeFullName: String,
        val dynamicTypeHintFullName: String,
        name: String,
        signature: String,
        code: String,
        order: Int,
        lineNumber: Int,
        columnNumber: Int

) : CallReprVertex(name, signature, code, lineNumber, columnNumber, order) {
    companion object {
        @JvmField
        val LABEL = VertexLabel.CALL

        @JvmField
        val TRAITS: EnumSet<VertexBaseTrait> = EnumSet.of(
                VertexBaseTrait.EXPRESSION,
                VertexBaseTrait.CALL_REPR
        )

        @JvmField
        val VALID_OUT_EDGES = mapOf(
                EdgeLabel.CFG to listOf(
                        VertexLabel.CALL,
                        VertexLabel.IDENTIFIER,
                        VertexLabel.FIELD_IDENTIFIER,
                        VertexLabel.LITERAL,
                        VertexLabel.METHOD_REF,
                        VertexLabel.TYPE_REF,
                        VertexLabel.RETURN,
                        VertexLabel.BLOCK,
                        VertexLabel.JUMP_TARGET,
                        VertexLabel.CONTROL_STRUCTURE,
                        VertexLabel.UNKNOWN
                ),
                EdgeLabel.AST to listOf(
                        VertexLabel.CALL,
                        VertexLabel.IDENTIFIER,
                        VertexLabel.FIELD_IDENTIFIER,
                        VertexLabel.LITERAL,
                        VertexLabel.METHOD_REF,
                        VertexLabel.TYPE_REF,
                        VertexLabel.RETURN,
                        VertexLabel.BLOCK,
                        VertexLabel.CONTROL_STRUCTURE
                ),
                EdgeLabel.RECEIVER to listOf(
                        VertexLabel.CALL,
                        VertexLabel.IDENTIFIER,
                        VertexLabel.LITERAL,
                        VertexLabel.METHOD_REF,
                        VertexLabel.TYPE_REF,
                        VertexLabel.BLOCK,
                        VertexLabel.CONTROL_STRUCTURE,
                        VertexLabel.UNKNOWN
                ),
                EdgeLabel.ARGUMENT to listOf(
                        VertexLabel.CALL,
                        VertexLabel.IDENTIFIER,
                        VertexLabel.FIELD_IDENTIFIER,
                        VertexLabel.LITERAL,
                        VertexLabel.METHOD_REF,
                        VertexLabel.TYPE_REF,
                        VertexLabel.BLOCK,
                        VertexLabel.JUMP_TARGET,
                        VertexLabel.CONTROL_STRUCTURE,
                        VertexLabel.UNKNOWN
                ),
                EdgeLabel.CALL to listOf(VertexLabel.METHOD)
        )
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CallVertex) return false
        if (!super.equals(other)) return false

        if (methodFullName != other.methodFullName) return false
        if (argumentIndex != other.argumentIndex) return false
        if (dispatchType != other.dispatchType) return false
        if (typeFullName != other.typeFullName) return false
        if (dynamicTypeHintFullName != other.dynamicTypeHintFullName) return false

        return true
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + methodFullName.hashCode()
        result = 31 * result + argumentIndex
        result = 31 * result + dispatchType.hashCode()
        result = 31 * result + typeFullName.hashCode()
        result = 31 * result + dynamicTypeHintFullName.hashCode()
        return result
    }

    override fun toString(): String {
        return "CallVertex(methodFullName='$methodFullName', argumentIndex=$argumentIndex, dispatchType=$dispatchType, typeFullName='$typeFullName', dynamicTypeHintFullName='$dynamicTypeHintFullName')"
    }
}