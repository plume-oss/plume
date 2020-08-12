package za.ac.sun.plume.domain.stack.operand

import za.ac.sun.plume.domain.stack.OperandItem

class VariableItem(id: String?, type: String?) : OperandItem(id!!, type!!) {
    override fun toString(): String {
        return "VAR {id: $id, type: $type}"
    }
}