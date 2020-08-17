package za.ac.sun.plume.util

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.objectweb.asm.Opcodes
import za.ac.sun.plume.domain.enums.EvaluationStrategies
import za.ac.sun.plume.domain.enums.ModifierTypes
import za.ac.sun.plume.util.SootParserUtil.determineEvaluationStrategy
import za.ac.sun.plume.util.SootParserUtil.determineModifiers
import za.ac.sun.plume.util.SootParserUtil.isArrayType
import java.util.*

class SootParserUtilTest {

    @Test
    fun testDetermineEvaluationStrategy() {
        assertEquals(determineEvaluationStrategy("int", true), EvaluationStrategies.BY_VALUE)
        assertEquals(determineEvaluationStrategy("byte", false), EvaluationStrategies.BY_VALUE)
        assertEquals(determineEvaluationStrategy("int[]", true), EvaluationStrategies.BY_SHARING)
        assertEquals(determineEvaluationStrategy("java.util.String", true), EvaluationStrategies.BY_SHARING)
        assertEquals(determineEvaluationStrategy("int[]", false), EvaluationStrategies.BY_REFERENCE)
        assertEquals(determineEvaluationStrategy("java.util.String", false), EvaluationStrategies.BY_REFERENCE)
    }

    @Test
    fun testDetermineModifiers() {
        assertEquals(EnumSet.of(ModifierTypes.CONSTRUCTOR, ModifierTypes.ABSTRACT, ModifierTypes.VIRTUAL),
                determineModifiers(Opcodes.ACC_ABSTRACT, "<init>"))
        assertEquals(EnumSet.of(ModifierTypes.STATIC, ModifierTypes.PUBLIC),
                determineModifiers(Opcodes.ACC_STATIC + Opcodes.ACC_PUBLIC, "test"))
        assertEquals(EnumSet.of(ModifierTypes.VIRTUAL, ModifierTypes.PROTECTED),
                determineModifiers(Opcodes.ACC_PROTECTED, "test"))
        assertEquals(EnumSet.of(ModifierTypes.NATIVE),
                determineModifiers(Opcodes.ACC_NATIVE + Opcodes.ACC_FINAL))
    }

    @Test
    fun testIsArrayType() {
        assertTrue(isArrayType("java.lang.String[]"))
        assertFalse(isArrayType("java.lang.String"))
        assertTrue(isArrayType("int[]"))
        assertFalse(isArrayType("int"))
    }

}