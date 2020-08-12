package za.ac.sun.plume.domain.meta

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test
import za.ac.sun.plume.util.ASMParserUtil

class ClassInfoTest {

    @Test
    fun equalsTest() {
        val value1 = ClassInfo(STRING_1, STRING_1, INT_1, INT_1)
        val value2 = ClassInfo(STRING_1, STRING_1, INT_1, INT_1)
        val value3 = ClassInfo(STRING_2, STRING_1, INT_1, INT_1)
        val value4 = ClassInfo(STRING_1, STRING_2, INT_1, INT_1)
        assertEquals(value1, value1)
        assertEquals(value1, value2)
        assertNotEquals(value1, value3)
        assertNotEquals(value1, value4)
        assertNotEquals(value1, STRING_1)
        assertEquals(value1.hashCode(), value2.hashCode())
        assertNotEquals(value1.hashCode(), value3.hashCode())
    }

    @Test
    fun toStringTest() {
        val value1 = ClassInfo(STRING_1, STRING_1, INT_1, INT_1)
        assertEquals("${ASMParserUtil.determineModifiers(INT_1)} $STRING_1.$STRING_1", value1.toString())
    }

    companion object {
        const val INT_1 = 1
        const val STRING_1 = "TEST1"
        const val STRING_2 = "TEST2"
    }
}