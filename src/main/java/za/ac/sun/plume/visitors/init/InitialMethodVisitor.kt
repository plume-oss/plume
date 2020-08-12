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
package za.ac.sun.plume.visitors.init

import org.apache.logging.log4j.LogManager
import org.objectweb.asm.Handle
import org.objectweb.asm.Label
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import org.objectweb.asm.util.ASMifier
import za.ac.sun.plume.controllers.MethodInfoController
import za.ac.sun.plume.domain.stack.operand.ConstantItem
import za.ac.sun.plume.domain.stack.operand.VariableItem
import za.ac.sun.plume.visitors.OpStackMethodVisitor

class InitialMethodVisitor(
        mv: MethodVisitor?,
        private val methodInfoController: MethodInfoController
) : OpStackMethodVisitor(mv, methodInfoController), Opcodes {

    private val logger = LogManager.getLogger()

    private var currentLabel: Label? = null

    override fun visitInsn(opcode: Int) {
        logger.debug("\t  " + ASMifier.OPCODES[opcode] + " (visitInsn)")
        super.visitInsn(opcode)
    }

    override fun visitIntInsn(opcode: Int, operand: Int) {
        logger.debug("\t  " + ASMifier.OPCODES[opcode] + " " + operand + " (visitIntInsn)")
        super.visitIntInsn(opcode, operand)
    }

    override fun visitVarInsn(opcode: Int, `var`: Int) {
        logger.debug("\t" + ASMifier.OPCODES[opcode] + " -> " + `var` + " (visitVarInsn)")
        methodInfoController.addVariable(`var`)
        super.visitVarInsn(opcode, `var`)
    }

    override fun visitJumpInsn(opcode: Int, label: Label) {
        logger.debug("\t  " + ASMifier.OPCODES[opcode] + " " + label + " (visitJumpInsn)")
        val operandStack = methodInfoController.operandStack
        if ("GOTO" == ASMifier.OPCODES[opcode] && !operandStack.isEmpty() && (operandStack.peek() is ConstantItem || operandStack.peek() is VariableItem))
            methodInfoController.addTernaryPair(ASMifier.OPCODES[opcode], label, currentLabel!!)
        else
            methodInfoController.addJump(ASMifier.OPCODES[opcode], label, currentLabel!!)
        super.visitJumpInsn(opcode, label)
    }

    override fun visitIincInsn(`var`: Int, increment: Int) {
        logger.debug("\t  VAR: $`var` INC: $increment (visitIincInsn)")
        super.visitIincInsn(`var`, increment)
    }

    override fun visitLabel(label: Label) {
        logger.debug("")
        logger.debug("\t$label (label)")
        currentLabel = label
        super.visitLabel(label)
    }

    override fun visitFieldInsn(opcode: Int, owner: String, name: String, descriptor: String) {
        logger.debug("\t" + ASMifier.OPCODES[opcode] + owner + " " + name + " " + descriptor + " (visitFieldInsn)")
        super.visitFieldInsn(opcode, owner, name, descriptor)
    }

    override fun visitLocalVariable(name: String, descriptor: String, signature: String?, start: Label, end: Label, index: Int) {
        logger.debug("\tDEBUG INFO: $descriptor $name -> ($start; $end) (visitLocalVariable)")
        methodInfoController.addVarDebugInfo(index, name, descriptor, start, end)
        super.visitLocalVariable(name, descriptor, signature, start, end, index)
    }

    override fun visitLineNumber(line: Int, start: Label) {
        logger.debug("\t  $line $start (visitLineNumber)")
        if (Integer.valueOf(-1) == methodInfoController.lineNumber) methodInfoController.lineNumber = line - 1
        super.visitLineNumber(line, start)
    }

    override fun visitLdcInsn(`val`: Any) {
        logger.debug("\t  $`val` (visitLdcInsn)")
        super.visitLdcInsn(`val`)
    }

    override fun visitInvokeDynamicInsn(name: String, descriptor: String, bootstrapMethodHandle: Handle, vararg bootstrapMethodArguments: Any) {
        logger.debug("\t  $name INC: $descriptor $bootstrapMethodHandle (visitInvokeDynamicInsn)")
        super.visitInvokeDynamicInsn(name, descriptor, bootstrapMethodHandle, *bootstrapMethodArguments)
    }

    override fun visitTypeInsn(opcode: Int, type: String) {
        logger.debug("\t  " + ASMifier.OPCODES[opcode] + " " + type + " (visitTypeInsn)")
        super.visitTypeInsn(opcode, type)
    }

    override fun visitMethodInsn(opcode: Int, owner: String, name: String, desc: String, itf: Boolean) {
        logger.debug("\t  " + ASMifier.OPCODES[opcode] + " " + owner + " " + name + " " + desc + " (visitMethodInsn)")
        super.visitMethodInsn(opcode, owner, name, desc, itf)
    }

    override fun visitEnd() {
        logger.debug("\t}")
    }

}