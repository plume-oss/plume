package za.ac.sun.plume.util

import org.objectweb.asm.Opcodes
import za.ac.sun.plume.domain.enums.EvaluationStrategies
import za.ac.sun.plume.domain.enums.ModifierTypes
import za.ac.sun.plume.util.ExtractorConst.PRIMITIVES
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
        return if (isArrayType(paramType) || !PRIMITIVES.contains(paramType))
            if (isMethodReturn) EvaluationStrategies.BY_SHARING else EvaluationStrategies.BY_REFERENCE
        else EvaluationStrategies.BY_VALUE
    }

    @JvmStatic
    fun isArrayType(type: String): Boolean {
        return type.contains("[]")
    }
}