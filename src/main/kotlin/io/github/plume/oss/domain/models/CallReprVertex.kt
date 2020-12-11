package io.github.plume.oss.domain.models

import io.github.plume.oss.domain.enums.VertexBaseTrait
import java.util.*

/**
 * Call representation.
 */
abstract class CallReprVertex (
        val name: String,
        val signature: String,
        code: String,
        lineNumber: Int,
        columnNumber: Int,
        order: Int
): CFGVertex(lineNumber, columnNumber, code, order) {
    companion object {
        @JvmField
        val TRAITS: EnumSet<VertexBaseTrait> = EnumSet.of(
                VertexBaseTrait.CFG_NODE,
                VertexBaseTrait.TRACKING_POINT,
                VertexBaseTrait.AST_NODE,
                VertexBaseTrait.CALL_REPR
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