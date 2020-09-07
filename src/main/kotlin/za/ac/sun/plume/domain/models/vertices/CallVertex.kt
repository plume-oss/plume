package za.ac.sun.plume.domain.models.vertices

import za.ac.sun.plume.domain.enums.DispatchType
import za.ac.sun.plume.domain.enums.EdgeLabel
import za.ac.sun.plume.domain.enums.VertexBaseTrait
import za.ac.sun.plume.domain.enums.VertexLabel
import za.ac.sun.plume.domain.models.CallReprVertex
import java.util.*

/**
 * A (method)-call.
 */
class CallVertex(
        val methodInstFullName: String,
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
                        VertexLabel.JUMP_TARGET,
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
                )
        )
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CallVertex) return false

        if (methodInstFullName != other.methodInstFullName) return false
        if (methodFullName != other.methodFullName) return false
        if (argumentIndex != other.argumentIndex) return false
        if (dispatchType != other.dispatchType) return false
        if (typeFullName != other.typeFullName) return false
        if (dynamicTypeHintFullName != other.dynamicTypeHintFullName) return false

        return true
    }

    override fun hashCode(): Int {
        var result = 31 * methodInstFullName.hashCode()
        result = 31 * result + methodFullName.hashCode()
        result = 31 * result + argumentIndex
        result = 31 * result + dispatchType.hashCode()
        result = 31 * result + typeFullName.hashCode()
        return result
    }

    override fun toString(): String {
        return "CallVertex(methodInstFullName='$methodInstFullName', methodFullName='$methodFullName', argumentIndex=$argumentIndex, dispatchType=$dispatchType, typeFullName='$typeFullName', dynamicTypeHintFullName='$dynamicTypeHintFullName')"
    }
}