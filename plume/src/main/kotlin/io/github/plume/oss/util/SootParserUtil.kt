package io.github.plume.oss.util

import io.shiftleft.codepropertygraph.generated.EvaluationStrategies.*
import io.shiftleft.codepropertygraph.generated.ModifierTypes.*
import io.shiftleft.codepropertygraph.generated.Operators
import org.objectweb.asm.Opcodes
import java.util.stream.IntStream

object SootParserUtil {
    private val PRIMITIVES: Map<Char, String> = mapOf(
        'Z' to "boolean",
        'C' to "char",
        'B' to "byte",
        'S' to "short",
        'I' to "int",
        'F' to "float",
        'J' to "long",
        'D' to "double",
        'V' to "void"
    )

    /**
     * Given a method signature, returns a list of all the parameters separated into a list.
     *
     * @param signature the raw method signature from ASM5
     * @return a list of the parameters
     */
    @JvmStatic
    fun obtainParameters(signature: String): List<String> {
        val parameters: MutableList<String> = ArrayList()
        val sigArr = signature.toCharArray()
        val sb = StringBuilder()
        IntStream.range(0, sigArr.size).mapToObj { i: Int -> sigArr[i] }.forEach { c: Char ->
            if (c == ';') {
                val prefix = sb.toString()
                    .replace("[", "")
                    .substring(1)
                    .replace("/", ".")
                val suffix = sb.filter { it == '[' }
                    .map { "[]" }
                    .joinToString("")
                parameters.add("$prefix$suffix")
                sb.delete(0, sb.length)
            } else if (isPrimitive(c) && sb.indexOf("L") == -1) {
                parameters.add("${PRIMITIVES[c]!!}${sb.filter { it == '[' }.map { "[]" }.joinToString("")}")
                sb.delete(0, sb.length)
            } else if (isObject(c)) {
                sb.append(c)
            } else if (isArray(c)) {
                sb.append(c)
            } else sb.append(c)
        }
        return parameters
    }

    /**
     * Given the ASM5 access parameter and method name, determines the modifier types.
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
    fun determineModifiers(access: Int, name: String? = null): Set<String> {
        val modifiers = mutableSetOf(VIRTUAL)
        if ("<init>" == name) modifiers.add(CONSTRUCTOR)
        var remaining = access
        var bit: Int
        while (remaining != 0) {
            bit = Integer.lowestOneBit(remaining)
            when (bit) {
                Opcodes.ACC_STATIC -> {
                    modifiers.add(STATIC)
                    modifiers.remove(VIRTUAL)
                }
                Opcodes.ACC_PUBLIC -> modifiers.add(PUBLIC)
                Opcodes.ACC_PRIVATE -> {
                    modifiers.add(PRIVATE)
                    modifiers.remove(VIRTUAL)
                }
                Opcodes.ACC_PROTECTED -> modifiers.add(PROTECTED)
                Opcodes.ACC_NATIVE -> modifiers.add(NATIVE)
                Opcodes.ACC_ABSTRACT -> modifiers.add(ABSTRACT)
                Opcodes.ACC_FINAL -> modifiers.remove(VIRTUAL)
            }
            remaining -= bit
        }
        return modifiers
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
    fun determineEvaluationStrategy(paramType: String, isMethodReturn: Boolean): String {
        return if (isArrayType(paramType) || !PRIMITIVES.values.contains(paramType))
            if (isMethodReturn) BY_SHARING else BY_REFERENCE
        else BY_VALUE
    }

    /**
     * Parses the jump statement equality and returns the opposite.
     *
     * @param jumpStatement the string of a jump statement e.g. NEQ.
     * @return the of the opposite jump statement, NOP if it could not be determined.
     */
    @JvmStatic
    fun parseAndFlipEquality(jumpStatement: String): String {
        return when (SootToPlumeUtil.parseBinopExpr(jumpStatement)) {
            Operators.equals -> Operators.notEquals
            Operators.notEquals -> Operators.equals
            Operators.lessThan -> Operators.greaterEqualsThan
            Operators.greaterEqualsThan -> Operators.lessThan
            Operators.lessEqualsThan -> Operators.greaterThan
            Operators.greaterThan -> Operators.lessEqualsThan
            else -> "<operator>.noOperation"
        }
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

    @JvmStatic
    fun isArrayType(type: String): Boolean {
        return type.contains("[]")
    }
}