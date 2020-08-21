package za.ac.sun.plume.domain.models

import za.ac.sun.plume.domain.enums.VertexBaseTraits
import java.util.*

/**
 * Call representation.
 */
abstract class CallReprVertex (
        val name: String,
        val signature: String,
        code: String,
        lineNumber: Int,
        order: Int
): CFGVertex(lineNumber, code, order) {
    companion object {
        @JvmField
        val TRAITS: EnumSet<VertexBaseTraits> = EnumSet.of(
                VertexBaseTraits.CFG_NODE,
                VertexBaseTraits.TRACKING_POINT,
                VertexBaseTraits.AST_NODE,
                VertexBaseTraits.CALL_REPR
        )
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CallReprVertex) return false
        if (!super.equals(other)) return false

        if (name != other.name) return false
        if (signature != other.signature) return false

        return true
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + name.hashCode()
        result = 31 * result + signature.hashCode()
        return result
    }
}