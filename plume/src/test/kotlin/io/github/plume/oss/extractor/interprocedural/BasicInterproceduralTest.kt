package io.github.plume.oss.extractor.interprocedural

import io.github.plume.oss.Extractor
import io.github.plume.oss.drivers.DriverFactory
import io.github.plume.oss.drivers.GraphDatabase
import io.github.plume.oss.drivers.TinkerGraphDriver
import io.shiftleft.codepropertygraph.generated.EdgeTypes.*
import io.shiftleft.codepropertygraph.generated.nodes.*
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInfo
import overflowdb.Graph
import java.io.File
import java.io.IOException

class BasicInterproceduralTest {
    companion object {
        private var driver = DriverFactory(GraphDatabase.TINKER_GRAPH) as TinkerGraphDriver
        private lateinit var g: Graph
        private var PATH: File
        private val TEST_PATH = "interprocedural${File.separator}basic"

        init {
            val testFileUrl = BasicInterproceduralTest::class.java.classLoader.getResource(TEST_PATH)
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
        val resourceDir = "${PATH.absolutePath}${File.separator}Basic$currentTestNumber.java"
        // Load test resource and project + export graph
        val f = File(resourceDir)
        extractor.load(f)
        extractor.project()
        g = driver.getWholeGraph()
    }

    @AfterEach
    fun tearDown() {
        driver.clearGraph()
    }

    @Test
    fun basicCall1Test() {
        val ns = g.nodes().asSequence()
        val mainMethod = ns.filterIsInstance<Method>()
            .find { it.fullName() == "interprocedural.basic.Basic1.main" }
            .apply { assertNotNull(this) }
        val fMethod = ns.filterIsInstance<Method>()
            .find { it.fullName() == "interprocedural.basic.Basic1.f" }
            .apply { assertNotNull(this) }
        val gMethod = ns.filterIsInstance<Method>()
            .find { it.fullName() == "interprocedural.basic.Basic1.g" }
            .apply { assertNotNull(this) }
        val fCall =
            ns.filterIsInstance<Call>().find { it.name() == "f" }.apply { assertNotNull(this) }
        val gCall =
            ns.filterIsInstance<Call>().find { it.name() == "g" }.apply { assertNotNull(this) }
        mainMethod!!; fMethod!!; gMethod!!; fCall!!; gCall!!
        assertTrue(g.V(fCall.id()).next().out(REF).asSequence().any { it.id() == fMethod.id() })
        assertTrue(g.V(gCall.id()).next().out(REF).asSequence().any { it.id() == gMethod.id() })
    }

    @Test
    fun basicCall2Test() {
        val ns = g.nodes().asSequence()
        val mainMethod = ns.filterIsInstance<Method>()
            .find { it.fullName() == "interprocedural.basic.Basic2.main" }
            .apply { assertNotNull(this) }
        val fMethod = ns.filterIsInstance<Method>()
            .find { it.fullName() == "interprocedural.basic.Basic2.f" }
            .apply { assertNotNull(this) }
        val fCall = ns.filterIsInstance<Call>().find { it.name() == "f" }.apply { assertNotNull(this) }
        mainMethod!!; fMethod!!; fCall!!
        val assignVert = g.V(fCall.id()).next().`in`(ARGUMENT).next()
        assertNotNull(assignVert)
        assertTrue(g.V(assignVert.id()).next().out(REF).asSequence().any { it.id() == fMethod.id() })
        g.V(fCall.id()).next().out(ARGUMENT).asSequence().filterIsInstance<Literal>().firstOrNull()
            ?.let { assertEquals("5", it.code()) }
    }

    @Test
    fun basicCall3Test() {
        val ns = g.nodes().asSequence()
        ns.filterIsInstance<TypeDecl>().find { it.fullName() == "java.lang.Object" }
            .apply { assertNotNull(this) }
        ns.filterIsInstance<Method>().find { it.fullName() == "java.lang.Object.<init>" }
            .apply { assertNotNull(this) }
        val mainMethod = ns.filterIsInstance<Method>()
            .find { it.fullName() == "interprocedural.basic.Basic3.main" }
            .apply { assertNotNull(this) }
        val initMethod = ns.filterIsInstance<Call>()
            .find { it.methodFullName() == "java.lang.Object: void <init>()" }
            .apply { assertNotNull(this) }
        val fMethod = ns.filterIsInstance<Method>()
            .find { it.fullName() == "interprocedural.basic.Basic3.f" }
            .apply { assertNotNull(this) }
        val fCall = ns.filterIsInstance<Call>().find { it.name() == "f" }.apply { assertNotNull(this) }
        mainMethod!!; initMethod!!; fCall!!; fMethod!!
        g.V(initMethod.id()).next().out(REF).asSequence().filterIsInstance<Method>().firstOrNull()
            ?.apply { assertNotNull(this); assertEquals(initMethod.name(), this.name()) }
        assertTrue(g.V(fCall.id()).next().out(REF).asSequence().any { it.id() == fMethod.id() })
    }

    @Test
    fun basicCall4Test() {
        val ns = g.nodes().asSequence()
        ns.filterIsInstance<TypeRef>()
            .find { it.typeFullName() == "interprocedural.basic.Basic4" }
            ?.let { trv ->
                assertNotNull(trv)
                val assignVert = g.V(trv.id()).next().`in`(AST).asSequence().first().apply { assertNotNull(this) }
                assignVert!!
                g.V(assignVert.id()).next().out(AST).asSequence().filterIsInstance<Identifier>()
                    .find { it.name() == "\$stack1" }.apply { assertNotNull(this) }
            }
        ns.filterIsInstance<TypeDecl>().find { it.fullName() == "java.lang.Object" }.apply { assertNotNull(this) }
        ns.filterIsInstance<Method>().find { it.fullName() == "java.lang.Object.<init>" }.apply { assertNotNull(this) }
        val mainMethod = ns.filterIsInstance<Method>()
            .find { it.fullName() == "interprocedural.basic.Basic4.main" }
            .apply { assertNotNull(this) }
        val initMethod = ns.filterIsInstance<Call>()
            .find { it.methodFullName() == "java.lang.Object: void <init>()" }
            .apply { assertNotNull(this) }
        val fMethod = ns.filterIsInstance<Method>()
            .find { it.fullName() == "interprocedural.basic.Basic4.f" }
            .apply { assertNotNull(this) }
        val fCall =
            ns.filterIsInstance<Call>().find { it.name() == "f" }.apply { assertNotNull(this) }
        mainMethod!!; initMethod!!; fCall!!; fMethod!!
        g.V(initMethod.id()).next().out(REF).asSequence().filterIsInstance<Method>().firstOrNull()
            ?.apply { assertNotNull(this); assertEquals(initMethod.name(), this.name()) }
        assertNotNull(g.V(fCall.id()).next().out(REF).asSequence().firstOrNull { it.id() == fMethod.id() })
        g.V(fCall.id()).next().out(ARGUMENT).asSequence().filterIsInstance<Literal>().let { lv ->
            assertTrue(lv.any { it.code() == "5" })
            assertTrue(lv.any { it.code() == "6" })
        }
        g.V(fMethod.id()).next().out(AST).asSequence().filterIsInstance<MethodParameterIn>().let { mpv ->
            assertTrue(mpv.any { it.name() == "i" })
            assertTrue(mpv.any { it.name() == "j" })
        }
    }

    @Test
    fun basicCall5Test() {
        val ns = g.nodes().asSequence()
        ns.filterIsInstance<TypeRef>()
            .find { it.typeFullName() == "interprocedural.basic.Basic5" }
            ?.let { trv ->
                assertNotNull(trv)
                val assignVert = g.V(trv.id()).next().out(AST).asSequence().first().apply { assertNotNull(this) }
                assignVert!!
                g.V(assignVert.id()).next().out(AST).asSequence().filterIsInstance<Identifier>()
                    .find { it.name() == "\$stack1" }.apply { assertNotNull(this) }
            }
        ns.filterIsInstance<TypeDecl>().find { it.fullName() == "java.lang.Object" }.apply { assertNotNull(this) }
        ns.filterIsInstance<Method>().find { it.fullName() == "java.lang.Object.<init>" }.apply { assertNotNull(this) }
        val mainMethod = ns.filterIsInstance<Method>()
            .find { it.fullName() == "interprocedural.basic.Basic5.main" }
            .apply { assertNotNull(this) }
        val initMethod = ns.filterIsInstance<Call>()
            .find { it.methodFullName() == "java.lang.Object: void <init>()" }
            .apply { assertNotNull(this) }
        val fMethod = ns.filterIsInstance<Method>()
            .find { it.fullName() == "interprocedural.basic.Basic5.f" }
            .apply { assertNotNull(this) }
        val fCall = ns.filterIsInstance<Call>().find { it.name() == "f" }.apply { assertNotNull(this) }
        mainMethod!!; initMethod!!; fCall!!; fMethod!!
        g.V(initMethod.id()).next().out(REF).asSequence().filterIsInstance<Method>().firstOrNull()
            ?.apply { assertNotNull(this); assertEquals(initMethod.name(), this.name()) }
        assertTrue(g.V(fCall.id()).next().out(REF).asSequence().any { it.id() == fMethod.id() })
        g.V(fCall.id()).next().out(ARGUMENT).asSequence().filterIsInstance<Literal>().let { lv ->
            assertTrue(lv.any { it.code() == "\"Test\"" })
            assertTrue(lv.any { it.code() == "\"Case\"" })
        }
        g.V(fMethod.id()).next().out(AST).asSequence().filterIsInstance<MethodParameterIn>().let { mpv ->
            assertTrue(mpv.any { it.name() == "prefix" })
            assertTrue(mpv.any { it.name() == "suffix" })
        }
    }
}