package za.ac.sun.plume.domain.meta

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test
import org.objectweb.asm.Label

class JumpInfoTest {

    @Test
    fun equalsTest() {
        val value1 = JumpInfo(STRING_1, LABEL_1, LABEL_1, INT_1)
        val value2 = JumpInfo(STRING_1, LABEL_1, LABEL_1, INT_1)
        val value3 = JumpInfo(STRING_2, LABEL_1, LABEL_1, INT_1)
        val value4 = JumpInfo(STRING_1, LABEL_2, LABEL_1, INT_1)
        val value5 = JumpInfo(STRING_1, LABEL_1, LABEL_2, INT_1)
        val value6 = JumpInfo(STRING_1, LABEL_1, LABEL_1, INT_2)
        assertEquals(value1, value1)
        assertEquals(value1, value2)
        assertNotEquals(value1, value3)
        assertNotEquals(value1, value4)
        assertNotEquals(value1, value5)
        assertNotEquals(value1, value6)
        assertNotEquals(value1, STRING_1)
        assertEquals(value1.hashCode(), value2.hashCode())
        assertNotEquals(value1.hashCode(), value3.hashCode())
    }

    @Test
    fun toStringTest() {
        val value1 = JumpInfo(STRING_1, LABEL_1, LABEL_1, INT_1)
        assertEquals("[$INT_1] $STRING_1 @ $LABEL_1 -> $LABEL_1", value1.toString())
    }

    companion object {
        const val INT_1 = 1
        const val INT_2 = 2
        const val STRING_1 = "TEST1"
        const val STRING_2 = "TEST2"
        val LABEL_1 = Label()
        val LABEL_2 = Label()
    }

}