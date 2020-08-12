/*
 * Copyright 2020 David Baker Effendi
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package za.ac.sun.plume.visitors

import org.objectweb.asm.Label
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import org.objectweb.asm.util.ASMifier
import za.ac.sun.plume.controllers.OpStackController
import za.ac.sun.plume.util.ASMParserUtil
import za.ac.sun.plume.util.ASMParserUtil.isLoad
import za.ac.sun.plume.util.ASMParserUtil.isStore

open class OpStackMethodVisitor(
        mv: MethodVisitor?,
        private val controller: OpStackController
) : MethodVisitor(Opcodes.ASM5, mv), Opcodes {

    override fun visitCode() {
        super.visitCode()
        controller.initializeMethod()
    }

    override fun visitLabel(label: Label) {
        controller.pushNewLabel(label)
        super.visitLabel(label)
    }

    override fun visitInsn(opcode: Int) {
        super.visitInsn(opcode)
        controller.pushConstInsnOperation(opcode)
    }

    override fun visitIntInsn(opcode: Int, operand: Int) {
        super.visitIntInsn(opcode, operand)
        controller.pushConstInsnOperation(opcode, operand)
    }

    override fun visitVarInsn(opcode: Int, `var`: Int) {
        super.visitVarInsn(opcode, `var`)
        val operation = ASMifier.OPCODES[opcode]
        if (isLoad(operation)) controller.pushVarInsnLoad(`var`, operation)
        else if (isStore(operation)) controller.pushVarInsnStore(`var`, operation)
    }

    override fun visitJumpInsn(opcode: Int, label: Label) {
        super.visitJumpInsn(opcode, label)
        val jumpOp = ASMifier.OPCODES[opcode]
        when {
            ASMParserUtil.NULLARY_JUMPS.contains(jumpOp) -> controller.pushNullaryJumps(label)
            ASMParserUtil.UNARY_JUMPS.contains(jumpOp) -> controller.pushUnaryJump(jumpOp, label)
            ASMParserUtil.BINARY_JUMPS.contains(jumpOp) -> controller.pushBinaryJump(jumpOp, label)
        }
    }

    override fun visitLdcInsn(`val`: Any) {
        super.visitLdcInsn(`val`)
        controller.pushConstInsnOperation(`val`)
    }

    override fun visitIincInsn(`var`: Int, increment: Int) {
        super.visitIincInsn(`var`, increment)
        controller.pushVarInc(`var`, increment)
    }

}