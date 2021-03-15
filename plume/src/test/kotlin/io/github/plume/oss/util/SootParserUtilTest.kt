package io.github.plume.oss.util

import io.github.plume.oss.util.SootParserUtil.determineEvaluationStrategy
import io.github.plume.oss.util.SootParserUtil.determineModifiers
import io.github.plume.oss.util.SootParserUtil.isArrayType
import io.github.plume.oss.util.SootParserUtil.obtainParameters
import io.shiftleft.codepropertygraph.generated.EvaluationStrategies.*
import io.shiftleft.codepropertygraph.generated.ModifierTypes.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.objectweb.asm.Opcodes

class SootParserUtilTest {

    @Test
    fun testObtainingParameters() {
        assertEquals(listOf("int"), obtainParameters("I"))
        assertEquals(listOf("int", "byte"), obtainParameters("IB"))
        assertEquals(emptyList<Any>(), obtainParameters(""))
        assertEquals(listOf("java.util.String"), obtainParameters("Ljava/util/String;"))
        assertEquals(listOf("java.util.String", "long"), obtainParameters("Ljava/util/String;J"))
        assertEquals(listOf("java.util.String[]", "byte[]"), obtainParameters("[Ljava/util/String;[B"))
    }

    @Test
    fun testDetermineEvaluationStrategy() {
        assertEquals(determineEvaluationStrategy("int", true), BY_VALUE)
        assertEquals(determineEvaluationStrategy("byte", false), BY_VALUE)
        assertEquals(determineEvaluationStrategy("int[]", true), BY_SHARING)
        assertEquals(determineEvaluationStrategy("java.util.String", true), BY_SHARING)
        assertEquals(determineEvaluationStrategy("int[]", false), BY_REFERENCE)
        assertEquals(determineEvaluationStrategy("java.util.String", false), BY_REFERENCE)
    }

    @Test
    fun testDetermineModifiers() {
        assertEquals(setOf(CONSTRUCTOR, ABSTRACT, VIRTUAL), determineModifiers(Opcodes.ACC_ABSTRACT, "<init>"))
        assertEquals(setOf(STATIC, PUBLIC), determineModifiers(Opcodes.ACC_STATIC + Opcodes.ACC_PUBLIC, "test"))
        assertEquals(setOf(VIRTUAL, PROTECTED), determineModifiers(Opcodes.ACC_PROTECTED, "test"))
        assertEquals(setOf(NATIVE), determineModifiers(Opcodes.ACC_NATIVE + Opcodes.ACC_FINAL))
    }

    @Test
    fun testIsArrayType() {
        assertTrue(isArrayType("java.lang.String[]"))
        assertFalse(isArrayType("java.lang.String"))
        assertTrue(isArrayType("int[]"))
        assertFalse(isArrayType("int"))
    }

}