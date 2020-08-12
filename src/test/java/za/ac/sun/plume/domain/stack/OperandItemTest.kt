package za.ac.sun.plume.domain.stack

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test

class OperandItemTest {

    inner class OperandChild(par1: String, par2: String) : OperandItem(par1, par2)

    @Test
    fun equalsTest() {
        val value1 = OperandChild(STRING_1, STRING_1)
        val value2 = OperandChild(STRING_1, STRING_1)
        val value3 = OperandChild(STRING_2, STRING_1)
        assertEquals(value1, value1)
        assertEquals(value1, value2)
        assertNotEquals(value1, value3)
        assertNotEquals(value1, STRING_1)
        assertEquals(value1.hashCode(), value2.hashCode())
        assertNotEquals(value1.hashCode(), value3.hashCode())
    }

    @Test
    fun toStringTest() {
        val value1 = OperandChild(STRING_1, STRING_1)
        assertEquals("OperandItem(id='$STRING_1', type='$STRING_1')", value1.toString())
    }

    companion object {
        const val STRING_1 = "TEST1"
        const val STRING_2 = "TEST2"
    }
}