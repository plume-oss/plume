package io.github.plume.oss.extractor.interprocedural

import io.github.plume.oss.Extractor
import io.github.plume.oss.domain.enums.EdgeLabel
import io.github.plume.oss.domain.models.PlumeGraph
import io.github.plume.oss.drivers.DriverFactory
import io.github.plume.oss.drivers.GraphDatabase
import io.github.plume.oss.drivers.TinkerGraphDriver
import io.shiftleft.codepropertygraph.generated.nodes.*
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInfo
import java.io.File
import java.io.IOException

class BasicInterproceduralTest {
    companion object {
        private var driver = DriverFactory(GraphDatabase.TINKER_GRAPH) as TinkerGraphDriver
        private lateinit var graph: PlumeGraph
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
        graph = driver.getWholeGraph()
    }

    @AfterEach
    fun tearDown() {
        driver.clearGraph()
    }

    @Test
    fun basicCall1Test() {
        val vertices = graph.vertices()
        val mainMethod =
            vertices.filterIsInstance<NewMethodBuilder>()
                .find { it.build().fullName() == "interprocedural.basic.Basic1.main" }
                .apply { assertNotNull(this) }
        val fMethod = vertices.filterIsInstance<NewMethodBuilder>()
            .find { it.build().fullName() == "interprocedural.basic.Basic1.f" }
            .apply { assertNotNull(this) }
        val gMethod = vertices.filterIsInstance<NewMethodBuilder>()
            .find { it.build().fullName() == "interprocedural.basic.Basic1.g" }
            .apply { assertNotNull(this) }
        val fCall =
            vertices.filterIsInstance<NewCallBuilder>().find { it.build().name() == "f" }.apply { assertNotNull(this) }
        val gCall =
            vertices.filterIsInstance<NewCallBuilder>().find { it.build().name() == "g" }.apply { assertNotNull(this) }
        mainMethod!!; fMethod!!; gMethod!!; fCall!!; gCall!!
        graph.edgesOut(fCall)[EdgeLabel.REF]?.contains(fMethod)?.let { assertTrue(it) }
        graph.edgesOut(gCall)[EdgeLabel.REF]?.contains(gMethod)?.let { assertTrue(it) }
    }

    @Test
    fun basicCall2Test() {
        val vertices = graph.vertices()
        val mainMethod =
            vertices.filterIsInstance<NewMethodBuilder>()
                .find { it.build().fullName() == "interprocedural.basic.Basic2.main" }
                .apply { assertNotNull(this) }
        val fMethod = vertices.filterIsInstance<NewMethodBuilder>()
            .find { it.build().fullName() == "interprocedural.basic.Basic2.f" }
            .apply { assertNotNull(this) }
        val fCall =
            vertices.filterIsInstance<NewCallBuilder>().find { it.build().name() == "f" }.apply { assertNotNull(this) }
        mainMethod!!; fMethod!!; fCall!!
        val assignVert = graph.edgesIn(fCall)[EdgeLabel.ARGUMENT]?.firstOrNull()?.apply { assertNotNull(this) }
        assignVert!!
        graph.edgesOut(assignVert)[EdgeLabel.REF]?.contains(fMethod)?.let { assertTrue(it) }
        graph.edgesOut(fCall)[EdgeLabel.ARGUMENT]?.filterIsInstance<NewLiteral>()?.firstOrNull()
            ?.let { assertEquals("5", it.code()) }
    }

    @Test
    fun basicCall3Test() {
        val vertices = graph.vertices()
        vertices.filterIsInstance<NewTypeDeclBuilder>().find { it.build().fullName() == "java.lang.Object" }
            .apply { assertNotNull(this) }
        vertices.filterIsInstance<NewMethodBuilder>().find { it.build().fullName() == "java.lang.Object.<init>" }
            .apply { assertNotNull(this) }
        val mainMethod =
            vertices.filterIsInstance<NewMethodBuilder>()
                .find { it.build().fullName() == "interprocedural.basic.Basic3.main" }
                .apply { assertNotNull(this) }
        val initMethod =
            vertices.filterIsInstance<NewCallBuilder>()
                .find { it.build().methodFullName() == "java.lang.Object: void <init>()" }
                .apply { assertNotNull(this) }
        val fMethod = vertices.filterIsInstance<NewMethodBuilder>()
            .find { it.build().fullName() == "interprocedural.basic.Basic3.f" }
            .apply { assertNotNull(this) }
        val fCall =
            vertices.filterIsInstance<NewCallBuilder>().find { it.build().name() == "f" }.apply { assertNotNull(this) }
        mainMethod!!; initMethod!!; fCall!!; fMethod!!
        graph.edgesOut(initMethod)[EdgeLabel.REF]?.filterIsInstance<NewMethodBuilder>()?.firstOrNull()
            ?.apply { assertNotNull(this); assertEquals(initMethod.build().name(), this.build().name()) }
        graph.edgesOut(fCall)[EdgeLabel.REF]?.contains(fMethod)?.let { assertTrue(it) }
    }

    @Test
    fun basicCall4Test() {
        val vertices = graph.vertices()
        vertices.filterIsInstance<NewTypeRefBuilder>()
            .find { it.build().typeFullName() == "interprocedural.basic.Basic4" }
            ?.let { trv ->
                assertNotNull(trv)
                val assignVert = graph.edgesIn(trv)[EdgeLabel.AST]?.first().apply { assertNotNull(this) }; assignVert!!
                graph.edgesOut(assignVert)[EdgeLabel.AST]?.filterIsInstance<NewIdentifierBuilder>()
                    ?.find { it.build().name() == "\$stack1" }.apply { assertNotNull(this) }
            }
        vertices.filterIsInstance<NewTypeDeclBuilder>().find { it.build().fullName() == "java.lang.Object" }
            .apply { assertNotNull(this) }
        vertices.filterIsInstance<NewMethodBuilder>().find { it.build().fullName() == "java.lang.Object.<init>" }
            .apply { assertNotNull(this) }
        val mainMethod =
            vertices.filterIsInstance<NewMethodBuilder>()
                .find { it.build().fullName() == "interprocedural.basic.Basic4.main" }
                .apply { assertNotNull(this) }
        val initMethod =
            vertices.filterIsInstance<NewCallBuilder>()
                .find { it.build().methodFullName() == "java.lang.Object: void <init>()" }
                .apply { assertNotNull(this) }
        val fMethod = vertices.filterIsInstance<NewMethodBuilder>()
            .find { it.build().fullName() == "interprocedural.basic.Basic4.f" }
            .apply { assertNotNull(this) }
        val fCall =
            vertices.filterIsInstance<NewCallBuilder>().find { it.build().name() == "f" }.apply { assertNotNull(this) }
        mainMethod!!; initMethod!!; fCall!!; fMethod!!
        graph.edgesOut(initMethod)[EdgeLabel.REF]?.filterIsInstance<NewMethodBuilder>()?.firstOrNull()
            ?.apply { assertNotNull(this); assertEquals(initMethod.build().name(), this.build().name()) }
        graph.edgesOut(fCall)[EdgeLabel.REF]?.contains(fMethod)?.let { assertTrue(it) }
        graph.edgesOut(fCall)[EdgeLabel.ARGUMENT]?.filterIsInstance<NewLiteral>()?.let { lv ->
            assertTrue(lv.any { it.code() == "5" })
            assertTrue(lv.any { it.code() == "6" })
        }
        graph.edgesOut(fMethod)[EdgeLabel.AST]?.filterIsInstance<NewMethodParameterInBuilder>()?.let { mpv ->
            assertTrue(mpv.any { it.build().name() == "i" })
            assertTrue(mpv.any { it.build().name() == "j" })
        }
    }

    @Test
    fun basicCall5Test() {
        val vertices = graph.vertices()
        vertices.filterIsInstance<NewTypeRefBuilder>()
            .find { it.build().typeFullName() == "interprocedural.basic.Basic5" }
            ?.let { trv ->
                assertNotNull(trv)
                val assignVert = graph.edgesIn(trv)[EdgeLabel.AST]?.first().apply { assertNotNull(this) }; assignVert!!
                graph.edgesOut(assignVert)[EdgeLabel.AST]?.filterIsInstance<NewIdentifierBuilder>()
                    ?.find { it.build().name() == "\$stack1" }.apply { assertNotNull(this) }
            }
        vertices.filterIsInstance<NewTypeDeclBuilder>().find { it.build().fullName() == "java.lang.Object" }
            .apply { assertNotNull(this) }
        vertices.filterIsInstance<NewMethodBuilder>().find { it.build().fullName() == "java.lang.Object.<init>" }
            .apply { assertNotNull(this) }
        val mainMethod =
            vertices.filterIsInstance<NewMethodBuilder>()
                .find { it.build().fullName() == "interprocedural.basic.Basic5.main" }
                .apply { assertNotNull(this) }
        val initMethod =
            vertices.filterIsInstance<NewCallBuilder>()
                .find { it.build().methodFullName() == "java.lang.Object: void <init>()" }
                .apply { assertNotNull(this) }
        val fMethod = vertices.filterIsInstance<NewMethodBuilder>()
            .find { it.build().fullName() == "interprocedural.basic.Basic5.f" }
            .apply { assertNotNull(this) }
        val fCall =
            vertices.filterIsInstance<NewCallBuilder>().find { it.build().name() == "f" }.apply { assertNotNull(this) }
        mainMethod!!; initMethod!!; fCall!!; fMethod!!
        graph.edgesOut(initMethod)[EdgeLabel.REF]?.filterIsInstance<NewMethodBuilder>()?.firstOrNull()
            ?.apply { assertNotNull(this); assertEquals(initMethod.build().name(), this.build().name()) }
        graph.edgesOut(fCall)[EdgeLabel.REF]?.contains(fMethod)?.let { assertTrue(it) }
        graph.edgesOut(fCall)[EdgeLabel.ARGUMENT]?.filterIsInstance<NewLiteral>()?.let { lv ->
            assertTrue(lv.any { it.code() == "\"Test\"" })
            assertTrue(lv.any { it.code() == "\"Case\"" })
        }
        graph.edgesOut(fMethod)[EdgeLabel.AST]?.filterIsInstance<NewMethodParameterInBuilder>()?.let { mpv ->
            assertTrue(mpv.any { it.build().name() == "prefix" })
            assertTrue(mpv.any { it.build().name() == "suffix" })
        }
    }
}