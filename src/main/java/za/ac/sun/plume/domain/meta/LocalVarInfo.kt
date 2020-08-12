package za.ac.sun.plume.domain.meta

import org.objectweb.asm.Label

data class LocalVarInfo(
        val frameId: Int,
        var debugName: String? = null,
        var descriptor: String? = null,
        var startLabel: Label? = null,
        var endLabel: Label? = null
) {
    override fun toString(): String {
        return if (debugName != null && descriptor != null) "LOCAL VAR $descriptor $debugName @ $frameId"
        else "LOCAL VAR $frameId"
    }
}