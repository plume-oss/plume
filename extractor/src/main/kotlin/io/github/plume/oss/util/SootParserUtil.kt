package io.github.plume.oss.util

import org.objectweb.asm.Opcodes
import io.github.plume.oss.domain.enums.EvaluationStrategy
import io.github.plume.oss.domain.enums.ModifierType
import io.github.plume.oss.util.ExtractorConst.BIN_OPS
import io.github.plume.oss.util.ExtractorConst.PRIMITIVES
import java.util.*

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
    fun determineModifiers(access: Int, name: String? = null): EnumSet<ModifierType> {
        val modifiers = EnumSet.of(ModifierType.VIRTUAL)
        if ("<init>" == name) modifiers.add(ModifierType.CONSTRUCTOR)
        var remaining = access
        var bit: Int
        while (remaining != 0) {
            bit = Integer.lowestOneBit(remaining)
            when (bit) {
                Opcodes.ACC_STATIC -> {
                    modifiers.add(ModifierType.STATIC)
                    modifiers.remove(ModifierType.VIRTUAL)
                }
                Opcodes.ACC_PUBLIC -> modifiers.add(ModifierType.PUBLIC)
                Opcodes.ACC_PRIVATE -> {
                    modifiers.add(ModifierType.PRIVATE)
                    modifiers.remove(ModifierType.VIRTUAL)
                }
                Opcodes.ACC_PROTECTED -> modifiers.add(ModifierType.PROTECTED)
                Opcodes.ACC_NATIVE -> modifiers.add(ModifierType.NATIVE)
                Opcodes.ACC_ABSTRACT -> modifiers.add(ModifierType.ABSTRACT)
                Opcodes.ACC_FINAL -> modifiers.remove(ModifierType.VIRTUAL)
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
    fun determineEvaluationStrategy(paramType: String, isMethodReturn: Boolean): EvaluationStrategy {
        return if (isArrayType(paramType) || !PRIMITIVES.contains(paramType))
            if (isMethodReturn) EvaluationStrategy.BY_SHARING else EvaluationStrategy.BY_REFERENCE
        else EvaluationStrategy.BY_VALUE
    }

    /**
     * Parses the jump statement equality and returns the opposite.
     *
     * @param jumpStatement the string of a jump statement e.g. NEQ.
     * @return the of the opposite jump statement, NOP if it could not be determined.
     */
    @JvmStatic
    fun parseAndFlipEquality(jumpStatement: String): String {
        return when (BIN_OPS[jumpStatement]) {
            "EQ" -> "NEQ"
            "NEQ" -> "EQ"
            "LT" -> "GTE"
            "GTE" -> "LT"
            "LTE" -> "GT"
            "GT" -> "LTE"
            else -> "NOP"
        }
    }

    @JvmStatic
    fun isArrayType(type: String): Boolean {
        return type.contains("[]")
    }
}