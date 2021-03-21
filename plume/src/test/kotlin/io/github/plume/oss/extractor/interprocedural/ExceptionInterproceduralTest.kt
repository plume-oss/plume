package io.github.plume.oss.extractor.interprocedural

import io.github.plume.oss.Extractor
import io.github.plume.oss.drivers.DriverFactory
import io.github.plume.oss.drivers.GraphDatabase
import io.github.plume.oss.drivers.TinkerGraphDriver
import io.github.plume.oss.store.LocalCache
import io.shiftleft.codepropertygraph.generated.EdgeTypes.AST
import io.shiftleft.codepropertygraph.generated.EdgeTypes.CFG
import io.shiftleft.codepropertygraph.generated.nodes.Block
import io.shiftleft.codepropertygraph.generated.nodes.Call
import io.shiftleft.codepropertygraph.generated.nodes.Literal
import io.shiftleft.codepropertygraph.generated.nodes.Local
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInfo
import overflowdb.Graph
import java.io.File
import java.io.IOException

class ExceptionInterproceduralTest {
    companion object {
        private val driver = DriverFactory(GraphDatabase.TINKER_GRAPH) as TinkerGraphDriver
        private lateinit var g: Graph
        private var PATH: File
        private val TEST_PATH = "interprocedural/exception"

        init {
            val testFileUrl = ExceptionInterproceduralTest::class.java.classLoader.getResource(TEST_PATH)
                ?: throw NullPointerException("Unable to obtain test resource")
            PATH = File(testFileUrl.file)
        }
    }

    @BeforeEach
    @Throws(IOException::class)
    fun setUp(testInfo: TestInfo) {
        val extractor = Extractor(driver)
        // Select test resource based on integer in method name
        val currentTestNumber = testInfo.displayName.replace("[^0-9]".toRegex(), "")
        val resourceDir = "${PATH.absolutePath}/Exception$currentTestNumber.java"
        // Load test resource and project + export graph
        val f = File(resourceDir)
        extractor.load(f).project()
        g = driver.getMethod(
            "intraprocedural.exception.Exception$currentTestNumber.main:void(java.lang.String[])",
            includeBody = true
        )
    }

    @AfterEach
    fun tearDown() {
        LocalCache.clear()
        driver.close()
        g.close()
    }

    @Test
    fun exception1Test() {
        val ns =  g.nodes().asSequence().toList()
        val localV = ns.filterIsInstance<Local>()
        val mtdV = ns.filterIsInstance<Block>().firstOrNull()?.apply { assertNotNull(this) }
        assertNotNull(localV.firstOrNull { it.name() == "e" && it.typeFullName() == "java.lang.Exception" })
        assertNotNull(localV.firstOrNull { it.name() == "a" && it.typeFullName() == "int" })
        assertNotNull(localV.firstOrNull { it.name() == "\$stack4" && it.typeFullName() == "java.lang.Exception" })
        assertNotNull(localV.firstOrNull { it.name() == "e#3" && it.typeFullName() == "int" })
        assertEquals(2, g.V(mtdV!!.id()).next().out(CFG).asSequence().toList().size)

        val parseIntCall = ns.filterIsInstance<Call>().filter { it.name() == "parseInt" }
            .apply { assertEquals(1, this.toList().size) }.firstOrNull().apply { assertNotNull(this) }
        parseIntCall!!
        g.V(parseIntCall.id()).next().out(AST).asSequence().filterIsInstance<Literal>().firstOrNull()
            ?.let { assertEquals("\"2\"", it.code()) }
    }

    @Test
    fun exception2Test() {
        val ns =  g.nodes().asSequence().toList()
        val localV = ns.filterIsInstance<Local>()

        val mtdV = ns.filterIsInstance<Block>().firstOrNull()?.apply { assertNotNull(this) }
        assertNotNull(localV.firstOrNull {
            it.name() == "e" && it.typeFullName() == "java.lang.Exception"
        })
        assertNotNull(localV.firstOrNull { it.name() == "a" && it.typeFullName() == "int" })
        assertNotNull(localV.firstOrNull {
            it.name() == "\$stack5" && it.typeFullName() == "java.lang.Exception"
        })
        assertNotNull(localV.firstOrNull { it.name() == "e#3" && it.typeFullName() == "int" })
        assertNotNull(localV.firstOrNull { it.name() == "b" && it.typeFullName() == "byte" })
        assertEquals(2, g.V(mtdV!!.id()).next().out(CFG).asSequence().toList().size)

        val parseIntCall = ns.filterIsInstance<Call>().filter { it.name() == "parseInt" }
            .apply { assertEquals(1, this.toList().size) }.firstOrNull().apply { assertNotNull(this) }
        parseIntCall!!
        g.V(parseIntCall.id()).next().out(AST).asSequence().filterIsInstance<Literal>().firstOrNull()
            ?.let { assertEquals("\"2\"", it.code()) }
    }

}