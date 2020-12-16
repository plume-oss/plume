package io.github.plume.oss.extractor

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import io.github.plume.oss.Extractor
import io.github.plume.oss.domain.models.vertices.LiteralVertex
import io.github.plume.oss.drivers.DriverFactory
import io.github.plume.oss.drivers.GraphDatabase
import io.github.plume.oss.drivers.TinkerGraphDriver
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

class UpdateGraphTest {
    companion object {
        private val driver = DriverFactory(GraphDatabase.TINKER_GRAPH) as TinkerGraphDriver
        private val TEST_PATH = "extractor_tests${File.separator}update_test${File.separator}"
        private lateinit var extractor: Extractor
        private lateinit var testFile1: File
        private lateinit var testFile2: File
        private lateinit var testFile2Update: File

        private fun getTestResource(dir: String): File {
            val resourceURL = UpdateGraphTest::class.java.classLoader.getResource(dir)
                    ?: throw java.lang.NullPointerException("Unable to obtain test resource")
            return File(resourceURL.file)
        }

        @BeforeAll
        @JvmStatic
        fun setUpAll() {
            testFile1 = getTestResource("${TEST_PATH}UpdateTest1.java")
            testFile2 = getTestResource("${TEST_PATH}UpdateTest2.java")
            testFile2Update = getTestResource("${TEST_PATH}UpdateTest2.txt")
            extractor = Extractor(driver)
        }
    }

    @AfterEach
    fun tearDown() {
        driver.clearGraph()
    }

    @Test
    fun testGraphUpdate() {
        // Initial projection
        listOf(testFile1, testFile2).forEach { extractor.load(it) }
        extractor.project()
        val g1 = driver.getWholeGraph()
        // Update file and do an update projection
        testFile2 = rewriteFileContents(testFile2, testFile2Update)
        listOf(testFile1, testFile2).forEach { extractor.load(it) }
        extractor.project()
        val g2 = driver.getWholeGraph()
        assertTrue(g1.vertices().filterIsInstance<LiteralVertex>().any { it.code == "5" })
        assertTrue(g2.vertices().filterIsInstance<LiteralVertex>().any { it.code == "9" })
        assertTrue(g2.vertices().filterIsInstance<LiteralVertex>().none { it.code == "5" })
        assertTrue(g1.vertices().filterIsInstance<LiteralVertex>().none { it.code == "9" })
        assertFalse(g1 == g2)
    }

    private fun rewriteFileContents(tgt: File, incoming: File): File {
        FileOutputStream(tgt, false).use { fos ->
            FileInputStream(incoming).use { fis ->
                val buf = ByteArray(4096)
                while (true) {
                    val read = fis.read(buf)
                    if (read == -1) {
                        break
                    }
                    fos.write(buf, 0, read)
                }
            }
        }
        return File(tgt.absolutePath)
    }
}