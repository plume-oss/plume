package za.ac.sun.plume.domain.meta

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.objectweb.asm.Label

class LineInfoTest {
    lateinit var testModel: LineInfo
    private val lineNumber = 1
    private val label1 = Label()

    @BeforeEach
    fun setUp() {
        testModel = LineInfo(lineNumber)
        testModel.associatedLabels.add(label1)
    }

    @Test
    fun toStringTest() {
        assertTrue("1: [$label1]" == testModel.toString())
        val label2 = Label()
        testModel.associatedLabels.add(label2)
        assertTrue("1: [$label1, $label2]" == testModel.toString())
    }

    @Test
    fun equalsTest() {
        val testModel1 = LineInfo(1)
        assertTrue(testModel == testModel1)
        assertTrue(testModel.hashCode() == testModel1.hashCode())
        val testModel2 = LineInfo(2)
        Assertions.assertFalse(testModel == testModel2)
        Assertions.assertFalse(testModel.hashCode() == testModel2.hashCode())
        Assertions.assertFalse(testModel.equals("Test"))
    }
}