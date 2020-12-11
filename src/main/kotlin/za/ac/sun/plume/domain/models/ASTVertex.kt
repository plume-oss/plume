package za.ac.sun.plume.domain.models

import za.ac.sun.plume.domain.enums.VertexBaseTrait
import java.util.*

/**
 * Any vertex that can exist in an abstract syntax tree.
 */
abstract class ASTVertex(val order: Int) : WithinMethod() {
    companion object {
        @JvmField
        val TRAITS: EnumSet<VertexBaseTrait> = EnumSet.of(
                VertexBaseTrait.AST_NODE,
                VertexBaseTrait.WITHIN_METHOD
        )
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ASTVertex) return false
        if (!super.equals(other)) return false

        if (order != other.order) return false

        return true
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + order
        return result
    }
}