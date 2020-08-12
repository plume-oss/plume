package za.ac.sun.plume.domain.stack.operand

import za.ac.sun.plume.domain.stack.OperandItem

class OperatorItem(id: String?, type: String?) : OperandItem(id!!, type!!) {
    override fun toString(): String {
        return "OPERATOR {value: $id, type: $type}"
    }
}