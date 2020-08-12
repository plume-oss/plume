package za.ac.sun.plume.domain.stack.block

import org.objectweb.asm.Label
import za.ac.sun.plume.domain.enums.JumpState

class GotoBlock(order: Int, label: Label?, destination: Label, position: JumpState) : JumpBlock(order, label, destination, position) {
    override fun toString(): String {
        return "GOTO {order: " + order + ", position: " + position.name + ", destination: " + destination + ", label: " + label + "}"
    }
}