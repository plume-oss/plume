package io.github.plume.oss.extractor

import io.github.plume.oss.Extractor
import io.github.plume.oss.drivers.DriverFactory
import io.github.plume.oss.drivers.GraphDatabase
import io.github.plume.oss.drivers.TinkerGraphDriver
import io.github.plume.oss.graphio.GraphMLWriter
import io.github.plume.oss.store.LocalCache
import io.shiftleft.codepropertygraph.generated.nodes.Literal
import io.shiftleft.codepropertygraph.generated.nodes.Member
import io.shiftleft.codepropertygraph.generated.nodes.Method
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import overflowdb.Graph
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.FileWriter

class UpdateGraphTest {
    companion object {
        private val driver = DriverFactory(GraphDatabase.TINKER_GRAPH) as TinkerGraphDriver
        private const val TEST_PATH = "extractor_tests/update_test/"
        private lateinit var extractor: Extractor
        private lateinit var testFile1: File
        private lateinit var testFile2: File
        private lateinit var testFile1Original: File
        private lateinit var testFile2Original: File
        private lateinit var testFile1MethodAdd: File
        private lateinit var testFile1MethodRemove: File
        private lateinit var testFile2MethodUpdate: File
        private lateinit var testFile1FieldAdd: File
        private lateinit var testFile1FieldRemove: File
        private lateinit var testFile1FieldUpdate: File
        private lateinit var g1: Graph

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
            testFile1Original = getTestResource("${TEST_PATH}UpdateTest1_Original.txt")
            testFile2Original = getTestResource("${TEST_PATH}UpdateTest2_Original.txt")
            testFile1MethodAdd = getTestResource("${TEST_PATH}UpdateTest1_MethodAdd.txt")
            testFile1MethodRemove = getTestResource("${TEST_PATH}UpdateTest1_MethodRemove.txt")
            testFile2MethodUpdate = getTestResource("${TEST_PATH}UpdateTest2_MethodUpdate.txt")
            testFile1FieldAdd = getTestResource("${TEST_PATH}UpdateTest1_FieldAdd.txt")
            testFile1FieldRemove = getTestResource("${TEST_PATH}UpdateTest1_FieldRemove.txt")
            testFile1FieldUpdate = getTestResource("${TEST_PATH}UpdateTest1_FieldUpdate.txt")
            extractor = Extractor(driver)
        }
    }

    @BeforeEach
    fun setUp() {
        // Make sure original files are intact
        rewriteFileContents(testFile1, testFile1Original)
        rewriteFileContents(testFile2, testFile2Original)
        // Initial projection
        listOf(testFile1, testFile2).forEach { extractor.load(it) }
        extractor.project()
        g1 = driver.getWholeGraph()
    }

    @AfterEach
    fun tearDown() {
        LocalCache.clear()
        driver.close()
        g1.close()
    }

    @Test
    fun testMethodAdd() {
        val file1Update = rewriteFileContents(testFile1, testFile1MethodAdd)
        listOf(file1Update, testFile2).forEach { extractor.load(it) }
        extractor.project()
        driver.getWholeGraph().use { g2 ->
            val methodsG1 = g1.nodes().asSequence().filterIsInstance<Method>().toList()
            val methodsG2 = g2.nodes().asSequence().filterIsInstance<Method>().toList()
            assertFalse(methodsG1.any { it.fullName() == "extractor_tests.update_test.UpdateTest1.bar:int(int)" })
            assertTrue(methodsG2.any { it.fullName() == "extractor_tests.update_test.UpdateTest1.bar:int(int)" })
            assertTrue(g1.nodeCount() < g2.nodeCount())
            assertTrue(g1.edgeCount() < g2.edgeCount())
        }
    }

    @Test
    fun testMethodRemove() {
        val file1Update = rewriteFileContents(testFile1, testFile1MethodRemove)
        listOf(file1Update, testFile2).forEach { extractor.load(it) }
        extractor.project()
        driver.getWholeGraph().use { g2 ->
            val methodsG1 = g1.nodes().asSequence().filterIsInstance<Method>().toList()
            val methodsG2 = g2.nodes().asSequence().filterIsInstance<Method>().toList()
            assertTrue(methodsG1.any { it.fullName() == "extractor_tests.update_test.UpdateTest1.foo:int(int)" })
            assertFalse(methodsG2.any { it.fullName() == "extractor_tests.update_test.UpdateTest1.foo:int(int)" })
            assertTrue(g1.nodeCount() > g2.nodeCount())
            assertTrue(g1.edgeCount() > g2.edgeCount())
        }
    }

    @Test
    fun testMethodUpdate() {
        val file2Update = rewriteFileContents(testFile2, testFile2MethodUpdate)
        listOf(testFile1, file2Update).forEach { extractor.load(it) }
        extractor.project()
        driver.getWholeGraph().use { g2 ->
            val literalsG1 = g1.nodes().asSequence().filterIsInstance<Literal>().toList()
            val literalsG2 = g2.nodes().asSequence().filterIsInstance<Literal>().toList()
            assertTrue(literalsG1.any { it.code() == "5" })
            assertTrue(literalsG2.any { it.code() == "9" })
            assertTrue(literalsG2.none { it.code() == "5" })
            assertTrue(literalsG1.none { it.code() == "9" })
            assertFalse(g1 == g2)
            assertEquals(g1.nodeCount(), g2.nodeCount())
            assertEquals(g1.edgeCount(), g2.edgeCount())
        }
    }

    @Test
    fun testFieldAdd() {
        val file1Update = rewriteFileContents(testFile1, testFile1FieldAdd)
        listOf(file1Update, testFile2).forEach { extractor.load(it) }
        extractor.project()
        driver.getWholeGraph().use { g2 ->
            val membersG1 = g1.nodes().asSequence().filterIsInstance<Member>().toList()
            val membersG2 = g2.nodes().asSequence().filterIsInstance<Member>().toList()
            assertTrue(membersG1.any { it.name() == "i" && it.typeFullName() == "int" })
            assertFalse(membersG1.any { it.name() == "j" && it.typeFullName() == "boolean" })
            assertTrue(membersG2.any { it.name() == "i" && it.typeFullName() == "int" })
            assertTrue(membersG2.any { it.name() == "j" && it.typeFullName() == "boolean" })
            assertTrue(g1.nodeCount() < g2.nodeCount())
            assertTrue(g1.edgeCount() < g2.edgeCount())
        }
    }

    @Test
    fun testFieldRemove() {
        val file1Update = rewriteFileContents(testFile1, testFile1FieldRemove)
        listOf(file1Update, testFile2).forEach { extractor.load(it) }
        extractor.project()
        driver.getWholeGraph().use { g2 ->
            val membersG1 = g1.nodes().asSequence().filterIsInstance<Member>().toList()
            val membersG2 = g2.nodes().asSequence().filterIsInstance<Member>().toList()
            assertTrue(membersG1.any { it.name() == "i" && it.typeFullName() == "int" })
            assertFalse(membersG2.any { it.name() == "i" && it.typeFullName() == "int" })
            assertTrue(g1.nodeCount() > g2.nodeCount())
            assertTrue(g1.edgeCount() > g2.edgeCount())
        }
    }

    @Test
    fun testFieldUpdate() {
        val file1Update = rewriteFileContents(testFile1, testFile1FieldUpdate)
        listOf(file1Update, testFile2).forEach { extractor.load(it) }
        extractor.project()
        driver.getWholeGraph().use { g2 ->
            val g1s = g1.edges().asSequence().groupBy { it.label() }.mapValues { it.value.size }
            val g2s = g2.edges().asSequence().groupBy { it.label() }.mapValues { it.value.size }
            println(g1s)
            println(g2s)
            GraphMLWriter.write(g1, FileWriter("/tmp/plume/g1.xml"))
            GraphMLWriter.write(g2, FileWriter("/tmp/plume/g2.xml"))
        }
        TODO("Write test")
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