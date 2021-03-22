package io.github.plume.oss.extractor.intraprocedural

import io.github.plume.oss.Extractor
import io.github.plume.oss.drivers.DriverFactory
import io.github.plume.oss.drivers.GraphDatabase
import io.github.plume.oss.drivers.TinkerGraphDriver
import io.github.plume.oss.store.LocalCache
import io.shiftleft.codepropertygraph.generated.Operators
import io.shiftleft.codepropertygraph.generated.nodes.*
import io.shiftleft.codepropertygraph.generated.nodes.File as ODBFile
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInfo
import overflowdb.Graph
import java.io.File
import java.io.IOException

class BasicIntraproceduralTest {
    private lateinit var currentTestNumber: String

    companion object {
        private val driver = DriverFactory(GraphDatabase.TINKER_GRAPH) as TinkerGraphDriver
        private lateinit var g: Graph
        private var PATH: File
        private val TEST_PATH = "intraprocedural/basic"
        private val sep = File.separator

        init {
            val testFileUrl = BasicIntraproceduralTest::class.java.classLoader.getResource(TEST_PATH)
                ?: throw NullPointerException("Unable to obtain test resource")
            PATH = File(testFileUrl.file)
        }
    }

    @BeforeEach
    @Throws(IOException::class)
    fun setUp(testInfo: TestInfo) {
        val extractor = Extractor(driver)
        // Select test resource based on integer in method name
        currentTestNumber = testInfo
            .displayName
            .replace("[^0-9]".toRegex(), "")
        val resourceDir = "${PATH.absolutePath}${File.separator}Basic$currentTestNumber.java"
        // Load test resource and project + export graph
        val f = File(resourceDir)
        extractor.load(f).project()
        g = driver.getWholeGraph()
    }

    @AfterEach
    fun tearDown() {
        LocalCache.clear()
        driver.close()
        g.close()
    }

    @Test
    fun basic1Test() {
        val ns = g.nodes().asSequence().toList()
        assertNotNull(ns.filterIsInstance<NamespaceBlock>().find { it.name() == "intraprocedural.basic" })
        ns.filterIsInstance<ODBFile>()
            .find { it.name() == "/intraprocedural/basic/Basic$currentTestNumber.class".replace("/", sep) }
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
    fun basic2Test() {
        val ns = g.nodes().asSequence().toList()
        assertNotNull(ns.filterIsInstance<NamespaceBlock>().find { it.name() == "intraprocedural.basic" })
        ns.filterIsInstance<ODBFile>()
            .find { it.name() == "/intraprocedural/basic/Basic$currentTestNumber.class".replace("/", sep) }
            .let { assertNotNull(it) }
        ns.filterIsInstance<Method>().find { it.name() == "main" }.let { assertNotNull(it) }
        ns.filterIsInstance<Local>().find { it.name() == "a" }
            .let { assertNotNull(it); assertEquals("byte", it!!.typeFullName()) }
        ns.filterIsInstance<Local>().find { it.name() == "b" }
            .let { assertNotNull(it); assertEquals("double", it!!.typeFullName()) }
        ns.filterIsInstance<Local>().find { it.name() == "c" }
            .let { assertNotNull(it); assertEquals("double", it!!.typeFullName()) }
        ns.filterIsInstance<Call>().find { it.name() == Operators.plus }.let { assertNotNull(it) }
    }

    @Test
    fun basic3Test() {
        val ns = g.nodes().asSequence().toList()
        assertNotNull(ns.filterIsInstance<NamespaceBlock>().find { it.name() == "intraprocedural.basic" })
        ns.filterIsInstance<ODBFile>()
            .find { it.name() == "/intraprocedural/basic/Basic$currentTestNumber.class".replace("/", sep) }
            .let { assertNotNull(it) }
        ns.filterIsInstance<Method>().find { it.name() == "main" }.let { assertNotNull(it) }
        ns.filterIsInstance<Local>().find { it.name() == "a" }
            .let { assertNotNull(it); assertEquals("long", it!!.typeFullName()) }
        ns.filterIsInstance<Local>().find { it.name() == "b" }
            .let { assertNotNull(it); assertEquals("short", it!!.typeFullName()) }
        ns.filterIsInstance<Local>().find { it.name() == "c" }
            .let { assertNotNull(it); assertEquals("long", it!!.typeFullName()) }
        ns.filterIsInstance<Call>().find { it.name() == Operators.plus }.let { assertNotNull(it) }
    }

    @Test
    fun basic4Test() {
        val ns = g.nodes().asSequence().toList()
        assertNotNull(ns.filterIsInstance<NamespaceBlock>().find { it.name() == "intraprocedural.basic" })
        ns.filterIsInstance<ODBFile>()
            .find { it.name() == "/intraprocedural/basic/Basic$currentTestNumber.class".replace("/", sep) }
            .let { assertNotNull(it) }
        ns.filterIsInstance<Method>().find { it.name() == "main" }.let { assertNotNull(it) }
        ns.filterIsInstance<Method>().find { it.name() == "Sally" }.let { assertNotNull(it) }
        ns.filterIsInstance<Method>().find { it.name() == "John" }.let { assertNotNull(it) }
        ns.filterIsInstance<Method>().find { it.name() == "Dick" }.let { assertNotNull(it) }
        ns.filterIsInstance<Method>().find { it.name() == "Nigel" }.let { assertNotNull(it) }
        ns.filterIsInstance<MethodReturn>().let { mrv ->
            assertNotNull(mrv.find { it.typeFullName() == "int" })
            assertNotNull(mrv.find { it.typeFullName() == "int[]" })
            assertNotNull(mrv.find { it.typeFullName() == "double" })
            assertNotNull(mrv.find { it.typeFullName() == "boolean" })
        }
    }

    @Test
    fun basic5Test() {
        val extractor = Extractor(driver)
        val resourceDir = "${PATH.absolutePath}${File.separator}basic5${File.separator}Basic$currentTestNumber.java"
        // Load test resource and project + export graph
        val f = File(resourceDir)
        extractor.load(f)
        extractor.project()
        g = driver.getWholeGraph()
        val ns = g.nodes().asSequence().toList()
        ns.filterIsInstance<NamespaceBlock>().let { nbv ->
            assertNotNull(nbv.find { it.name() == "intraprocedural.basic.basic5" })
            assertEquals(5, nbv.toList().size)
        }
        ns.filterIsInstance<TypeDecl>().filter { !it.isExternal() }.let { mrv ->
            assertNotNull(mrv.find { it.fullName() == "intraprocedural.basic.Basic5" })
            assertNotNull(mrv.find { it.fullName() == "intraprocedural.basic.basic5.Basic5" })
            assertNotNull(mrv.find { it.fullName() == "int" })
            assertNotNull(mrv.find { it.fullName() == "byte" })
            assertNotNull(mrv.find { it.fullName() == "java.lang.String[]" })
            assertEquals(6, mrv.toList().size)
        }
        ns.filterIsInstance<Method>().filter { !it.isExternal() }.let { mv ->
            assertNotNull(mv.find { it.fullName() == "intraprocedural.basic.Basic5.main:void(java.lang.String[])" })
            assertNotNull(mv.find { it.fullName() == "intraprocedural.basic.basic5.Basic5.main:void(java.lang.String[])" })
            assertNotNull(mv.find { it.fullName() == "intraprocedural.basic.Basic5.<init>:void()" })
            assertNotNull(mv.find { it.fullName() == "intraprocedural.basic.basic5.Basic5.<init>:void()" })
            assertEquals(4, mv.toList().size)
        }
    }
}