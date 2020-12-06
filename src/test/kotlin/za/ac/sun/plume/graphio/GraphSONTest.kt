package za.ac.sun.plume.graphio

import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import za.ac.sun.plume.TestDomainResources.Companion.DISPATCH_1
import za.ac.sun.plume.TestDomainResources.Companion.EVAL_1
import za.ac.sun.plume.TestDomainResources.Companion.INT_1
import za.ac.sun.plume.TestDomainResources.Companion.INT_2
import za.ac.sun.plume.TestDomainResources.Companion.STRING_1
import za.ac.sun.plume.TestDomainResources.Companion.STRING_2
import za.ac.sun.plume.domain.enums.EdgeLabel
import za.ac.sun.plume.domain.models.PlumeGraph
import za.ac.sun.plume.domain.models.vertices.*
import za.ac.sun.plume.drivers.DriverFactory
import za.ac.sun.plume.drivers.GraphDatabase
import za.ac.sun.plume.drivers.TinkerGraphDriver
import java.io.File
import java.io.FileWriter

class GraphSONTest {

    companion object {
        val driver = (DriverFactory.invoke(GraphDatabase.TINKER_GRAPH) as TinkerGraphDriver).apply { connect() }
        private val v1 = MethodVertex(STRING_1, STRING_1, STRING_2, STRING_1, INT_1, INT_2, INT_1)
        private val v2 = MethodParameterInVertex(STRING_1, EVAL_1, STRING_1, INT_1, STRING_2, INT_2)
        private val v3 = BlockVertex(STRING_1, STRING_1, INT_1, INT_2, INT_2, INT_1)
        private val v4 = CallVertex(STRING_1, INT_1, DISPATCH_1, STRING_1, STRING_1, STRING_2, STRING_2, STRING_2, INT_1, INT_1, INT_1)
        private val v5 = LocalVertex(STRING_1, STRING_2, INT_1, INT_1, STRING_1, INT_1)
        private val v6 = IdentifierVertex(STRING_1, STRING_1, STRING_1, INT_1, INT_1, INT_1, INT_1)
        private val v7 = TypeDeclVertex(STRING_1, STRING_2, STRING_1, INT_1)
        private val v8 = LiteralVertex(STRING_2, STRING_2, INT_1, INT_1, INT_1, INT_1)
        private val v9 = ReturnVertex(INT_1, INT_1, INT_1, INT_1, STRING_1)
        private val v10 = MethodReturnVertex(STRING_1, EVAL_1, STRING_1, INT_1, INT_1, INT_1)
        private val v11 = FileVertex(STRING_1, STRING_2, INT_1)
        private val v12 = NamespaceBlockVertex(STRING_1, STRING_1, INT_1)
        private val v13 = NamespaceBlockVertex(STRING_2, STRING_2, INT_1)
        private val v14 = MetaDataVertex(STRING_1, STRING_2)

        private lateinit var graph: PlumeGraph
        private val tempDir = System.getProperty("java.io.tmpdir")
        private val testGraphSON = "${tempDir}/plume/plume_driver_test.json"

        @JvmStatic
        @AfterAll
        fun tearDownAll() {
            File(testGraphSON).delete()
        }
    }

    @BeforeEach
    fun setUp() {
        // Create program data
        driver.addVertex(v14)
        driver.addEdge(v11, v12, EdgeLabel.AST)
        driver.addEdge(v12, v13, EdgeLabel.AST)
        // Create method head
        driver.addEdge(v7, v1, EdgeLabel.AST)
        driver.addEdge(v1, v11, EdgeLabel.SOURCE_FILE)
        driver.addEdge(v1, v2, EdgeLabel.AST)
        driver.addEdge(v1, v5, EdgeLabel.AST)
        driver.addEdge(v1, v3, EdgeLabel.AST)
        driver.addEdge(v1, v3, EdgeLabel.CFG)
        // Create method body
        driver.addEdge(v3, v4, EdgeLabel.AST)
        driver.addEdge(v3, v4, EdgeLabel.CFG)
        driver.addEdge(v4, v6, EdgeLabel.AST)
        driver.addEdge(v4, v8, EdgeLabel.AST)
        driver.addEdge(v4, v6, EdgeLabel.ARGUMENT)
        driver.addEdge(v4, v8, EdgeLabel.ARGUMENT)
        driver.addEdge(v3, v9, EdgeLabel.AST)
        driver.addEdge(v4, v9, EdgeLabel.CFG)
        driver.addEdge(v1, v10, EdgeLabel.AST)
        driver.addEdge(v9, v10, EdgeLabel.CFG)
        // Link dependencies
        driver.addEdge(v6, v5, EdgeLabel.REF)

        graph = driver.getWholeGraph()
    }

    @AfterEach
    fun tearDown() {
        driver.clearGraph()
    }

    @Test
    fun graphWriteTest() {
        GraphSONWriter.write(graph, FileWriter(testGraphSON))
        driver.clearGraph()
        driver.importGraph(testGraphSON)
        val otherGraph = driver.getWholeGraph()
        assertEquals(graph, otherGraph)
    }
}