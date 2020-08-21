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
}