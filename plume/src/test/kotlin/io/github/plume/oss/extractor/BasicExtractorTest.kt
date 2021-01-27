package io.github.plume.oss.extractor

import io.github.plume.oss.Extractor
import io.github.plume.oss.drivers.DriverFactory
import io.github.plume.oss.drivers.GraphDatabase
import io.github.plume.oss.drivers.OverflowDbDriver
import io.shiftleft.codepropertygraph.generated.nodes.*
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.io.File

class BasicExtractorTest {
    companion object {
        private val driver = DriverFactory(GraphDatabase.OVERFLOWDB) as OverflowDbDriver
        private val TEST_PATH = "extractor_tests${File.separator}"
        private lateinit var extractor: Extractor
        private lateinit var validSourceFile: File
        private lateinit var validClassFile: File
        private lateinit var validDirectory: File
        private lateinit var validJarFile: File

        private fun getTestResource(dir: String): File {
            val resourceURL = BasicExtractorTest::class.java.classLoader.getResource(dir)
                ?: throw java.lang.NullPointerException("Unable to obtain test resource")
            return File(resourceURL.file)
        }

        @BeforeAll
        @JvmStatic
        fun setUpAll() {
            validSourceFile = getTestResource("${TEST_PATH}Test1.java")
            validClassFile = getTestResource("${TEST_PATH}Test2.class")
            validJarFile = getTestResource("${TEST_PATH}Test3.jar")
            validDirectory = getTestResource("${TEST_PATH}dir_test")
            extractor = Extractor(driver)
        }
    }

    @AfterEach
    fun tearDown() {
        driver.clearGraph()
    }

    @Test
    fun validSourceFileTest() {
        extractor.load(validSourceFile)
        extractor.project()
        val graph = driver.getWholeGraph()
        val vertices = graph.vertices()
        assertNotNull(
            vertices.filterIsInstance<NewNamespaceBlockBuilder>().find { it.build().name() == "extractor_tests" })
        vertices.filterIsInstance<NewFileBuilder>().find { it.build().name() == "extractor_tests.Test1" }
            .let { assertNotNull(it) }
        vertices.filterIsInstance<NewMethodBuilder>().find { it.build().name() == "main" }.let { assertNotNull(it) }
        vertices.filterIsInstance<NewLocalBuilder>().find { it.build().name() == "a" }
            .let { assertNotNull(it); assertEquals("byte", it!!.build().typeFullName()) }
        vertices.filterIsInstance<NewLocalBuilder>().find { it.build().name() == "b" }
            .let { assertNotNull(it); assertEquals("byte", it!!.build().typeFullName()) }
        vertices.filterIsInstance<NewLocalBuilder>().find { it.build().name() == "c" }
            .let { assertNotNull(it); assertEquals("int", it!!.build().typeFullName()) }
        vertices.filterIsInstance<NewCallBuilder>().find { it.build().name() == "ADD" }.let { assertNotNull(it) }
    }

    @Test
    fun validClassFileTest() {
        extractor.load(validClassFile)
        extractor.project()
        val graph = driver.getWholeGraph()
        val vertices = graph.vertices()
        assertNotNull(
            vertices.filterIsInstance<NewNamespaceBlockBuilder>().find { it.build().name() == "extractor_tests" })
        vertices.filterIsInstance<NewFileBuilder>().find { it.build().name() == "extractor_tests.Test2" }
            .let { assertNotNull(it) }
        vertices.filterIsInstance<NewMethodBuilder>().find { it.build().name() == "main" }.let { assertNotNull(it) }
        vertices.filterIsInstance<NewLocalBuilder>().find { it.build().name() == "l1" }
            .let { assertNotNull(it); assertEquals("byte", it!!.build().typeFullName()) }
        vertices.filterIsInstance<NewLocalBuilder>().find { it.build().name() == "l2" }
            .let { assertNotNull(it); assertEquals("byte", it!!.build().typeFullName()) }
        vertices.filterIsInstance<NewLocalBuilder>().find { it.build().name() == "l3" }
            .let { assertNotNull(it); assertEquals("int", it!!.build().typeFullName()) }
        vertices.filterIsInstance<NewCallBuilder>().find { it.build().name() == "ADD" }.let { assertNotNull(it) }
    }

    @Test
    fun validDirectoryTest() {
        extractor.load(validDirectory)
        extractor.project()
        val graph = driver.getProgramStructure()
        val vertices = graph.vertices()
        vertices.filterIsInstance<NewFileBuilder>().let { fileList ->
            assertNotNull(fileList.firstOrNull { it.build().name() == "extractor_tests.dir_test.Dir1" })
            assertNotNull(fileList.firstOrNull { it.build().name() == "extractor_tests.dir_test.pack.Dir2" })
        }
        vertices.filterIsInstance<NewNamespaceBlockBuilder>().let { fileList ->
            assertNotNull(fileList.firstOrNull { it.build().name() == "dir_test" })
            assertNotNull(fileList.firstOrNull { it.build().name() == "extractor_tests" })
            assertNotNull(fileList.firstOrNull { it.build().name() == "pack" })
        }
    }

    @Test
    fun loadFileThatDoesNotExistTest() {
        Assertions.assertThrows(NullPointerException::class.java) { extractor.load(File("dne.class")) }
    }
}