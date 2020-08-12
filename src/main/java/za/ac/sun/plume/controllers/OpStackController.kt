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
package za.ac.sun.plume.controllers

import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.objectweb.asm.Label
import org.objectweb.asm.util.ASMifier
import za.ac.sun.plume.domain.meta.LineInfo
import za.ac.sun.plume.domain.stack.OperandItem
import za.ac.sun.plume.domain.stack.operand.ConstantItem
import za.ac.sun.plume.domain.stack.operand.OperatorItem
import za.ac.sun.plume.domain.stack.operand.VariableItem
import za.ac.sun.plume.util.ASMParserUtil
import java.util.*
import kotlin.math.absoluteValue

abstract class OpStackController(var allLines: HashSet<LineInfo> = HashSet()) : AbstractController {

    private val logger: Logger = LogManager.getLogger()

    val operandStack = Stack<OperandItem?>()
    val variables = HashSet<VariableItem>()
    var pseudoLineNo = 0

    fun initializeMethod() {
        pseudoLineNo = 0
    }

    fun pushNewLabel(label: Label) =
            getLineInfo(++pseudoLineNo)?.apply { associatedLabels.add(label) }
                    ?: allLines.add(LineInfo(pseudoLineNo).apply { associatedLabels.add(label) })

    protected fun getLineInfo(pseudoLineNo: Int): LineInfo? = allLines.find { lineInfo -> lineInfo.pseudoLineNumber == pseudoLineNo }

    protected fun getLineInfo(label: Label): LineInfo? = allLines.find { lineInfo -> lineInfo.associatedLabels.contains(label) }

    open fun pushConstInsnOperation(`val`: Any): ConstantItem {
        val canonicalType = `val`.javaClass.canonicalName.replace("\\.".toRegex(), "/")
        val className = canonicalType.substring(canonicalType.lastIndexOf("/") + 1)
        val stackItem: ConstantItem
        stackItem = if ("Integer" == className || "Long" == className || "Float" == className || "Double" == className) {
            ConstantItem(`val`.toString(), className.toUpperCase())
        } else {
            ConstantItem(`val`.toString(), canonicalType)
        }
        logger.debug("Pushing $stackItem")
        operandStack.push(stackItem)
        return stackItem
    }

    open fun pushConstInsnOperation(opcode: Int): OperandItem? {
        val line = ASMifier.OPCODES[opcode]
        val type: String
        var item: OperandItem? = null
        type = if (line[0] == 'L') "LONG" else ASMParserUtil.getReadableType(line[0])
        if (ASMParserUtil.isConstant(line)) {
            val `val` = line.substring(line.indexOf('_') + 1).replace("M", "-")
            item = ConstantItem(`val`, type)
        } else if (ASMParserUtil.isOperator(line)) {
            item = OperatorItem(line.substring(1), type)
        }
        if (Objects.nonNull(item)) {
            logger.debug("Pushing $item")
            operandStack.push(item)
        }
        return item
    }

    open fun pushConstInsnOperation(opcode: Int, operand: Int): ConstantItem {
        val type = ASMParserUtil.getReadableType(ASMifier.OPCODES[opcode][0])
        val item = ConstantItem(operand.toString(), type)
        logger.debug("Pushing $item")
        operandStack.push(item)
        return item
    }

    /**
     * Handles visitIincInsn and artificially coordinates an arithmetic operation with STORE call. This is only called
     * for integers - other types go through the usual CONST/STORE/OPERATOR hooks.
     *
     * @param var the variable being incremented.
     * @param increment the amount by which `var` is being incremented.
     */
    fun pushVarInc(`var`: Int, increment: Int) {
        val opType = "INTEGER"
        val op = if (increment > 0) OperatorItem("ADD", opType) else OperatorItem("SUB", opType)
        val varItem = VariableItem(`var`.toString(), opType)
        val constItem = ConstantItem(increment.absoluteValue.toString(), opType)
        operandStack.addAll(listOf(varItem, constItem, op))
        pushVarInsnStore(`var`, "I${op.id}")
    }

    /**
     * Handles visitVarInsn if the opcode is a load operation.
     *
     * @param varName   the variable name.
     * @param operation the load operation.
     */
    open fun pushVarInsnLoad(varName: Int, operation: String): VariableItem {
        val variableItem = getOrPutVariable(varName, ASMParserUtil.getStackOperationType(operation))
        logger.debug("Pushing $variableItem")
        operandStack.push(variableItem)
        return variableItem
    }

    fun getOrPutVariable(varName: Int, type: String): VariableItem {
        val varString = varName.toString()
        return variables.stream()
                .filter { variable: VariableItem -> varString == variable.id }
                .findFirst()
                .orElseGet {
                    val temp = VariableItem(varString, type)
                    variables.add(temp)
                    temp
                }
    }

    open fun handleOperator(operatorItem: OperatorItem) {
        val noOperands = 2
        for (i in 0 until noOperands) {
            when (val stackItem = operandStack.pop()) {
                is OperatorItem -> {
                    handleOperator(stackItem)
                }
            }
        }
    }

    open fun pushVarInsnStore(varName: Int, operation: String) {
        when (val operandItem = operandStack.pop()) {
            is OperatorItem -> {
                handleOperator(operandItem)
            }
        }
    }

    open fun pushNullaryJumps(label: Label) = Unit

    open fun pushBinaryJump(jumpOp: String, label: Label): List<OperandItem> = listOfNotNull(operandStack.pop(), operandStack.pop()).asReversed()

    open fun pushUnaryJump(jumpOp: String, label: Label): OperandItem? = operandStack.pop()

    override fun toString(): String {
        return """
            ${this.javaClass.canonicalName}
            Stack: $operandStack
            Variables: $variables
            """.trimIndent()
    }

    open fun clear(): AbstractController {
        operandStack.clear()
        variables.clear()
        return this
    }
}