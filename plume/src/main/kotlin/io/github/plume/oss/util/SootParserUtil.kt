package io.github.plume.oss.util

import org.objectweb.asm.Opcodes
import io.github.plume.oss.util.ExtractorConst.BIN_OPS
import io.github.plume.oss.util.ExtractorConst.PRIMITIVES
import io.shiftleft.codepropertygraph.generated.EvaluationStrategies.*
import io.shiftleft.codepropertygraph.generated.ModifierTypes.*
import io.shiftleft.codepropertygraph.generated.Operators

object SootParserUtil {
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
        return if (isArrayType(paramType) || !PRIMITIVES.contains(paramType))
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

    @JvmStatic
    fun isArrayType(type: String): Boolean {
        return type.contains("[]")
    }
}