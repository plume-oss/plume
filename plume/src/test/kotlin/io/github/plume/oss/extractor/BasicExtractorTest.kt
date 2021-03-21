package io.github.plume.oss.extractor

import io.github.plume.oss.Extractor
import io.github.plume.oss.drivers.DriverFactory
import io.github.plume.oss.drivers.GraphDatabase
import io.github.plume.oss.drivers.OverflowDbDriver
import io.github.plume.oss.metrics.CacheMetrics
import io.github.plume.oss.options.ExtractorOptions
import io.github.plume.oss.store.LocalCache
import io.shiftleft.codepropertygraph.generated.Operators
import io.shiftleft.codepropertygraph.generated.nodes.Call
import io.shiftleft.codepropertygraph.generated.nodes.Local
import io.shiftleft.codepropertygraph.generated.nodes.Method
import io.shiftleft.codepropertygraph.generated.nodes.NamespaceBlock
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import overflowdb.Graph
import java.io.File
import io.shiftleft.codepropertygraph.generated.nodes.File as ODBFile

class BasicExtractorTest {
    companion object {
        private val driver = DriverFactory(GraphDatabase.OVERFLOWDB) as OverflowDbDriver
        private val TEST_PATH = "extractor_tests/"
        private val sep = File.separator
        private lateinit var extractor: Extractor
        private lateinit var g: Graph
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
            ExtractorOptions.methodChunkSize = 5
            extractor = Extractor(driver)
        }
    }

    @AfterEach
    fun tearDown() {
        assertTrue(CacheMetrics.getHits() >= 0)
        assertTrue(CacheMetrics.getMisses() >= 0)
        LocalCache.clear()
        CacheMetrics.reset()
        driver.clearGraph()
        if (!g.isClosed) g.close()
    }

    @Test
    fun validSourceFileTest() {
        extractor.load(validSourceFile)
        extractor.project()
        g = driver.getWholeGraph()
        val ns = g.nodes().asSequence().toList()
        assertNotNull(ns.filterIsInstance<NamespaceBlock>().find { it.name() == "extractor_tests" })
        ns.filterIsInstance<ODBFile>().find { it.name() == "/extractor_tests/Test1.class".replace("/", sep) }
            .let { assertNotNull(it) }
        ns.filterIsInstance<Method>().find { it.name() == "main" }.let { assertNotNull(it) }
        ns.filterIsInstance<Local>().find { it.name() == "a" }
            .let { assertNotNull(it); assertEquals("byte", it!!.typeFullName()) }
        ns.filterIsInstance<Local>().find { it.name() == "b" }
            .let { assertNotNull(it); assertEquals("byte", it!!.typeFullName()) }
        ns.filterIsInstance<Local>().find { it.name() == "c" }
            .let { assertNotNull(it); assertEquals("int", it!!.typeFullName()) }
        ns.filterIsInstance<Call>().find { it.name() == Operators.plus }.let { assertNotNull(it) }
    }

    @Test
    fun validClassFileTest() {
        extractor.load(validClassFile)
        extractor.project()
        g = driver.getWholeGraph()
        val ns = g.nodes().asSequence().toList()
        assertNotNull(
            ns.filterIsInstance<NamespaceBlock>().find { it.name() == "extractor_tests" })
        ns.filterIsInstance<ODBFile>().find { it.name() == "/extractor_tests/Test2.class".replace("/", sep) }
            .let { assertNotNull(it) }
        ns.filterIsInstance<Method>().find { it.name() == "main" }.let { assertNotNull(it) }
        ns.filterIsInstance<Local>().find { it.name() == "l1" }
            .let { assertNotNull(it); assertEquals("byte", it!!.typeFullName()) }
        ns.filterIsInstance<Local>().find { it.name() == "l2" }
            .let { assertNotNull(it); assertEquals("byte", it!!.typeFullName()) }
        ns.filterIsInstance<Local>().find { it.name() == "l3" }
            .let { assertNotNull(it); assertEquals("int", it!!.typeFullName()) }
        ns.filterIsInstance<Call>().find { it.name() == Operators.plus }.let { assertNotNull(it) }
    }

    @Test
    fun validDirectoryTest() {
        extractor.load(validDirectory)
        extractor.project()
        g = driver.getProgramStructure()
        val ns = g.nodes().asSequence().toList()
        ns.filterIsInstance<ODBFile>().let { fileList ->
            assertNotNull(fileList.firstOrNull { it.name() == "/extractor_tests/dir_test/Dir1.class".replace("/", sep) })
            assertNotNull(fileList.firstOrNull { it.name() == "/extractor_tests/dir_test/pack/Dir2.class".replace("/", sep) })
        }
        assertNotNull(
            ns.filterIsInstance<NamespaceBlock>().firstOrNull { it.name() == "extractor_tests.dir_test.pack" })
    }

    @Test
    fun validJarTest() {
        extractor.load(validJarFile)
        extractor.project()
        g = driver.getWholeGraph()
        g = driver.getProgramStructure()
        val ns = g.nodes().asSequence().toList()
        ns.filterIsInstance<ODBFile>().let { fileList ->
            assertNotNull(fileList.firstOrNull { it.name() == "/intraprocedural/basic/Basic6.class".replace("/", sep) })
            assertNotNull(fileList.firstOrNull { it.name() == "/intraprocedural/basic/basic6/Basic6.class".replace("/", sep) })
        }
        ns.filterIsInstance<NamespaceBlock>().let { nsList ->
            assertNotNull(nsList.firstOrNull { it.name() == "intraprocedural.basic" })
            assertNotNull(nsList.firstOrNull { it.name() == "intraprocedural.basic.basic6" })
        }
    }

    @Test
    fun loadFileThatDoesNotExistTest() {
        assertThrows(NullPointerException::class.java) { extractor.load(File("dne.class")) }
    }
}