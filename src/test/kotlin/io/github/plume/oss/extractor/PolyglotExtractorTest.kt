package io.github.plume.oss.extractor

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import io.github.plume.oss.Extractor
import io.github.plume.oss.drivers.DriverFactory
import io.github.plume.oss.drivers.GraphDatabase
import io.github.plume.oss.drivers.TinkerGraphDriver
import java.io.File

class PolyglotExtractorTest {
    companion object {
        private val driver = DriverFactory(GraphDatabase.TINKER_GRAPH) as TinkerGraphDriver
        private val TEST_PATH = "extractor_tests${File.separator}"
        private lateinit var CLS_PATH: File
        private lateinit var extractor: Extractor
        private lateinit var validPy2File: File
        private lateinit var validJsFile: File
        private lateinit var polyglotDir: File

        private fun getTestResource(dir: String): File {
            val resourceURL = PolyglotExtractorTest::class.java.classLoader.getResource(dir)
                    ?: throw java.lang.NullPointerException("Unable to obtain test resource")
            return File(resourceURL.file)
        }

        @BeforeAll
        @JvmStatic
        fun setUpAll() {
            validPy2File = getTestResource("${TEST_PATH}Test4.py")
            validJsFile = getTestResource("${TEST_PATH}Test5.js")
            polyglotDir = getTestResource("${TEST_PATH}polyglot")
            CLS_PATH = File(
                getTestResource(TEST_PATH).absolutePath.replace(System.getProperty("user.dir") + File.separator, "").removeSuffix(
                    TEST_PATH.replace(File.separator, "")))
            extractor = Extractor(driver, CLS_PATH)
        }
    }

    @AfterEach
    fun tearDown() {
        driver.clearGraph()
    }

    @Test
    fun validPy2Test() {
        extractor.load(validPy2File)
        extractor.project()
    }

    @Test
    fun validJsTest() {
        extractor.load(validJsFile)
        extractor.project()
    }

    @Test
    fun compileMultipleLanguagesTest() {
        extractor.load(polyglotDir)
        extractor.project()
    }

}