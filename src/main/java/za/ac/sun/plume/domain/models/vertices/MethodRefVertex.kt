package za.ac.sun.plume.domain.models.vertices

import za.ac.sun.plume.domain.enums.VertexBaseTraits
import za.ac.sun.plume.domain.enums.VertexLabels
import za.ac.sun.plume.domain.models.ASTVertex
import java.util.*

/**
 * Reference to a method instance
 */
class MethodRefVertex(
        val code: String,
        order: Int,
        val argumentIndex: Int,
        val methodInstFullName: String,
        val methodFullName: String,
        val lineNumber: Int
) : ASTVertex(order) {
    override fun toString(): String {
        return "MethodRefVertex{" +
                "code='" + code + '\'' +
                ", order=" + order +
                ", argumentIndex=" + argumentIndex +
                ", methodInstFullName='" + methodInstFullName + '\'' +
                ", methodFullName='" + methodFullName + '\'' +
                ", lineNumber=" + lineNumber +
                '}'
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as MethodRefVertex

        if (code != other.code) return false
        if (order != other.order) return false
        if (argumentIndex != other.argumentIndex) return false
        if (methodInstFullName != other.methodInstFullName) return false
        if (methodFullName != other.methodFullName) return false
        if (lineNumber != other.lineNumber) return false

        return true
    }

    override fun hashCode(): Int {
        var result = code.hashCode()
        result = 31 * result + order
        result = 31 * result + argumentIndex
        result = 31 * result + methodInstFullName.hashCode()
        result = 31 * result + methodFullName.hashCode()
        result = 31 * result + lineNumber
        return result
    }

    companion object {
        @kotlin.jvm.JvmField
        val LABEL = VertexLabels.METHOD_REF
        val TRAITS: EnumSet<VertexBaseTraits> = EnumSet.of(VertexBaseTraits.EXPRESSION)
    }

}