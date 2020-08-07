package za.ac.sun.plume.domain.models.vertices

import za.ac.sun.plume.domain.enums.VertexBaseTraits
import za.ac.sun.plume.domain.enums.VertexLabels
import za.ac.sun.plume.domain.models.ASTVertex
import java.util.*

/**
 * A return instruction
 */
class ReturnVertex(val lineNumber: Int, order: Int, val argumentIndex: Int, val code: String) : ASTVertex(order) {
    override fun toString(): String {
        return "ReturnVertex{" +
                "lineNumber=" + lineNumber +
                ", order=" + order +
                ", argumentIndex=" + argumentIndex +
                ", code='" + code + '\'' +
                '}'
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ReturnVertex

        if (lineNumber != other.lineNumber) return false
        if (order != other.order) return false
        if (argumentIndex != other.argumentIndex) return false
        if (code != other.code) return false

        return true
    }

    override fun hashCode(): Int {
        var result = lineNumber
        result = 31 * result + order
        result = 31 * result + argumentIndex
        result = 31 * result + code.hashCode()
        return result
    }

    companion object {
        @kotlin.jvm.JvmField
        val LABEL = VertexLabels.RETURN
        val TRAITS: EnumSet<VertexBaseTraits> = EnumSet.of(VertexBaseTraits.EXPRESSION)
    }

}