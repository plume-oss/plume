package za.ac.sun.plume.domain.models.vertices

import za.ac.sun.plume.domain.enums.DispatchTypes
import za.ac.sun.plume.domain.enums.VertexBaseTraits
import za.ac.sun.plume.domain.enums.VertexLabels
import za.ac.sun.plume.domain.models.ASTVertex
import java.util.*

/**
 * A (method)-call
 */
class CallVertex(
        val code: String,
        val name: String,
        order: Int,
        val methodInstFullName: String,
        val methodFullName: String,
        val argumentIndex: Int,
        val dispatchType: DispatchTypes,
        val signature: String,
        val typeFullName: String,
        val lineNumber: Int
) : ASTVertex(order) {
    override fun toString(): String {
        return "CallVertex{" +
                "code='" + code + '\'' +
                ", name='" + name + '\'' +
                ", order=" + order +
                ", methodInstFullName='" + methodInstFullName + '\'' +
                ", methodFullName='" + methodFullName + '\'' +
                ", argumentIndex=" + argumentIndex +
                ", dispatchType=" + dispatchType +
                ", signature='" + signature + '\'' +
                ", typeFullName='" + typeFullName + '\'' +
                ", lineNumber=" + lineNumber +
                '}'
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as CallVertex

        if (code != other.code) return false
        if (name != other.name) return false
        if (order != other.order) return false
        if (methodInstFullName != other.methodInstFullName) return false
        if (methodFullName != other.methodFullName) return false
        if (argumentIndex != other.argumentIndex) return false
        if (dispatchType != other.dispatchType) return false
        if (signature != other.signature) return false
        if (typeFullName != other.typeFullName) return false
        if (lineNumber != other.lineNumber) return false

        return true
    }

    override fun hashCode(): Int {
        var result = code.hashCode()
        result = 31 * result + name.hashCode()
        result = 31 * result + order
        result = 31 * result + methodInstFullName.hashCode()
        result = 31 * result + methodFullName.hashCode()
        result = 31 * result + argumentIndex
        result = 31 * result + dispatchType.hashCode()
        result = 31 * result + signature.hashCode()
        result = 31 * result + typeFullName.hashCode()
        result = 31 * result + lineNumber
        return result
    }

    companion object {
        @kotlin.jvm.JvmField
        val LABEL = VertexLabels.CALL
        val TRAITS: EnumSet<VertexBaseTraits> = EnumSet.of(VertexBaseTraits.EXPRESSION,
                VertexBaseTraits.CALL_REPR)
    }
}