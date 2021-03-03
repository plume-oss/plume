package io.github.plume.oss.util

import io.github.plume.oss.drivers.IOverridenIdDriver
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock

class PlumeKeyProviderTest {

    companion object {
        private lateinit var mockDriver: IOverridenIdDriver
        private val originalKeyPoolSize = PlumeKeyProvider.keyPoolSize

        @BeforeAll
        @JvmStatic
        fun setUpAll() {
            mockDriver = mock(IOverridenIdDriver::class.java)
            `when`(mockDriver.getVertexIds(0, 5)).thenReturn(setOf(0L, 1L, 2L, 3L, 4L, 5L))
            `when`(mockDriver.getVertexIds(6, 11)).thenReturn(setOf(7L, 9L))
            `when`(mockDriver.getVertexIds(12, 17)).thenReturn(emptySet())
        }

        @AfterAll
        @JvmStatic
        fun tearDownAll() {
            PlumeKeyProvider.keyPoolSize = originalKeyPoolSize
        }
    }

    @Test
    fun testPlumeKeyPoolSet() {
        assertEquals(1000, PlumeKeyProvider.keyPoolSize)
        PlumeKeyProvider.keyPoolSize = -5
        assertEquals(1000, PlumeKeyProvider.keyPoolSize)
        PlumeKeyProvider.keyPoolSize = 5
        assertEquals(5, PlumeKeyProvider.keyPoolSize)
    }

    @Test
    fun testPlumeIdAllocation() {
        PlumeKeyProvider.keyPoolSize = 5
        assertEquals(6, PlumeKeyProvider.getNewId(mockDriver))
        assertEquals(8, PlumeKeyProvider.getNewId(mockDriver))
        assertEquals(10, PlumeKeyProvider.getNewId(mockDriver))
        assertEquals(11, PlumeKeyProvider.getNewId(mockDriver))
        assertEquals(12, PlumeKeyProvider.getNewId(mockDriver))
        assertEquals(13, PlumeKeyProvider.getNewId(mockDriver))
        assertEquals(14, PlumeKeyProvider.getNewId(mockDriver))
        assertEquals(15, PlumeKeyProvider.getNewId(mockDriver))
        assertEquals(16, PlumeKeyProvider.getNewId(mockDriver))
        assertEquals(17, PlumeKeyProvider.getNewId(mockDriver))
    }

}