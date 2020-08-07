package za.ac.sun.plume.domain.models.vertices

import za.ac.sun.plume.domain.enums.VertexBaseTraits
import za.ac.sun.plume.domain.enums.VertexLabels
import za.ac.sun.plume.domain.models.ASTVertex
import java.util.*

/**
 * A control structure such as if, while, or for
 */
class ControlStructureVertex(
        val name: String,
        val lineNumber: Int,
        order: Int,
        val argumentIndex: Int
) : ASTVertex(order) {
    override fun toString(): String {
        return "ControlStructureVertex{" +
                "name='" + name + '\'' +
                ", lineNumber=" + lineNumber +
                ", order=" + order +
                ", argumentIndex=" + argumentIndex +
                '}'
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ControlStructureVertex

        if (name != other.name) return false
        if (lineNumber != other.lineNumber) return false
        if (order != other.order) return false
        if (argumentIndex != other.argumentIndex) return false

        return true
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + lineNumber
        result = 31 * result + order
        result = 31 * result + argumentIndex
        return result
    }


    companion object {
        @kotlin.jvm.JvmField
        val LABEL = VertexLabels.CONTROL_STRUCTURE
        val TRAITS: EnumSet<VertexBaseTraits> = EnumSet.of(VertexBaseTraits.EXPRESSION)
    }

}