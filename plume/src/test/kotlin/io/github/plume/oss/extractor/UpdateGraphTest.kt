package io.github.plume.oss.extractor

import io.github.plume.oss.Extractor
import io.github.plume.oss.drivers.DriverFactory
import io.github.plume.oss.drivers.GraphDatabase
import io.github.plume.oss.drivers.TinkerGraphDriver
import io.github.plume.oss.store.LocalCache
import io.shiftleft.codepropertygraph.generated.EdgeTypes.AST
import io.shiftleft.codepropertygraph.generated.ModifierTypes.*
import io.shiftleft.codepropertygraph.generated.nodes.*
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import overflowdb.Graph
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import io.shiftleft.codepropertygraph.generated.nodes.File as ODBFile

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
        private lateinit var testFile1FieldUpdateValue: File
        private lateinit var testFile1FieldUpdateModifier: File
        private lateinit var testFile1FieldUpdateType: File
        private lateinit var testFile2ClassUpdateModifier: File
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
            testFile1FieldUpdateValue = getTestResource("${TEST_PATH}UpdateTest1_FieldUpdateValue.txt")
            testFile1FieldUpdateModifier = getTestResource("${TEST_PATH}UpdateTest1_FieldUpdateModifier.txt")
            testFile1FieldUpdateType = getTestResource("${TEST_PATH}UpdateTest1_FieldUpdateType.txt")
            testFile2ClassUpdateModifier = getTestResource("${TEST_PATH}UpdateTest2_ClassUpdateModifier.txt")
            extractor = Extractor(driver)
        }

        @AfterAll
        @JvmStatic
        fun tearDownAll() {
            driver.close()
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
        driver.clearGraph()
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
    fun testFieldUpdateValue() {
        val file1Update = rewriteFileContents(testFile1, testFile1FieldUpdateValue)
        listOf(file1Update, testFile2).forEach { extractor.load(it) }
        extractor.project()
        driver.getWholeGraph().use { g2 ->
            val membersG1 = g1.nodes().asSequence().filterIsInstance<Member>().toList()
            val membersG2 = g2.nodes().asSequence().filterIsInstance<Member>().toList()
            assertTrue(membersG1.any { it.name() == "i" && it.typeFullName() == "int" })
            assertTrue(membersG2.any { it.name() == "i" && it.typeFullName() == "int" })
            assertTrue(g1.nodeCount() == g2.nodeCount())
            assertTrue(g1.edgeCount() == g2.edgeCount())
        }
    }

    @Test
    fun testFieldUpdateModifier() {
        val file1Update = rewriteFileContents(testFile1, testFile1FieldUpdateModifier)
        listOf(file1Update, testFile2).forEach { extractor.load(it) }
        extractor.project()
        driver.getWholeGraph().use { g2 ->
            val membersG1 = g1.nodes().asSequence().filterIsInstance<Member>().toList()
            val membersG2 = g2.nodes().asSequence().filterIsInstance<Member>().toList()
            val modifiersG1 = mutableListOf<Modifier>()
            val modifiersG2 = mutableListOf<Modifier>()
            membersG1.firstOrNull { it.name() == "i" && it.typeFullName() == "int" }?.let { m1 ->
                g1.node(m1.id()).out(AST).asSequence().filterIsInstance<Modifier>().toCollection(modifiersG1)
            }
            membersG2.firstOrNull { it.name() == "i" && it.typeFullName() == "int" }?.let { m2 ->
                g2.node(m2.id()).out(AST).asSequence().filterIsInstance<Modifier>().toCollection(modifiersG2)
            }
            assertTrue(modifiersG1.map { it.modifierType() }.contains(PRIVATE))
            assertTrue(
                modifiersG2.map { it.modifierType() }.contains(PUBLIC)
                        && modifiersG2.map { it.modifierType() }.contains(VIRTUAL)
            )
            assertEquals(g1.nodeCount(), g2.nodeCount() - 1)
            assertEquals(g1.edgeCount(), g2.edgeCount() - 1)
        }
    }

    @Test
    fun testFieldUpdateType() {
        val file1Update = rewriteFileContents(testFile1, testFile1FieldUpdateType)
        listOf(file1Update, testFile2).forEach { extractor.load(it) }
        extractor.project()
        driver.getWholeGraph().use { g2 ->
            val membersG1 = g1.nodes().asSequence().filterIsInstance<Member>().toList()
            val membersG2 = g2.nodes().asSequence().filterIsInstance<Member>().toList()
            assertTrue(membersG1.any { it.name() == "i" && it.typeFullName() == "int" })
            assertTrue(membersG2.any { it.name() == "i" && it.typeFullName() == "double" })
            assertEquals(g1.nodeCount(), g2.nodeCount())
            assertEquals(g1.edgeCount(), g2.edgeCount() + 2)
        }
    }

    @Test
    fun testClassUpdateModifier() {
        val file2Update = rewriteFileContents(testFile2, testFile2ClassUpdateModifier)
        listOf(testFile1, file2Update).forEach { extractor.load(it) }
        extractor.project()
        driver.getWholeGraph().use { g2 ->
            val typeDeclG1 = g1.nodes().asSequence().filterIsInstance<TypeDecl>().toList()
            val typeDeclG2 = g2.nodes().asSequence().filterIsInstance<TypeDecl>().toList()
            val modifiersG1 = mutableListOf<Modifier>()
            val modifiersG2 = mutableListOf<Modifier>()
            typeDeclG1.firstOrNull { it.name() == "UpdateTest2" }?.let { td1 ->
                g1.node(td1.id()).out(AST).asSequence().filterIsInstance<Modifier>().toCollection(modifiersG1)
            }
            typeDeclG2.firstOrNull { it.name() == "UpdateTest2" }?.let { td2 ->
                g2.node(td2.id()).out(AST).asSequence().filterIsInstance<Modifier>().toCollection(modifiersG2)
            }
            assertTrue(
                modifiersG1.map { it.modifierType() }.contains(PUBLIC)
                        && modifiersG1.map { it.modifierType() }.contains(VIRTUAL)
            )
            assertTrue(modifiersG2.map { it.modifierType() }.contains(VIRTUAL))
            assertEquals(g1.nodeCount(), g2.nodeCount() + 2)
            assertEquals(g1.edgeCount(), g2.edgeCount() + 2)
        }
    }

    @Test
    fun testClassRemoval() {
        listOf(testFile2).forEach { extractor.load(it) }
        extractor.project()
        driver.getWholeGraph().use { g2 ->
            // Only meta data may be the only node with no edges
            assertEquals(1, g2.nodes().asSequence().filter { it.both().asSequence().toList().isEmpty() }.toList().size)
            val filesG1 = g1.nodes().asSequence().filterIsInstance<ODBFile>().toList()
            val filesG2 = g2.nodes().asSequence().filterIsInstance<ODBFile>().toList()
            val npbG1 = g1.nodes().asSequence().filterIsInstance<NamespaceBlock>().toList()
            val npbG2 = g2.nodes().asSequence().filterIsInstance<NamespaceBlock>().toList()
            val tdG1 = g1.nodes().asSequence().filterIsInstance<TypeDecl>().toList()
            val tdG2 = g2.nodes().asSequence().filterIsInstance<TypeDecl>().toList()
            val tG1 = g1.nodes().asSequence().filterIsInstance<Type>().toList()
            val tG2 = g2.nodes().asSequence().filterIsInstance<Type>().toList()

            fun cleanPathSeps(s: String) = s.replace("/", File.separator)

            // Check files
            assertTrue(filesG1.any { it.name() == cleanPathSeps("/extractor_tests/update_test/UpdateTest1.class") })
            assertTrue(filesG1.any { it.name() == cleanPathSeps("/extractor_tests/update_test/UpdateTest2.class") })
            assertFalse(filesG2.any { it.name() == cleanPathSeps("/extractor_tests/update_test/UpdateTest1.class") })
            assertTrue(filesG2.any { it.name() == cleanPathSeps("/extractor_tests/update_test/UpdateTest2.class") })
            // Check namespace blocks
            assertTrue(npbG1.any { it.fullName() == cleanPathSeps("/extractor_tests/update_test/UpdateTest1.class:extractor_tests.update_test") })
            assertTrue(npbG1.any { it.fullName() == cleanPathSeps("/extractor_tests/update_test/UpdateTest2.class:extractor_tests.update_test") })
            assertFalse(npbG2.any { it.fullName() == cleanPathSeps("/extractor_tests/update_test/UpdateTest1.class:extractor_tests.update_test") })
            assertTrue(npbG2.any { it.fullName() == cleanPathSeps("/extractor_tests/update_test/UpdateTest2.class:extractor_tests.update_test") })
            // Check types
            assertTrue(tG1.any { it.fullName() == "extractor_tests.update_test.UpdateTest1" })
            assertTrue(tG1.any { it.fullName() == "extractor_tests.update_test.UpdateTest2" })
            assertFalse(tG2.any { it.fullName() == "extractor_tests.update_test.UpdateTest1" })
            assertTrue(tG2.any { it.fullName() == "extractor_tests.update_test.UpdateTest2" })
            // Check type decl
            assertTrue(tdG1.any { it.fullName() == "extractor_tests.update_test.UpdateTest2" })
            assertTrue(tdG1.any { it.fullName() == "extractor_tests.update_test.UpdateTest2" })
            assertFalse(tdG2.any { it.fullName() == "extractor_tests.update_test.UpdateTest1" })
            assertTrue(tdG2.any { it.fullName() == "extractor_tests.update_test.UpdateTest2" })

            assertTrue(g1.nodeCount() > g2.nodeCount())
            assertTrue(g1.edgeCount() > g2.edgeCount())
        }
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