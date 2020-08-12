package za.ac.sun.plume.domain.meta

import org.objectweb.asm.Label

data class LineInfo(val pseudoLineNumber: Int) {

    val associatedLabels = emptyList<Label>().toMutableList()

    override fun toString(): String {
        return "$pseudoLineNumber: $associatedLabels"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as LineInfo

        if (pseudoLineNumber != other.pseudoLineNumber) return false

        return true
    }

    override fun hashCode(): Int {
        return pseudoLineNumber
    }
}