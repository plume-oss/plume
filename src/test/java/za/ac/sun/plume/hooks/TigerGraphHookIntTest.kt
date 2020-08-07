package za.ac.sun.plume.hooks

import org.json.JSONArray
import org.json.JSONObject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import za.ac.sun.plume.TestDomainResources.Companion.INT_3
import za.ac.sun.plume.TestDomainResources.Companion.INT_4
import za.ac.sun.plume.TestDomainResources.Companion.ROOT_PACKAGE
import za.ac.sun.plume.TestDomainResources.Companion.SECOND_PACKAGE
import za.ac.sun.plume.TestDomainResources.Companion.THIRD_PACKAGE
import za.ac.sun.plume.domain.enums.VertexLabels
import za.ac.sun.plume.domain.mappers.VertexMapper
import za.ac.sun.plume.domain.models.vertices.*

class TigerGraphHookIntTest : AbstractHookTest() {

    override fun provideHook(): TigerGraphHook = provideBuilder().build()

    override fun provideBuilder(): TigerGraphHook.Builder = TigerGraphHook.Builder()
            .hostname(DEFAULT_HOSTNAME).port(DEFAULT_PORT).secure(false)

    private fun headers(): Map<String, String> = mapOf("Content-Type" to "application/json")

    fun get(endpoint: String): JSONArray {
        val response = khttp.get(
                url = "http://$DEFAULT_HOSTNAME:$DEFAULT_PORT/$endpoint",
                headers = headers()
        )
        return response.jsonObject["results"] as JSONArray
    }

    @Nested
    inner class RestPPCheckMethodJoinInteraction : CheckMethodJoinInteraction() {
        lateinit var tigerGraphHook: TigerGraphHook

        @BeforeEach
        override fun setUp() {
            super.setUp()
            tigerGraphHook = hook as TigerGraphHook
        }

        @Test
        override fun joinMethodToMethodParamIn() {
            super.joinMethodToMethodParamIn()
            val methodParamInRaw = get("graph/$GRAPH_NAME/vertices/${VertexLabels.METHOD_PARAMETER_IN.name}_VERT")
            val methodRaw = get("graph/$GRAPH_NAME/vertices/${VertexLabels.METHOD.name}_VERT")
            val fileRaw = get("graph/$GRAPH_NAME/vertices/${VertexLabels.FILE.name}_VERT")
            assertTrue(methodParamInRaw.any())
            assertTrue(methodRaw.any())
            assertTrue(fileRaw.any())
            val methodParamIn = VertexMapper.mapToVertex(tigerGraphHook.flattenVertexResult(methodParamInRaw.first() as JSONObject))
            val method = VertexMapper.mapToVertex(tigerGraphHook.flattenVertexResult(methodRaw.first() as JSONObject))
            val file = VertexMapper.mapToVertex(tigerGraphHook.flattenVertexResult(fileRaw.first() as JSONObject))
            assertTrue(methodParamIn is MethodParameterInVertex)
            assertTrue(method is MethodVertex)
            assertTrue(file is FileVertex)
            val methodEdges = get("graph/$GRAPH_NAME/edges/${VertexLabels.METHOD.name}_VERT/${method.hashCode()}")
            val fileEdges = get("graph/$GRAPH_NAME/edges/${VertexLabels.FILE.name}_VERT/${file.hashCode()}")
            assertTrue(methodEdges.any())
            assertTrue(fileEdges.any())
            assertEquals(method.hashCode().toString(), (methodEdges.first() as JSONObject)["from_id"])
            assertEquals(methodParamIn.hashCode().toString(), (methodEdges.first() as JSONObject)["to_id"])
            assertEquals(file.hashCode().toString(), (fileEdges.first() as JSONObject)["from_id"])
            assertEquals(method.hashCode().toString(), (fileEdges.first() as JSONObject)["to_id"])
        }

        @Test
        override fun joinMethodToMethodReturn() {
            super.joinMethodToMethodReturn()
            val methodReturnRaw = get("graph/$GRAPH_NAME/vertices/${VertexLabels.METHOD_RETURN.name}_VERT")
            val methodRaw = get("graph/$GRAPH_NAME/vertices/${VertexLabels.METHOD.name}_VERT")
            val fileRaw = get("graph/$GRAPH_NAME/vertices/${VertexLabels.FILE.name}_VERT")
            assertTrue(methodReturnRaw.any())
            assertTrue(methodRaw.any())
            assertTrue(fileRaw.any())
            val methodReturn = VertexMapper.mapToVertex(tigerGraphHook.flattenVertexResult(methodReturnRaw.first() as JSONObject))
            val method = VertexMapper.mapToVertex(tigerGraphHook.flattenVertexResult(methodRaw.first() as JSONObject))
            val file = VertexMapper.mapToVertex(tigerGraphHook.flattenVertexResult(fileRaw.first() as JSONObject))
            assertTrue(methodReturn is MethodReturnVertex)
            assertTrue(method is MethodVertex)
            assertTrue(file is FileVertex)
            val methodEdges = get("graph/$GRAPH_NAME/edges/${VertexLabels.METHOD.name}_VERT/${method.hashCode()}")
            val fileEdges = get("graph/$GRAPH_NAME/edges/${VertexLabels.FILE.name}_VERT/${file.hashCode()}")
            assertTrue(methodEdges.any())
            assertTrue(fileEdges.any())
            assertEquals(method.hashCode().toString(), (methodEdges.first() as JSONObject)["from_id"])
            assertEquals(methodReturn.hashCode().toString(), (methodEdges.first() as JSONObject)["to_id"])
            assertEquals(file.hashCode().toString(), (fileEdges.first() as JSONObject)["from_id"])
            assertEquals(method.hashCode().toString(), (fileEdges.first() as JSONObject)["to_id"])
        }

        @Test
        override fun joinMethodToModifier() {
            super.joinMethodToModifier()
            val modifierRaw = get("graph/$GRAPH_NAME/vertices/${VertexLabels.MODIFIER.name}_VERT")
            val methodRaw = get("graph/$GRAPH_NAME/vertices/${VertexLabels.METHOD.name}_VERT")
            val fileRaw = get("graph/$GRAPH_NAME/vertices/${VertexLabels.FILE.name}_VERT")
            assertTrue(modifierRaw.any())
            assertTrue(methodRaw.any())
            assertTrue(fileRaw.any())
            val modifier = VertexMapper.mapToVertex(tigerGraphHook.flattenVertexResult(modifierRaw.first() as JSONObject))
            val method = VertexMapper.mapToVertex(tigerGraphHook.flattenVertexResult(methodRaw.first() as JSONObject))
            val file = VertexMapper.mapToVertex(tigerGraphHook.flattenVertexResult(fileRaw.first() as JSONObject))
            assertTrue(modifier is ModifierVertex)
            assertTrue(method is MethodVertex)
            assertTrue(file is FileVertex)
            val methodEdges = get("graph/$GRAPH_NAME/edges/${VertexLabels.METHOD.name}_VERT/${method.hashCode()}")
            val fileEdges = get("graph/$GRAPH_NAME/edges/${VertexLabels.FILE.name}_VERT/${file.hashCode()}")
            assertTrue(methodEdges.any())
            assertTrue(fileEdges.any())
            assertEquals(method.hashCode().toString(), (methodEdges.first() as JSONObject)["from_id"])
            assertEquals(modifier.hashCode().toString(), (methodEdges.first() as JSONObject)["to_id"])
            assertEquals(file.hashCode().toString(), (fileEdges.first() as JSONObject)["from_id"])
            assertEquals(method.hashCode().toString(), (fileEdges.first() as JSONObject)["to_id"])
        }
    }

    @Nested
    inner class RestPPFileJoinInteraction : FileJoinInteraction() {
        lateinit var tigerGraphHook: TigerGraphHook

        @BeforeEach
        override fun setUp() {
            super.setUp()
            tigerGraphHook = hook as TigerGraphHook
        }

        @Test
        override fun testJoinFile2NamespaceBlock() {
            super.testJoinFile2NamespaceBlock()
            val namespaceRaw = get("graph/$GRAPH_NAME/vertices/${VertexLabels.NAMESPACE_BLOCK.name}_VERT")
            val fileRaw = get("graph/$GRAPH_NAME/vertices/${VertexLabels.FILE.name}_VERT")
            assertTrue(namespaceRaw.any())
            assertTrue(fileRaw.any())
            val namespace = VertexMapper.mapToVertex(tigerGraphHook.flattenVertexResult(namespaceRaw.first() as JSONObject))
            val file = VertexMapper.mapToVertex(tigerGraphHook.flattenVertexResult(fileRaw.first() as JSONObject))
            assertTrue(namespace is NamespaceBlockVertex)
            assertTrue(file is FileVertex)
            val namespaceEdges = get("graph/$GRAPH_NAME/edges/${VertexLabels.NAMESPACE_BLOCK.name}_VERT/${namespace.hashCode()}")
            assertTrue(namespaceEdges.any())
            assertEquals(namespace.hashCode().toString(), (namespaceEdges.first() as JSONObject)["from_id"])
            assertEquals(file.hashCode().toString(), (namespaceEdges.first() as JSONObject)["to_id"])
        }

        @Test
        override fun testJoinFile2Method() {
            super.testJoinFile2Method()
            val methodRaw = get("graph/$GRAPH_NAME/vertices/${VertexLabels.METHOD.name}_VERT")
            val fileRaw = get("graph/$GRAPH_NAME/vertices/${VertexLabels.FILE.name}_VERT")
            assertTrue(methodRaw.any())
            assertTrue(fileRaw.any())
            val method = VertexMapper.mapToVertex(tigerGraphHook.flattenVertexResult(methodRaw.first() as JSONObject))
            val file = VertexMapper.mapToVertex(tigerGraphHook.flattenVertexResult(fileRaw.first() as JSONObject))
            assertTrue(method is MethodVertex)
            assertTrue(file is FileVertex)
            val fileEdges = get("graph/$GRAPH_NAME/edges/${VertexLabels.FILE.name}_VERT/${file.hashCode()}")
            assertTrue(fileEdges.any())
            assertEquals(file.hashCode().toString(), (fileEdges.first() as JSONObject)["from_id"])
            assertEquals(method.hashCode().toString(), (fileEdges.first() as JSONObject)["to_id"])
        }
    }

    @Nested
    inner class RestPPBlockJoinInteraction : BlockJoinInteraction() {
        lateinit var tigerGraphHook: TigerGraphHook

        @BeforeEach
        override fun setUp() {
            super.setUp()
            tigerGraphHook = hook as TigerGraphHook
        }

        @Test
        override fun testMethodJoinBlockTest() {
            super.testMethodJoinBlockTest()
            val blockRaw = get("graph/$GRAPH_NAME/vertices/${VertexLabels.BLOCK.name}_VERT")
            val methodRaw = get("graph/$GRAPH_NAME/vertices/${VertexLabels.METHOD.name}_VERT")
            val fileRaw = get("graph/$GRAPH_NAME/vertices/${VertexLabels.FILE.name}_VERT")
            assertTrue(blockRaw.any())
            assertTrue(methodRaw.any())
            assertTrue(fileRaw.any())
            val block = VertexMapper.mapToVertex(tigerGraphHook.flattenVertexResult(blockRaw.first() as JSONObject))
            val method = VertexMapper.mapToVertex(tigerGraphHook.flattenVertexResult(methodRaw.first() as JSONObject))
            val file = VertexMapper.mapToVertex(tigerGraphHook.flattenVertexResult(fileRaw.first() as JSONObject))
            assertTrue(block is BlockVertex)
            assertTrue(method is MethodVertex)
            assertTrue(file is FileVertex)
            val methodEdges = get("graph/$GRAPH_NAME/edges/${VertexLabels.METHOD.name}_VERT/${method.hashCode()}")
            val fileEdges = get("graph/$GRAPH_NAME/edges/${VertexLabels.FILE.name}_VERT/${file.hashCode()}")
            assertTrue(methodEdges.any())
            assertTrue(fileEdges.any())
            assertEquals(method.hashCode().toString(), (methodEdges.first() as JSONObject)["from_id"])
            assertEquals(block.hashCode().toString(), (methodEdges.first() as JSONObject)["to_id"])
            assertEquals(file.hashCode().toString(), (fileEdges.first() as JSONObject)["from_id"])
            assertEquals(method.hashCode().toString(), (fileEdges.first() as JSONObject)["to_id"])
        }

        @Test
        override fun testBlockJoinBlockTest() {
            super.testBlockJoinBlockTest()
            val blockRaw = get("graph/$GRAPH_NAME/vertices/${VertexLabels.BLOCK.name}_VERT")
            val methodRaw = get("graph/$GRAPH_NAME/vertices/${VertexLabels.METHOD.name}_VERT")
            assertTrue(blockRaw.any())
            assertTrue(methodRaw.any())
            val blockVertices = blockRaw.map { VertexMapper.mapToVertex(tigerGraphHook.flattenVertexResult(it as JSONObject)) as BlockVertex }
            val block1 = blockVertices.find { it.order == INT_3 }
            val block2 = blockVertices.find { it.order == INT_4 }
            val method = VertexMapper.mapToVertex(tigerGraphHook.flattenVertexResult(methodRaw.first() as JSONObject))
            assertTrue(block1 is BlockVertex)
            assertTrue(block2 is BlockVertex)
            assertTrue(method is MethodVertex)
            val methodEdges = get("graph/$GRAPH_NAME/edges/${VertexLabels.METHOD.name}_VERT/${method.hashCode()}")
            val block1Edges = get("graph/$GRAPH_NAME/edges/${VertexLabels.BLOCK.name}_VERT/${block1.hashCode()}")
            assertTrue(methodEdges.any())
            assertTrue(block1Edges.any())
            assertEquals(method.hashCode().toString(), (methodEdges.first() as JSONObject)["from_id"])
            assertEquals(block1.hashCode().toString(), (methodEdges.first() as JSONObject)["to_id"])
            assertEquals(block1.hashCode().toString(), (block1Edges.first() as JSONObject)["from_id"])
            assertEquals(block2.hashCode().toString(), (block1Edges.first() as JSONObject)["to_id"])
        }

        @Test
        override fun testAssignLiteralToBlock() {
            super.testAssignLiteralToBlock()
            val blockRaw = get("graph/$GRAPH_NAME/vertices/${VertexLabels.BLOCK.name}_VERT")
            val methodRaw = get("graph/$GRAPH_NAME/vertices/${VertexLabels.METHOD.name}_VERT")
            val literalRaw = get("graph/$GRAPH_NAME/vertices/${VertexLabels.LITERAL.name}_VERT")
            assertTrue(blockRaw.any())
            assertTrue(methodRaw.any())
            assertTrue(literalRaw.any())
            val block = VertexMapper.mapToVertex(tigerGraphHook.flattenVertexResult(blockRaw.first() as JSONObject))
            val method = VertexMapper.mapToVertex(tigerGraphHook.flattenVertexResult(methodRaw.first() as JSONObject))
            val literal = VertexMapper.mapToVertex(tigerGraphHook.flattenVertexResult(literalRaw.first() as JSONObject))
            assertTrue(block is BlockVertex)
            assertTrue(method is MethodVertex)
            assertTrue(literal is LiteralVertex)
            val methodEdges = get("graph/$GRAPH_NAME/edges/${VertexLabels.METHOD.name}_VERT/${method.hashCode()}")
            val blockEdges = get("graph/$GRAPH_NAME/edges/${VertexLabels.BLOCK.name}_VERT/${block.hashCode()}")
            assertTrue(methodEdges.any())
            assertTrue(blockEdges.any())
            assertEquals(method.hashCode().toString(), (methodEdges.first() as JSONObject)["from_id"])
            assertEquals(block.hashCode().toString(), (methodEdges.first() as JSONObject)["to_id"])
            assertEquals(block.hashCode().toString(), (blockEdges.first() as JSONObject)["from_id"])
            assertEquals(literal.hashCode().toString(), (blockEdges.first() as JSONObject)["to_id"])
        }

        @Test
        override fun testAssignLocalToBlock() {
            super.testAssignLocalToBlock()
            val blockRaw = get("graph/$GRAPH_NAME/vertices/${VertexLabels.BLOCK.name}_VERT")
            val methodRaw = get("graph/$GRAPH_NAME/vertices/${VertexLabels.METHOD.name}_VERT")
            val localRaw = get("graph/$GRAPH_NAME/vertices/${VertexLabels.LOCAL.name}_VERT")
            assertTrue(blockRaw.any())
            assertTrue(methodRaw.any())
            assertTrue(localRaw.any())
            val block = VertexMapper.mapToVertex(tigerGraphHook.flattenVertexResult(blockRaw.first() as JSONObject))
            val method = VertexMapper.mapToVertex(tigerGraphHook.flattenVertexResult(methodRaw.first() as JSONObject))
            val local = VertexMapper.mapToVertex(tigerGraphHook.flattenVertexResult(localRaw.first() as JSONObject))
            assertTrue(block is BlockVertex)
            assertTrue(method is MethodVertex)
            assertTrue(local is LocalVertex)
            val methodEdges = get("graph/$GRAPH_NAME/edges/${VertexLabels.METHOD.name}_VERT/${method.hashCode()}")
            val blockEdges = get("graph/$GRAPH_NAME/edges/${VertexLabels.BLOCK.name}_VERT/${block.hashCode()}")
            assertTrue(methodEdges.any())
            assertTrue(blockEdges.any())
            assertEquals(method.hashCode().toString(), (methodEdges.first() as JSONObject)["from_id"])
            assertEquals(block.hashCode().toString(), (methodEdges.first() as JSONObject)["to_id"])
            assertEquals(block.hashCode().toString(), (blockEdges.first() as JSONObject)["from_id"])
            assertEquals(local.hashCode().toString(), (blockEdges.first() as JSONObject)["to_id"])
        }

        @Test
        override fun testAssignControlToBlock() {
            super.testAssignControlToBlock()
            val blockRaw = get("graph/$GRAPH_NAME/vertices/${VertexLabels.BLOCK.name}_VERT")
            val methodRaw = get("graph/$GRAPH_NAME/vertices/${VertexLabels.METHOD.name}_VERT")
            val controlRaw = get("graph/$GRAPH_NAME/vertices/${VertexLabels.CONTROL_STRUCTURE.name}_VERT")
            assertTrue(blockRaw.any())
            assertTrue(methodRaw.any())
            assertTrue(controlRaw.any())
            val block = VertexMapper.mapToVertex(tigerGraphHook.flattenVertexResult(blockRaw.first() as JSONObject))
            val method = VertexMapper.mapToVertex(tigerGraphHook.flattenVertexResult(methodRaw.first() as JSONObject))
            val control = VertexMapper.mapToVertex(tigerGraphHook.flattenVertexResult(controlRaw.first() as JSONObject))
            assertTrue(block is BlockVertex)
            assertTrue(method is MethodVertex)
            assertTrue(control is ControlStructureVertex)
            val methodEdges = get("graph/$GRAPH_NAME/edges/${VertexLabels.METHOD.name}_VERT/${method.hashCode()}")
            val blockEdges = get("graph/$GRAPH_NAME/edges/${VertexLabels.BLOCK.name}_VERT/${block.hashCode()}")
            assertTrue(methodEdges.any())
            assertTrue(blockEdges.any())
            assertEquals(method.hashCode().toString(), (methodEdges.first() as JSONObject)["from_id"])
            assertEquals(block.hashCode().toString(), (methodEdges.first() as JSONObject)["to_id"])
            assertEquals(block.hashCode().toString(), (blockEdges.first() as JSONObject)["from_id"])
            assertEquals(control.hashCode().toString(), (blockEdges.first() as JSONObject)["to_id"])
        }
    }

    @Nested
    inner class RestPPNamespaceBlockJoinInteraction : NamespaceBlockJoinInteraction() {
        lateinit var tigerGraphHook: TigerGraphHook

        @BeforeEach
        override fun setUp() {
            super.setUp()
            tigerGraphHook = hook as TigerGraphHook
        }

        @Test
        override fun joinTwoNamespaceBlocks() {
            super.joinTwoNamespaceBlocks()
            val namespaceRaw = get("graph/$GRAPH_NAME/vertices/${VertexLabels.NAMESPACE_BLOCK.name}_VERT")
            assertTrue(namespaceRaw.any())
            val namespaceVertices = namespaceRaw.map { VertexMapper.mapToVertex(tigerGraphHook.flattenVertexResult(it as JSONObject)) as NamespaceBlockVertex }
            val namespace1 = namespaceVertices.find { it.name == ROOT_PACKAGE }
            val namespace2 = namespaceVertices.find { it.name == SECOND_PACKAGE }
            assertTrue(namespace1 is NamespaceBlockVertex)
            assertTrue(namespace2 is NamespaceBlockVertex)
            val namespaceEdges = get("graph/$GRAPH_NAME/edges/${VertexLabels.NAMESPACE_BLOCK.name}_VERT/${namespace1.hashCode()}")
            assertEquals(namespace1.hashCode().toString(), (namespaceEdges.first() as JSONObject)["from_id"])
            assertEquals(namespace2.hashCode().toString(), (namespaceEdges.first() as JSONObject)["to_id"])
        }

        @Test
        override fun joinThreeNamespaceBlocks() {
            super.joinThreeNamespaceBlocks()
            val namespaceRaw = get("graph/$GRAPH_NAME/vertices/${VertexLabels.NAMESPACE_BLOCK.name}_VERT")
            assertTrue(namespaceRaw.any())
            val namespaceVertices = namespaceRaw.map { VertexMapper.mapToVertex(tigerGraphHook.flattenVertexResult(it as JSONObject)) as NamespaceBlockVertex }
            val namespace1 = namespaceVertices.find { it.name == ROOT_PACKAGE }
            val namespace2 = namespaceVertices.find { it.name == SECOND_PACKAGE }
            val namespace3 = namespaceVertices.find { it.name == THIRD_PACKAGE }
            val namespace1Edges = get("graph/$GRAPH_NAME/edges/${VertexLabels.NAMESPACE_BLOCK.name}_VERT/${namespace1.hashCode()}")
            val namespace2Edges = get("graph/$GRAPH_NAME/edges/${VertexLabels.NAMESPACE_BLOCK.name}_VERT/${namespace2.hashCode()}")
            assertEquals(namespace1.hashCode().toString(), (namespace1Edges.first() as JSONObject)["from_id"])
            assertEquals(namespace2.hashCode().toString(), (namespace1Edges.first() as JSONObject)["to_id"])
            assertEquals(namespace2.hashCode().toString(), (namespace2Edges.first() as JSONObject)["from_id"])
            assertEquals(namespace3.hashCode().toString(), (namespace2Edges.first() as JSONObject)["to_id"])
        }

        @Test
        override fun joinExistingConnection() {
            super.joinExistingConnection()
            val namespaceRaw = get("graph/$GRAPH_NAME/vertices/${VertexLabels.NAMESPACE_BLOCK.name}_VERT")
            assertTrue(namespaceRaw.any())
            val namespaceVertices = namespaceRaw.map { VertexMapper.mapToVertex(tigerGraphHook.flattenVertexResult(it as JSONObject)) as NamespaceBlockVertex }
            val namespace1 = namespaceVertices.find { it.name == ROOT_PACKAGE }
            val namespace2 = namespaceVertices.find { it.name == SECOND_PACKAGE }
            assertTrue(namespace1 is NamespaceBlockVertex)
            assertTrue(namespace2 is NamespaceBlockVertex)
            val namespaceEdges = get("graph/$GRAPH_NAME/edges/${VertexLabels.NAMESPACE_BLOCK.name}_VERT/${namespace1.hashCode()}")
            assertEquals(namespace1.hashCode().toString(), (namespaceEdges.first() as JSONObject)["from_id"])
            assertEquals(namespace2.hashCode().toString(), (namespaceEdges.first() as JSONObject)["to_id"])
        }
    }

    @Nested
    inner class RestPPUpdateChecks : UpdateChecks() {
        lateinit var tigerGraphHook: TigerGraphHook

        @BeforeEach
        override fun setUp() {
            super.setUp()
            tigerGraphHook = hook as TigerGraphHook
            val blockRaw = get("graph/$GRAPH_NAME/vertices/${VertexLabels.BLOCK.name}_VERT")
            assertTrue(blockRaw.any())
            val blockMap = tigerGraphHook.flattenVertexResult(blockRaw.first() as JSONObject)
            val block = VertexMapper.mapToVertex(blockMap)
            assertTrue(block is BlockVertex)
            assertEquals(super.initValue, blockMap[super.keyToTest])
        }

        @Test
        override fun testUpdateOnOneBlockProperty() {
            super.testUpdateOnOneBlockProperty()
            val blockRaw = get("graph/$GRAPH_NAME/vertices/${VertexLabels.BLOCK.name}_VERT")
            assertTrue(blockRaw.any())
            val blockMap = tigerGraphHook.flattenVertexResult(blockRaw.first() as JSONObject)
            val block = VertexMapper.mapToVertex(blockMap)
            assertTrue(block is BlockVertex)
            assertEquals(super.updatedValue, blockMap[super.keyToTest])
        }
    }

    companion object {
        private const val DEFAULT_HOSTNAME = "127.0.0.1"
        private const val DEFAULT_PORT = 9000
        private const val GRAPH_NAME = "cpg"
    }
}