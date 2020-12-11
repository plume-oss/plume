package io.github.plume.oss.extractor.interprocedural

import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import io.github.plume.oss.Extractor
import io.github.plume.oss.domain.enums.EdgeLabel
import io.github.plume.oss.domain.models.PlumeGraph
import io.github.plume.oss.domain.models.vertices.*
import io.github.plume.oss.drivers.DriverFactory
import io.github.plume.oss.drivers.GraphDatabase
import io.github.plume.oss.drivers.TinkerGraphDriver
import java.io.File
import java.io.IOException

class BasicInterproceduralTest {
    companion object {
        private var driver = DriverFactory(GraphDatabase.TINKER_GRAPH) as TinkerGraphDriver
        private lateinit var graph: PlumeGraph
        private var PATH: File
        private var CLS_PATH: File
        private val TEST_PATH = "interprocedural${File.separator}basic"

        init {
            val testFileUrl = BasicInterproceduralTest::class.java.classLoader.getResource(TEST_PATH)
                    ?: throw NullPointerException("Unable to obtain test resource")
            PATH = File(testFileUrl.file)
            CLS_PATH = File(PATH.absolutePath.replace(System.getProperty("user.dir") + File.separator, "").replace(TEST_PATH, ""))
        }
    }

    @BeforeEach
    @Throws(IOException::class)
    fun setUp(testInfo: TestInfo) {
        val extractor = Extractor(driver, CLS_PATH)
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
        val mainMethod = vertices.filterIsInstance<MethodVertex>().find { it.fullName == "interprocedural.basic.Basic1.main" }.apply { assertNotNull(this) }
        val fMethod = vertices.filterIsInstance<MethodVertex>().find { it.fullName == "interprocedural.basic.Basic1.f" }.apply { assertNotNull(this) }
        val gMethod = vertices.filterIsInstance<MethodVertex>().find { it.fullName == "interprocedural.basic.Basic1.g" }.apply { assertNotNull(this) }
        val fCall = vertices.filterIsInstance<CallVertex>().find { it.name == "f" }.apply { assertNotNull(this) }
        val gCall = vertices.filterIsInstance<CallVertex>().find { it.name == "g" }.apply { assertNotNull(this) }
        mainMethod!!; fMethod!!; gMethod!!; fCall!!; gCall!!
        graph.edgesOut(fCall)[EdgeLabel.REF]?.contains(fMethod)?.let { assertTrue(it) }
        graph.edgesOut(gCall)[EdgeLabel.REF]?.contains(gMethod)?.let { assertTrue(it) }
    }

    @Test
    fun basicCall2Test() {
        val vertices = graph.vertices()
        val mainMethod = vertices.filterIsInstance<MethodVertex>().find { it.fullName == "interprocedural.basic.Basic2.main" }.apply { assertNotNull(this) }
        val fMethod = vertices.filterIsInstance<MethodVertex>().find { it.fullName == "interprocedural.basic.Basic2.f" }.apply { assertNotNull(this) }
        val fCall = vertices.filterIsInstance<CallVertex>().find { it.name == "f" }.apply { assertNotNull(this) }
        mainMethod!!; fMethod!!; fCall!!
        val assignVert = graph.edgesIn(fCall)[EdgeLabel.ARGUMENT]?.firstOrNull()?.apply { assertNotNull(this) }
        assignVert!!
        graph.edgesOut(assignVert)[EdgeLabel.REF]?.contains(fMethod)?.let { assertTrue(it) }
        graph.edgesOut(fCall)[EdgeLabel.ARGUMENT]?.filterIsInstance<LiteralVertex>()?.firstOrNull()?.let { assertEquals("5", it.code) }
    }

    @Test
    fun basicCall3Test() {
        val vertices = graph.vertices()
        vertices.filterIsInstance<TypeDeclVertex>().find { it.fullName == "java.lang.Object" }.apply { assertNotNull(this) }
        vertices.filterIsInstance<MethodVertex>().find { it.fullName == "java.lang.Object.<init>" }.apply { assertNotNull(this) }
        val mainMethod = vertices.filterIsInstance<MethodVertex>().find { it.fullName == "interprocedural.basic.Basic3.main" }.apply { assertNotNull(this) }
        val initMethod = vertices.filterIsInstance<CallVertex>().find { it.methodFullName == "java.lang.Object: void <init>()" }.apply { assertNotNull(this) }
        val fMethod = vertices.filterIsInstance<MethodVertex>().find { it.fullName == "interprocedural.basic.Basic3.f" }.apply { assertNotNull(this) }
        val fCall = vertices.filterIsInstance<CallVertex>().find { it.name == "f" }.apply { assertNotNull(this) }
        mainMethod!!; initMethod!!; fCall!!; fMethod!!
        graph.edgesOut(initMethod)[EdgeLabel.REF]?.filterIsInstance<MethodVertex>()?.firstOrNull()?.apply { assertNotNull(this); assertEquals(initMethod.name, this.name) }
        graph.edgesOut(fCall)[EdgeLabel.REF]?.contains(fMethod)?.let { assertTrue(it) }
    }

    @Test
    fun basicCall4Test() {
        val vertices = graph.vertices()
        vertices.filterIsInstance<TypeRefVertex>().find { it.typeFullName == "interprocedural.basic.Basic4" }?.let { trv ->
            assertNotNull(trv)
            val assignVert = graph.edgesIn(trv)[EdgeLabel.AST]?.first().apply { assertNotNull(this) }; assignVert!!
            graph.edgesOut(assignVert)[EdgeLabel.AST]?.filterIsInstance<IdentifierVertex>()?.find { it.name == "\$stack1" }.apply { assertNotNull(this) }
        }
        vertices.filterIsInstance<TypeDeclVertex>().find { it.fullName == "java.lang.Object" }.apply { assertNotNull(this) }
        vertices.filterIsInstance<MethodVertex>().find { it.fullName == "java.lang.Object.<init>" }.apply { assertNotNull(this) }
        val mainMethod = vertices.filterIsInstance<MethodVertex>().find { it.fullName == "interprocedural.basic.Basic4.main" }.apply { assertNotNull(this) }
        val initMethod = vertices.filterIsInstance<CallVertex>().find { it.methodFullName == "java.lang.Object: void <init>()" }.apply { assertNotNull(this) }
        val fMethod = vertices.filterIsInstance<MethodVertex>().find { it.fullName == "interprocedural.basic.Basic4.f" }.apply { assertNotNull(this) }
        val fCall = vertices.filterIsInstance<CallVertex>().find { it.name == "f" }.apply { assertNotNull(this) }
        mainMethod!!; initMethod!!; fCall!!; fMethod!!
        graph.edgesOut(initMethod)[EdgeLabel.REF]?.filterIsInstance<MethodVertex>()?.firstOrNull()?.apply { assertNotNull(this); assertEquals(initMethod.name, this.name) }
        graph.edgesOut(fCall)[EdgeLabel.REF]?.contains(fMethod)?.let { assertTrue(it) }
        graph.edgesOut(fCall)[EdgeLabel.ARGUMENT]?.filterIsInstance<LiteralVertex>()?.let { lv ->
            assertTrue(lv.any { it.code == "5" })
            assertTrue(lv.any { it.code == "6" })
        }
        graph.edgesOut(fMethod)[EdgeLabel.AST]?.filterIsInstance<MethodParameterInVertex>()?.let { mpv ->
            assertTrue(mpv.any { it.name == "i" })
            assertTrue(mpv.any { it.name == "j" })
        }
    }

    @Test
    fun basicCall5Test() {
        val vertices = graph.vertices()
        vertices.filterIsInstance<TypeRefVertex>().find { it.typeFullName == "interprocedural.basic.Basic5" }?.let { trv ->
            assertNotNull(trv)
            val assignVert = graph.edgesIn(trv)[EdgeLabel.AST]?.first().apply { assertNotNull(this) }; assignVert!!
            graph.edgesOut(assignVert)[EdgeLabel.AST]?.filterIsInstance<IdentifierVertex>()?.find { it.name == "\$stack1" }.apply { assertNotNull(this) }
        }
        vertices.filterIsInstance<TypeDeclVertex>().find { it.fullName == "java.lang.Object" }.apply { assertNotNull(this) }
        vertices.filterIsInstance<MethodVertex>().find { it.fullName == "java.lang.Object.<init>" }.apply { assertNotNull(this) }
        val mainMethod = vertices.filterIsInstance<MethodVertex>().find { it.fullName == "interprocedural.basic.Basic5.main" }.apply { assertNotNull(this) }
        val initMethod = vertices.filterIsInstance<CallVertex>().find { it.methodFullName == "java.lang.Object: void <init>()" }.apply { assertNotNull(this) }
        val fMethod = vertices.filterIsInstance<MethodVertex>().find { it.fullName == "interprocedural.basic.Basic5.f" }.apply { assertNotNull(this) }
        val fCall = vertices.filterIsInstance<CallVertex>().find { it.name == "f" }.apply { assertNotNull(this) }
        mainMethod!!; initMethod!!; fCall!!; fMethod!!
        graph.edgesOut(initMethod)[EdgeLabel.REF]?.filterIsInstance<MethodVertex>()?.firstOrNull()?.apply { assertNotNull(this); assertEquals(initMethod.name, this.name) }
        graph.edgesOut(fCall)[EdgeLabel.REF]?.contains(fMethod)?.let { assertTrue(it) }
        graph.edgesOut(fCall)[EdgeLabel.ARGUMENT]?.filterIsInstance<LiteralVertex>()?.let { lv ->
            assertTrue(lv.any { it.code == "\"Test\"" })
            assertTrue(lv.any { it.code == "\"Case\"" })
        }
        graph.edgesOut(fMethod)[EdgeLabel.AST]?.filterIsInstance<MethodParameterInVertex>()?.let { mpv ->
            assertTrue(mpv.any { it.name == "prefix" })
            assertTrue(mpv.any { it.name == "suffix" })
        }
    }
}