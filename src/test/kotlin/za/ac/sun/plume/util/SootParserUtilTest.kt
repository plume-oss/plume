package za.ac.sun.plume.util

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.objectweb.asm.Opcodes
import za.ac.sun.plume.domain.enums.EvaluationStrategy
import za.ac.sun.plume.domain.enums.ModifierType
import za.ac.sun.plume.util.SootParserUtil.determineEvaluationStrategy
import za.ac.sun.plume.util.SootParserUtil.determineModifiers
import za.ac.sun.plume.util.SootParserUtil.isArrayType
import java.util.*

class SootParserUtilTest {

    @Test
    fun testDetermineEvaluationStrategy() {
        assertEquals(determineEvaluationStrategy("int", true), EvaluationStrategy.BY_VALUE)
        assertEquals(determineEvaluationStrategy("byte", false), EvaluationStrategy.BY_VALUE)
        assertEquals(determineEvaluationStrategy("int[]", true), EvaluationStrategy.BY_SHARING)
        assertEquals(determineEvaluationStrategy("java.util.String", true), EvaluationStrategy.BY_SHARING)
        assertEquals(determineEvaluationStrategy("int[]", false), EvaluationStrategy.BY_REFERENCE)
        assertEquals(determineEvaluationStrategy("java.util.String", false), EvaluationStrategy.BY_REFERENCE)
    }

    @Test
    fun testDetermineModifiers() {
        assertEquals(EnumSet.of(ModifierType.CONSTRUCTOR, ModifierType.ABSTRACT, ModifierType.VIRTUAL),
                determineModifiers(Opcodes.ACC_ABSTRACT, "<init>"))
        assertEquals(EnumSet.of(ModifierType.STATIC, ModifierType.PUBLIC),
                determineModifiers(Opcodes.ACC_STATIC + Opcodes.ACC_PUBLIC, "test"))
        assertEquals(EnumSet.of(ModifierType.VIRTUAL, ModifierType.PROTECTED),
                determineModifiers(Opcodes.ACC_PROTECTED, "test"))
        assertEquals(EnumSet.of(ModifierType.NATIVE),
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