package za.ac.sun.plume.drivers

import org.junit.jupiter.api.Test

class TigerGraphDriverIntTest {

    @Test
    fun test1() {

    }

//        @Test
//        override fun joinMethodToMethodReturn() {
//            super.joinMethodToMethodReturn()
//            val methodReturnRaw = get("graph/$GRAPH_NAME/vertices/${VertexLabel.METHOD_RETURN.name}_VERT")
//            val methodRaw = get("graph/$GRAPH_NAME/vertices/${VertexLabel.METHOD.name}_VERT")
//            val fileRaw = get("graph/$GRAPH_NAME/vertices/${VertexLabel.FILE.name}_VERT")
//            assertTrue(methodReturnRaw.any())
//            assertTrue(methodRaw.any())
//            assertTrue(fileRaw.any())
//            val methodReturn = VertexMapper.mapToVertex(tigerGraphHook.flattenVertexResult(methodReturnRaw.first() as JSONObject))
//            val method = VertexMapper.mapToVertex(tigerGraphHook.flattenVertexResult(methodRaw.first() as JSONObject))
//            val file = VertexMapper.mapToVertex(tigerGraphHook.flattenVertexResult(fileRaw.first() as JSONObject))
//            assertTrue(methodReturn is MethodReturnVertex)
//            assertTrue(method is MethodVertex)
//            assertTrue(file is FileVertex)
//            val methodEdges = get("graph/$GRAPH_NAME/edges/${VertexLabel.METHOD.name}_VERT/${method.hashCode()}")
//            val fileEdges = get("graph/$GRAPH_NAME/edges/${VertexLabel.FILE.name}_VERT/${file.hashCode()}")
//            assertTrue(methodEdges.any())
//            assertTrue(fileEdges.any())
//            assertEquals(method.hashCode().toString(), (methodEdges.first() as JSONObject)["from_id"])
//            assertEquals(methodReturn.hashCode().toString(), (methodEdges.first() as JSONObject)["to_id"])
//            assertEquals(file.hashCode().toString(), (fileEdges.first() as JSONObject)["from_id"])
//            assertEquals(method.hashCode().toString(), (fileEdges.first() as JSONObject)["to_id"])
//        }


    companion object {
        private const val DEFAULT_HOSTNAME = "127.0.0.1"
        private const val DEFAULT_PORT = 9000
        private const val GRAPH_NAME = "cpg"
    }
}