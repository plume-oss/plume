package za.ac.sun.plume.domain.models.vertices

import za.ac.sun.plume.domain.enums.DispatchType
import za.ac.sun.plume.domain.enums.VertexBaseTrait
import za.ac.sun.plume.domain.enums.VertexLabel
import za.ac.sun.plume.domain.models.CallReprVertex
import java.util.*

/**
 * A (method)-call
 */
class CallVertex(
        name: String,
        signature: String,
        code: String,
        order: Int,
        lineNumber: Int,
        val methodInstFullName: String,
        val methodFullName: String,
        val argumentIndex: Int,
        val dispatchType: DispatchType,
        val typeFullName: String
) : CallReprVertex(name, signature, code, lineNumber, order) {
    companion object {
        @kotlin.jvm.JvmField
        val LABEL = VertexLabel.CALL
        @kotlin.jvm.JvmField
        val TRAITS: EnumSet<VertexBaseTrait> = EnumSet.of(
                VertexBaseTrait.EXPRESSION,
                VertexBaseTrait.CALL_REPR
        )
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CallVertex) return false
        if (!super.equals(other)) return false

        if (methodInstFullName != other.methodInstFullName) return false
        if (methodFullName != other.methodFullName) return false
        if (argumentIndex != other.argumentIndex) return false
        if (dispatchType != other.dispatchType) return false
        if (typeFullName != other.typeFullName) return false

        return true
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + methodInstFullName.hashCode()
        result = 31 * result + methodFullName.hashCode()
        result = 31 * result + argumentIndex
        result = 31 * result + dispatchType.hashCode()
        result = 31 * result + typeFullName.hashCode()
        return result
    }

    override fun toString(): String {
        return "CallVertex(methodInstFullName='$methodInstFullName', methodFullName='$methodFullName', argumentIndex=$argumentIndex, dispatchType=$dispatchType, typeFullName='$typeFullName')"
    }
}