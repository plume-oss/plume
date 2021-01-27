package io.github.plume.oss.extractor.interprocedural

import io.github.plume.oss.Extractor
import io.github.plume.oss.domain.models.PlumeGraph
import io.github.plume.oss.drivers.DriverFactory
import io.github.plume.oss.drivers.GraphDatabase
import io.github.plume.oss.drivers.TinkerGraphDriver
import io.shiftleft.codepropertygraph.generated.nodes.*
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInfo
import java.io.File
import java.io.IOException

class TypeInterproceduralTest {

    companion object {
        private val driver = DriverFactory(GraphDatabase.TINKER_GRAPH) as TinkerGraphDriver
        private lateinit var graph: PlumeGraph
        private var PATH: File
        private val TEST_PATH = "interprocedural${File.separator}type"

        init {
            val testFileUrl = TypeInterproceduralTest::class.java.classLoader.getResource(TEST_PATH)
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
        val resourceDir = "${PATH.absolutePath}${File.separator}Type$currentTestNumber.java"
        // Load test resource and project + export graph
        val f = File(resourceDir)
        extractor.load(f)
        extractor.project()
        graph = driver.getWholeGraph()
    }

    @AfterEach
    fun tearDown() {
        driver.close()
    }

    @Test
    fun type1Test() {
        val vertices = graph.vertices()
        vertices.filterIsInstance<NewLocalBuilder>().let { localList ->
            assertNotNull(localList.firstOrNull {
                it.build().name() == "intList" && it.build().typeFullName() == "java.util.LinkedList"
            })
            assertNotNull(localList.firstOrNull {
                it.build().name() == "stringList" && it.build().typeFullName() == "java.util.LinkedList"
            })
            assertNotNull(localList.firstOrNull {
                it.build().name() == "\$stack3" && it.build().typeFullName() == "java.util.LinkedList"
            })
            assertNotNull(localList.firstOrNull {
                it.build().name() == "\$stack4" && it.build().typeFullName() == "java.util.LinkedList"
            })
        }
        vertices.filterIsInstance<NewCallBuilder>().filter { it.build().name() == "<init>" }.let { callList ->
            assertNotNull(callList.firstOrNull { it.build().methodFullName() == "java.util.LinkedList: void <init>()" })
            assertNotNull(callList.firstOrNull { it.build().methodFullName() == "java.lang.Object: void <init>()" })
        }
        vertices.filterIsInstance<NewTypeRefBuilder>().filter { it.build().typeFullName() == "java.util.LinkedList" }
            .let { typeRefs -> assertEquals(2, typeRefs.size) }
    }

    @Test
    fun type2Test() {
        val vertices = graph.vertices()
        assertNotNull(
            vertices.filterIsInstance<NewLocalBuilder>()
                .firstOrNull { it.build().name() == "intArray" && it.build().typeFullName() == "int[]" })
        vertices.filterIsInstance<NewIdentifierBuilder>().filter { it.build().name().contains("intArray") }
            .let { callList ->
                assertNotNull(callList.firstOrNull { it.build().argumentIndex() == 0 })
                assertNotNull(callList.firstOrNull { it.build().argumentIndex() == 4 })
            }
    }

    @Test
    fun type3Test() {
        val vertices = graph.vertices()
        assertNotNull(
            vertices.filterIsInstance<NewLocalBuilder>()
                .firstOrNull { it.build().name() == "cls" && it.build().typeFullName() == "java.lang.Class" })
        assertNotNull(
            vertices.filterIsInstance<NewLiteralBuilder>()
                .firstOrNull {
                    it.build().code() == "class \"Lintraprocedural/type/Type3;\"" && it.build()
                        .typeFullName() == "java.lang.Class"
                })
    }
}