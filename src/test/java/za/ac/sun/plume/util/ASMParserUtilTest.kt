package za.ac.sun.plume.util

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.objectweb.asm.Opcodes
import za.ac.sun.plume.domain.enums.Equality
import za.ac.sun.plume.domain.enums.EvaluationStrategies
import za.ac.sun.plume.domain.enums.JumpAssociations
import za.ac.sun.plume.domain.enums.ModifierTypes
import za.ac.sun.plume.domain.enums.Operators
import za.ac.sun.plume.util.ASMParserUtil.determineEvaluationStrategy
import za.ac.sun.plume.util.ASMParserUtil.determineModifiers
import za.ac.sun.plume.util.ASMParserUtil.getBinaryJumpType
import za.ac.sun.plume.util.ASMParserUtil.getOperatorType
import za.ac.sun.plume.util.ASMParserUtil.getReadableType
import za.ac.sun.plume.util.ASMParserUtil.getStackOperationType
import za.ac.sun.plume.util.ASMParserUtil.isConstant
import za.ac.sun.plume.util.ASMParserUtil.isJumpStatement
import za.ac.sun.plume.util.ASMParserUtil.isLoad
import za.ac.sun.plume.util.ASMParserUtil.isOperator
import za.ac.sun.plume.util.ASMParserUtil.isStore
import za.ac.sun.plume.util.ASMParserUtil.obtainMethodReturnType
import za.ac.sun.plume.util.ASMParserUtil.obtainParameters
import za.ac.sun.plume.util.ASMParserUtil.parseAndFlipEquality
import za.ac.sun.plume.util.ASMParserUtil.parseEquality
import za.ac.sun.plume.util.ASMParserUtil.parseJumpAssociation
import za.ac.sun.plume.util.ASMParserUtil.parseOperator
import java.util.*

class ASMParserUtilTest {
    @Test
    fun testObtainingParameters() {
        Assertions.assertEquals(listOf("I"), obtainParameters("(I)V"))
        Assertions.assertEquals(listOf("I", "B"), obtainParameters("(IB)V"))
        Assertions.assertEquals(emptyList<Any>(), obtainParameters("()Ljava/util/String;"))
        Assertions.assertEquals(listOf("Ljava/util/String"), obtainParameters("(Ljava/util/String;)V"))
        Assertions.assertEquals(listOf("Ljava/util/String", "J"), obtainParameters("(Ljava/util/String;J)B"))
        Assertions.assertEquals(listOf("[Ljava/util/String", "[B"), obtainParameters("([Ljava/util/String;[B)I"))
    }

    @Test
    fun testObtainMethodReturnType() {
        Assertions.assertEquals("V", obtainMethodReturnType("()V"))
        Assertions.assertEquals("Ljava/util/String", obtainMethodReturnType("(IIB)Ljava/util/String;"))
        Assertions.assertEquals("[[B", obtainMethodReturnType("()[[B"))
        Assertions.assertEquals("[Ljava/util/Double", obtainMethodReturnType("(IIB)[Ljava/util/Double;"))
    }

    @Test
    fun testDetermineEvaluationStrategy() {
        Assertions.assertEquals(determineEvaluationStrategy("I", true), EvaluationStrategies.BY_VALUE)
        Assertions.assertEquals(determineEvaluationStrategy("B", false), EvaluationStrategies.BY_VALUE)
        Assertions.assertEquals(determineEvaluationStrategy("[I", true), EvaluationStrategies.BY_SHARING)
        Assertions.assertEquals(determineEvaluationStrategy("Ljava/util/String", true), EvaluationStrategies.BY_SHARING)
        Assertions.assertEquals(determineEvaluationStrategy("[I", false), EvaluationStrategies.BY_REFERENCE)
        Assertions.assertEquals(determineEvaluationStrategy("Ljava/util/String", false), EvaluationStrategies.BY_REFERENCE)
    }

    @Test
    fun testDetermineModifiers() {
        Assertions.assertEquals(EnumSet.of(ModifierTypes.CONSTRUCTOR, ModifierTypes.ABSTRACT, ModifierTypes.VIRTUAL),
                determineModifiers(Opcodes.ACC_ABSTRACT, "<init>"))
        Assertions.assertEquals(EnumSet.of(ModifierTypes.STATIC, ModifierTypes.PUBLIC),
                determineModifiers(Opcodes.ACC_STATIC + Opcodes.ACC_PUBLIC, "test"))
        Assertions.assertEquals(EnumSet.of(ModifierTypes.VIRTUAL, ModifierTypes.PROTECTED),
                determineModifiers(Opcodes.ACC_PROTECTED, "test"))
        Assertions.assertEquals(EnumSet.of(ModifierTypes.NATIVE),
                determineModifiers(Opcodes.ACC_NATIVE + Opcodes.ACC_FINAL))
    }

    @Test
    fun testShortNameReturn() {
        Assertions.assertEquals("INTEGER", getReadableType("I"))
        Assertions.assertEquals("BOOLEAN", getReadableType("Z"))
        Assertions.assertEquals("[INTEGER", getReadableType("[I"))
        Assertions.assertEquals("String", getReadableType("Ljava/util/String"))
        Assertions.assertEquals("[Double", getReadableType("[Ljava/util/Double"))
        Assertions.assertEquals("[[LONG", getReadableType("[[J"))
    }

    @Test
    fun testIsOperator() {
        Assertions.assertTrue(isOperator("IADD"))
        Assertions.assertFalse(isOperator("IAD"))
        Assertions.assertFalse(isOperator("DSUBB"))
        Assertions.assertFalse(isOperator("JDIV"))
        Assertions.assertTrue(isOperator("FREM"))
        Assertions.assertTrue(isOperator("LMUL"))
        Assertions.assertFalse(isOperator("DUSHR"))
        Assertions.assertTrue(isOperator("IUSHR"))
        Assertions.assertTrue(isOperator("LUSHR"))
        Assertions.assertTrue(isOperator("IOR"))
        Assertions.assertTrue(isOperator("LOR"))
        Assertions.assertTrue(isOperator("IXOR"))
        Assertions.assertTrue(isOperator("LXOR"))
        Assertions.assertTrue(isOperator("LAND"))
        Assertions.assertTrue(isOperator("LSHR"))
        Assertions.assertTrue(isOperator("ISHR"))
        Assertions.assertTrue(isOperator("ISHL"))
        Assertions.assertTrue(isOperator("LSHL"))
        Assertions.assertFalse(isOperator(null))
    }

    @Test
    fun testIsStore() {
        Assertions.assertTrue(isStore("ISTORE"))
        Assertions.assertFalse(isStore("ILOAD"))
        Assertions.assertFalse(isStore("DSTOR"))
        Assertions.assertFalse(isStore("JSTORE"))
        Assertions.assertTrue(isStore("FSTORE"))
        Assertions.assertTrue(isStore("LSTORE"))
        Assertions.assertTrue(isStore("ASTORE"))
        Assertions.assertFalse(isStore(null))
    }

    @Test
    fun testIsLoad() {
        Assertions.assertTrue(isLoad("ILOAD"))
        Assertions.assertFalse(isLoad("ISTORE"))
        Assertions.assertFalse(isLoad("DLOA"))
        Assertions.assertFalse(isLoad("JLOAD"))
        Assertions.assertTrue(isLoad("FLOAD"))
        Assertions.assertTrue(isLoad("LLOAD"))
        Assertions.assertTrue(isLoad("ALOAD"))
        Assertions.assertFalse(isLoad(null))
    }

    @Test
    fun testIsConstant() {
        Assertions.assertTrue(isConstant("ACONST_NULL"))
        Assertions.assertTrue(isConstant("ICONST_0"))
        Assertions.assertTrue(isConstant("FCONST_2"))
        Assertions.assertTrue(isConstant("DCONST_1"))
        Assertions.assertTrue(isConstant("LCONST_1"))
        Assertions.assertFalse(isConstant("JCONST_3"))
        Assertions.assertFalse(isConstant("JCONST_392831093289210"))
        Assertions.assertFalse(isConstant("JCO"))
        Assertions.assertFalse(isConstant(null))
    }

    @Test
    fun testStackOperationType() {
        Assertions.assertEquals("INTEGER", getStackOperationType("ILOAD"))
        Assertions.assertEquals("OBJECT", getStackOperationType("ASTORE"))
        Assertions.assertEquals("LONG", getStackOperationType("LLOAD"))
        Assertions.assertEquals("UNKNOWN", getStackOperationType("[LOAD"))
        Assertions.assertEquals("UNKNOWN", getStackOperationType("JSTORE"))
        Assertions.assertEquals("UNKNOWN", getStackOperationType("IITEST"))
        Assertions.assertEquals("UNKNOWN", getStackOperationType("LSTOREL"))
    }

    @Test
    fun testOperatorType() {
        Assertions.assertEquals("INTEGER", getOperatorType("IADD"))
        Assertions.assertEquals("OBJECT", getOperatorType("ASUB"))
        Assertions.assertEquals("LONG", getOperatorType("LADD"))
        Assertions.assertEquals("UNKNOWN", getOperatorType("[DIV"))
        Assertions.assertEquals("UNKNOWN", getOperatorType("JM"))
        Assertions.assertEquals("UNKNOWN", getOperatorType("IITESTLL"))
        Assertions.assertEquals("UNKNOWN", getOperatorType("LDIVL"))
        Assertions.assertEquals("LONG", getOperatorType("LOR"))
        Assertions.assertEquals("INTEGER", getOperatorType("IAND"))
        Assertions.assertEquals("INTEGER", getOperatorType("IUSHR"))
        Assertions.assertEquals("UNKNOWN", getOperatorType(null))
    }

    @Test
    fun testParseOperator() {
        Assertions.assertEquals(Operators.IADD, parseOperator("IADD"))
        Assertions.assertEquals(Operators.LADD, parseOperator("LADD"))
        Assertions.assertEquals(Operators.DADD, parseOperator("DADD"))
        Assertions.assertEquals(Operators.FADD, parseOperator("FADD"))
        Assertions.assertEquals(Operators.LOR, parseOperator("LOR"))
    }

    @Test
    fun testIsJumpStatement() {
        Assertions.assertTrue(isJumpStatement("IF_ICMPEQ"))
        Assertions.assertTrue(isJumpStatement("IFNE"))
        Assertions.assertTrue(isJumpStatement("IFNONNULL"))
        Assertions.assertTrue(isJumpStatement("LOOKUPSWITCH"))
        Assertions.assertFalse(isJumpStatement("IF"))
    }

    @Test
    fun testParseEquality() {
        Assertions.assertEquals(Equality.EQ, parseEquality("IF_ICMPEQ"))
        Assertions.assertEquals(Equality.EQ, parseEquality("IFNULL"))
        Assertions.assertEquals(Equality.LE, parseEquality("IFLE"))
        Assertions.assertEquals(Equality.NE, parseEquality("IFNONNULL"))
        Assertions.assertEquals(Equality.LE, parseEquality("IF_ICMPLE"))
        Assertions.assertEquals(Equality.UNKNOWN, parseEquality("GOTO"))
        Assertions.assertEquals(Equality.UNKNOWN, parseEquality("IF_JCMPLE"))
    }

    @Test
    fun testParseAndFlipEquality() {
        Assertions.assertEquals(Equality.NE, parseAndFlipEquality("IF_ICMPEQ"))
        Assertions.assertEquals(Equality.NE, parseAndFlipEquality("IFNULL"))
        Assertions.assertEquals(Equality.GT, parseAndFlipEquality("IFLE"))
        Assertions.assertEquals(Equality.EQ, parseAndFlipEquality("IFNONNULL"))
        Assertions.assertEquals(Equality.GT, parseAndFlipEquality("IF_ICMPLE"))
        Assertions.assertEquals(Equality.GE, parseAndFlipEquality("IFLT"))
        Assertions.assertEquals(Equality.LE, parseAndFlipEquality("IF_ICMPGT"))
        Assertions.assertEquals(Equality.GT, parseAndFlipEquality("IF_ICMPLE"))
        Assertions.assertEquals(Equality.UNKNOWN, parseAndFlipEquality("GOTO"))
        Assertions.assertEquals(Equality.UNKNOWN, parseAndFlipEquality("IF_JCMPLE"))
    }

    @Test
    fun testGetBinaryJumpType() {
        Assertions.assertEquals("INTEGER", getBinaryJumpType("IF_ICMPEQ"))
        Assertions.assertEquals("OBJECT", getBinaryJumpType("IF_ACMPEQ"))
        Assertions.assertEquals("UNKNOWN", getBinaryJumpType("IF_LCMPEQ"))
        Assertions.assertEquals("UNKNOWN", getBinaryJumpType(null))
    }

    @Test
    fun testParseJumpAssociation() {
        Assertions.assertEquals(JumpAssociations.JUMP, parseJumpAssociation("GOTO"))
        Assertions.assertEquals(JumpAssociations.IF_CMP, parseJumpAssociation("IF_ACMPEQ"))
        Assertions.assertNull(parseJumpAssociation("IF_LCMPEQ"))
    }
}