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
package za.ac.sun.plume.util

import org.objectweb.asm.Opcodes
import za.ac.sun.plume.domain.enums.Equality
import za.ac.sun.plume.domain.enums.EvaluationStrategies
import za.ac.sun.plume.domain.enums.JumpAssociations
import za.ac.sun.plume.domain.enums.ModifierTypes
import za.ac.sun.plume.domain.enums.Operators
import java.util.*
import java.util.stream.IntStream

object ASMParserUtil : Opcodes {
    private val PRIMITIVES: Map<Char, String> = mapOf(
            'Z' to "BOOLEAN",
            'C' to "CHARACTER",
            'B' to "BYTE",
            'S' to "SHORT",
            'I' to "INTEGER",
            'F' to "FLOAT",
            'J' to "LONG",
            'D' to "DOUBLE",
            'V' to "VOID"
    )
    private val OPERANDS: Set<String> = setOf("ADD", "SUB", "MUL", "DIV", "REM", "OR", "XOR", "AND", "SHR", "SHL", "USHR")

    @JvmField
    val NULLARY_JUMPS: Set<String> = setOf("GOTO", "TABLESWITCH", "LOOKUPSWITCH")

    @JvmField
    val UNARY_JUMPS: Set<String> = setOf("IFEQ", "IFNE", "IFLT", "IFGE", "IFGT", "IFLE", "IFNULL", "IFNONNULL")

    @JvmField
    val BINARY_JUMPS: Set<String> = setOf("IF_ICMPEQ", "IF_ICMPNE", "IF_ICMPLT", "IF_ICMPGE", "IF_ICMPGT", "IF_ICMPLE", "IF_ACMPEQ", "IF_ACMPNE")

    /**
     * Given a method signature, returns a list of all the parameters separated into a list.
     *
     * @param signature the raw method signature from ASM5
     * @return a list of the parameters
     */
    @JvmStatic
    fun obtainParameters(signature: String): List<String> {
        val parameters: MutableList<String> = ArrayList()
        val sigArr = signature.substring(1, signature.indexOf(')')).toCharArray()
        val sb = StringBuilder()
        IntStream.range(0, sigArr.size).mapToObj { i: Int -> sigArr[i] }.forEach { c: Char ->
            if (c == ';') {
                parameters.add(sb.toString())
                sb.delete(0, sb.length)
            } else if (isPrimitive(c) && sb.indexOf("L") == -1) {
                parameters.add(sb.append(c).toString())
                sb.delete(0, sb.length)
            } else if (isObject(c)) {
                sb.append(c)
            } else if (isArray(c)) sb.append(c) else sb.append(c)
        }
        return parameters
    }

    /**
     * Given a method signature, returns the return type.
     *
     * @param signature the raw method signature from ASM5
     * @return a list of the parameters.
     */
    @JvmStatic
    fun obtainMethodReturnType(signature: String): String {
        return signature.substring(signature.lastIndexOf(')') + 1).replace(";".toRegex(), "")
    }

    /**
     * Given a parameter signature and context of the parameter, determines the evaluation strategy used.
     * TODO: Confirm if these assumptions are true
     *
     * @param paramType      the parameter signature from ASM5
     * @param isMethodReturn true if the parameter type is from a method
     * @return the type of evaluation strategy used
     */
    @JvmStatic
    fun determineEvaluationStrategy(paramType: String, isMethodReturn: Boolean): EvaluationStrategies {
        val evalChar = paramType[0]
        return if (isArray(evalChar) || isObject(evalChar)) {
            if (isMethodReturn) EvaluationStrategies.BY_SHARING else EvaluationStrategies.BY_REFERENCE
        } else EvaluationStrategies.BY_VALUE
    }

    /**
     * Given the ASM5 access parameter and method name, determines the modifier types.
     *
     *
     * In Java, all non-static methods are by default "virtual functions." Only methods marked with the keyword final,
     * which cannot be overridden, along with private methods, which are not inherited, are non-virtual.
     *
     * @param access ASM5 access parameter obtained from visitClass and visitMethod.
     * @param name   name of the method obtained from visitClass and visitMethod.
     * @return an EnumSet of the applicable modifier types.
     */
    @JvmStatic
    @JvmOverloads
    fun determineModifiers(access: Int, name: String? = null): EnumSet<ModifierTypes> {
        val modifiers = EnumSet.of(ModifierTypes.VIRTUAL)
        if ("<init>" == name) modifiers.add(ModifierTypes.CONSTRUCTOR)
        var remaining = access
        var bit: Int
        while (remaining != 0) {
            bit = Integer.lowestOneBit(remaining)
            when (bit) {
                Opcodes.ACC_STATIC -> {
                    modifiers.add(ModifierTypes.STATIC)
                    modifiers.remove(ModifierTypes.VIRTUAL)
                }
                Opcodes.ACC_PUBLIC -> modifiers.add(ModifierTypes.PUBLIC)
                Opcodes.ACC_PRIVATE -> {
                    modifiers.add(ModifierTypes.PRIVATE)
                    modifiers.remove(ModifierTypes.VIRTUAL)
                }
                Opcodes.ACC_PROTECTED -> modifiers.add(ModifierTypes.PROTECTED)
                Opcodes.ACC_NATIVE -> modifiers.add(ModifierTypes.NATIVE)
                Opcodes.ACC_ABSTRACT -> modifiers.add(ModifierTypes.ABSTRACT)
                Opcodes.ACC_FINAL -> modifiers.remove(ModifierTypes.VIRTUAL)
            }
            remaining -= bit
        }
        return modifiers
    }

    fun getReadableType(rawType: Char): String {
        return getReadableType(rawType.toString())
    }

    /**
     * Returns the "readable" name of a given raw name from ASM. For example I -> Integer and
     * java/util/String -> String.
     *
     * @param rawType the unprocessed type string.
     * @return a more "readable" variant of the type.
     */
    @JvmStatic
    fun getReadableType(rawType: String): String {
        val sb = StringBuilder()
        val sigArr = rawType.toCharArray()
        IntStream.range(0, sigArr.size).mapToObj { i: Int -> sigArr[i] }.forEach { c: Char ->
            if (isPrimitive(c) && sb.indexOf("L") == -1) {
                sb.append(c)
            } else if (isArray(c) || isObject(c)) sb.append(c)
        }
        if (rawType.contains("L")) {
            sb.delete(sb.indexOf("L"), sb.length)
            if (rawType.contains("/")) {
                sb.append(rawType.substring(rawType.lastIndexOf("/") + 1))
            }
        } else {
            val oldLength = sb.length
            sb.append(convertAllPrimitivesToName(sb.toString()))
            sb.delete(0, oldLength)
        }
        return sb.toString()
    }

    /**
     * Given a stack operation, returns the type.
     *
     * @param operation an xSTORE or xLOAD operation.
     * @return the type of the STORE or LOAD operation. If the operation is invalid, will return "UNKNOWN".
     */
    @JvmStatic
    fun getStackOperationType(operation: String): String {
        if (!operation.contains("STORE") && !operation.contains("LOAD")) return "UNKNOWN"
        return if (operation.length != 6 && operation.length != 5) "UNKNOWN" else stackType(operation[0])
    }

    private fun stackType(type: Char): String {
        if (type == 'A') return "OBJECT"
        if (type == 'L') return "LONG"
        if (type == 'J') return "UNKNOWN"
        return if (type == '[') "UNKNOWN" else PRIMITIVES[type] ?: "UNKNOWN"
    }

    /**
     * Checks if the given character is associated with a primitive or not according to Section 2.1.3 of the ASM docs.
     *
     * @param c the character e.g. I, D, F, etc.
     * @return true if the character is associated with a primitive, false if otherwise.
     */
    private fun isPrimitive(c: Char): Boolean {
        return PRIMITIVES.containsKey(c)
    }

    /**
     * Checks if the given character is associated an object or not according to Section 2.1.3 of the ASM docs.
     *
     * @param c the character e.g. L
     * @return true if the character is associated with an object, false if otherwise.
     */
    private fun isObject(c: Char): Boolean {
        return c == 'L'
    }

    /**
     * Checks if the given character is associated an array or not according to Section 2.1.3 of the ASM docs.
     *
     * @param c the character e.g. [
     * @return true if the character is associated with an array, false if otherwise.
     */
    private fun isArray(c: Char): Boolean {
        return c == '['
    }

    /**
     * Converts all primitive characters in a signature to their descriptive names e.g. I -> INTEGER.
     *
     * @param signature the type signature to parse and insert descriptive names into.
     * @return the type signature with all primitive codes converted to their descriptive names.
     */
    private fun convertAllPrimitivesToName(signature: String): String {
        val sb = StringBuilder()
        val sigArr = signature.toCharArray()
        IntStream.range(0, sigArr.size).mapToObj { i: Int -> sigArr[i] }.forEach { c: Char ->
            if (isPrimitive(c)) {
                sb.append(PRIMITIVES[c])
            } else {
                sb.append(c)
            }
        }
        return sb.toString()
    }

    /**
     * Given an arithmetic operator, returns the type.
     *
     * @param operation an arithmetic operator.
     * @return the type of the operator. If the operator is invalid, will return "UNKNOWN".
     */
    @JvmStatic
    fun getOperatorType(operation: String?): String {
        if (operation == null) return "UNKNOWN"
        return if (!OPERANDS.contains(operation.substring(1))) "UNKNOWN" else stackType(operation[0])
    }

    /**
     * From the ASM docs: The ILOAD, LLOAD, FLOAD, DLOAD, and ALOAD instructions read a local variable and push its
     * value on the operand stack. They take as argument the index i of the local variable that must be read.
     *
     * @param line the possible LOAD instruction.
     * @return true if the line is a LOAD instruction, false if otherwise.
     */
    @JvmStatic
    fun isLoad(line: String?): Boolean {
        if (line == null) return false
        if (line.length != 5) return false
        val type = line[0]
        return if (type != 'I' && type != 'L' && type != 'F' && type != 'D' && type != 'A') false else "LOAD".contains(line.substring(1))
    }

    /**
     * From the ASM docs: ISTORE, LSTORE, FSTORE, DSTORE and ASTORE instructions pop a value from the operand stack
     * and store it in a local variable designated by its index i.
     *
     * @param line the possible STORE instruction.
     * @return true if the line is a STORE instruction, false if otherwise.
     */
    @JvmStatic
    fun isStore(line: String?): Boolean {
        if (line == null) return false
        if (line.length != 6) return false
        val type = line[0]
        return if (type != 'I' && type != 'L' && type != 'F' && type != 'D' && type != 'A') false else "STORE".contains(line.substring(1))
    }

    /**
     * From the ASM docs: xADD, xSUB, xMUL, xDIV and xREM correspond to the +,
     * -, *, / and % operations, where x is either I, L, F or D.
     *
     *
     * The logic operators only over I and L. These are SHL, SHR, USHR, AND, OR, and XOR.
     *
     * @param line the possible operand instruction.
     * @return true if the line is an operand instruction, false if otherwise.
     */
    @JvmStatic
    fun isOperator(line: String?): Boolean {
        if (line == null) return false
        if (line.length > 5 || line.length < 3) return false
        val type = line[0]
        if (type != 'I' && type != 'L' && type != 'F' && type != 'D') return false
        if (line.contains("SHL") || line.contains("SHR") || line.contains("USHR") ||
                line.contains("AND") || line.contains("OR") || line.contains("XOR")) {
            if (type != 'I' && type != 'L') return false
        }
        return OPERANDS.contains(line.substring(1))
    }

    /**
     * Determines if the given line is a constant of the pattern xCONST_n found in the
     * [org.objectweb.asm.MethodVisitor.visitInsn] hook.
     *
     * @param line the string to evaluate.
     * @return true if line represents a constant, false if otherwise.
     */
    @JvmStatic
    fun isConstant(line: String?): Boolean {
        if (line == null) return false
        if (line.length < 6) return false
        if ("ACONST_NULL" == line) return true
        val type = line[0]
        return line.contains("CONST_") && (type == 'I' || type == 'F' || type == 'D' || type == 'L') && line.length > 7
    }

    /**
     * Parses an operator to determine which Operator enum is associated with it.
     *
     * @param line an operator String e.g. IADD.
     * @return the  [za.ac.sun.plume.domain.enums.Operators] enum associated with the given operator string.
     */
    @JvmStatic
    fun parseOperator(line: String): Operators {
        return Operators.valueOf(line)
    }

    /**
     * Determines if the given string is a jump statement.
     *
     * @param line the opcode.
     * @return true if the given string is a jump statement, false if otherwise.
     */
    @JvmStatic
    fun isJumpStatement(line: String): Boolean {
        return NULLARY_JUMPS.contains(line) || UNARY_JUMPS.contains(line) || BINARY_JUMPS.contains(line)
    }

    /**
     * Parses the jump statement equality and returns the opposite.
     *
     * @param jumpStatement the string of a jump statement e.g. IF_ICMPGE.
     * @return the [Equality] of the opposite jump statement, UNKNOWN if it could not be determined.
     */
    @JvmStatic
    fun parseAndFlipEquality(jumpStatement: String): Equality {
        return when (parseEquality(jumpStatement)) {
            Equality.EQ -> Equality.NE
            Equality.NE -> Equality.EQ
            Equality.LT -> Equality.GE
            Equality.GE -> Equality.LT
            Equality.GT -> Equality.LE
            Equality.LE -> Equality.GT
            else -> Equality.UNKNOWN
        }
    }

    /**
     * Parses the equality of the given jump statement.
     *
     * @param jumpStatement the string of a jump statement e.g. IF_ICMPGE.
     * @return the [Equality] of the jump statement, UNKNOWN if it could not be determined.
     */
    @JvmStatic
    fun parseEquality(jumpStatement: String): Equality {
        if (UNARY_JUMPS.contains(jumpStatement)) {
            val eq = jumpStatement.substring(2)
            if ("NULL" == eq) return Equality.EQ else if ("NONNULL" == eq) return Equality.NE
            return Equality.valueOf(eq)
        } else if (BINARY_JUMPS.contains(jumpStatement)) {
            val eq = jumpStatement.substring(7)
            return Equality.valueOf(eq)
        }
        return Equality.UNKNOWN
    }

    /**
     * Determines the type of the given binary jump.
     *
     * @param line the binary jump.
     * @return INTEGER or OBJECT of the binary jump, UNKNOWN if the input is invalid.
     */
    @JvmStatic
    fun getBinaryJumpType(line: String?): String {
        if (line == null || !BINARY_JUMPS.contains(line)) return "UNKNOWN"
        return if (line[3] == 'I') "INTEGER" else if (line[3] == 'A') "OBJECT" else "UNKNOWN"
    }

    /**
     * Parses the jump operation and returns its [JumpAssociations] used in control flow construction.
     *
     * @param jumpOp the jump operation e.g. IF_ICMPLE, GOTO, etc.
     * @return the jump association of the jump operation, null if there is no association.
     */
    @JvmStatic
    fun parseJumpAssociation(jumpOp: String): JumpAssociations? {
        if (BINARY_JUMPS.contains(jumpOp)) {
            return JumpAssociations.IF_CMP
        } else if (NULLARY_JUMPS.contains(jumpOp)) {
            return JumpAssociations.JUMP
        }
        return null
    }

}