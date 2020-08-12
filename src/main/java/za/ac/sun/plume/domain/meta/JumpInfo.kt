package za.ac.sun.plume.domain.meta

import org.objectweb.asm.Label

data class JumpInfo(val jumpOp: String, val destLabel: Label, val currLabel: Label, val pseudoLineNo: Int) {
    override fun toString(): String {
        return "[$pseudoLineNo] $jumpOp @ $currLabel -> $destLabel"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as JumpInfo

        if (jumpOp != other.jumpOp) return false
        if (destLabel != other.destLabel) return false
        if (currLabel != other.currLabel) return false
        if (pseudoLineNo != other.pseudoLineNo) return false

        return true
    }

    override fun hashCode(): Int {
        var result = jumpOp.hashCode()
        result = 31 * result + destLabel.hashCode()
        result = 31 * result + currLabel.hashCode()
        result = 31 * result + pseudoLineNo
        return result
    }
}