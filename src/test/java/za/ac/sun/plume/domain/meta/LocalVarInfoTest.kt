package za.ac.sun.plume.domain.meta

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.objectweb.asm.Label

class LocalVarInfoTest {

    lateinit var testModel: LocalVarInfo
    private val startLabel = Label()
    private val endLabel = Label()

    @BeforeEach
    fun setUp() {
        testModel = LocalVarInfo(1, "test", "I", startLabel, endLabel)
    }

    @Test
    fun toStringTest() {
        assertTrue("LOCAL VAR I test @ 1" == testModel.toString())
        testModel.debugName = "test1"
        assertFalse("LOCAL VAR I test @ 1" == testModel.toString())
        assertTrue("LOCAL VAR I test1 @ 1" == testModel.toString())
    }
}