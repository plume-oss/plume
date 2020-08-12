package za.ac.sun.plume.domain.stack

import org.objectweb.asm.Label

abstract class BlockItem(val order: Int, var label: Label?) : StackItem