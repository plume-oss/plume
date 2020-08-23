package za.ac.sun.plume.drivers

import org.apache.logging.log4j.LogManager
import org.apache.tinkerpop.shaded.jackson.databind.ObjectMapper
import org.json.JSONArray
import org.json.JSONObject
import za.ac.sun.plume.domain.enums.EdgeLabel
import za.ac.sun.plume.domain.enums.VertexLabel
import za.ac.sun.plume.domain.mappers.VertexMapper
import za.ac.sun.plume.domain.models.PlumeVertex
import za.ac.sun.plume.domain.models.vertices.FileVertex
import za.ac.sun.plume.domain.models.vertices.MethodVertex
import za.ac.sun.plume.domain.models.vertices.ModifierVertex
import za.ac.sun.plume.domain.models.vertices.NamespaceBlockVertex
import java.io.IOException
import java.lang.Thread.sleep
import java.util.*


class TigerGraphDriver : IDriver {

    private val logger = LogManager.getLogger(TigerGraphDriver::class.java)
    private val objectMapper = ObjectMapper()

    var hostname: String = DEFAULT_HOSTNAME
    var port: Int = DEFAULT_PORT
    var secure: Boolean = false
    var authKey: String? = null

    private fun api() = "http${if (secure) "s" else ""}://$hostname:$port"

    override fun joinFileVertexTo(to: FileVertex, from: NamespaceBlockVertex) = upsertAndJoinVertices(from, to, EdgeLabel.AST)

    override fun joinFileVertexTo(from: FileVertex, to: MethodVertex) = upsertAndJoinVertices(from, to, EdgeLabel.AST)

    override fun createAndAddToMethod(from: MethodVertex, to: MethodDescriptorVertex) = upsertAndJoinVertices(from, to, EdgeLabel.AST)

    override fun createAndAddToMethod(from: MethodVertex, to: ModifierVertex) = upsertAndJoinVertices(from, to, EdgeLabel.AST)

    override fun joinASTVerticesByOrder(blockFrom: Int, blockTo: Int, edgeLabel: EdgeLabel) {
        val from = getVertexByASTOrder(blockFrom)
        val to = getVertexByASTOrder(blockTo)
        if (from != null && to != null) upsertAndJoinVertices(from, to, EdgeLabel.AST)
    }

    override fun areASTVerticesConnected(orderFrom: Int, orderTo: Int, edgeLabel: EdgeLabel): Boolean = ((get("query/$GRAPH_NAME/areASTVerticesJoinedByEdge?blockFrom=$orderFrom&blockTo=$orderTo&edgeLabel=${edgeLabel.name}")).first() as JSONObject)["result"] as Boolean

    override fun joinNamespaceBlocks(from: NamespaceBlockVertex, to: NamespaceBlockVertex) = upsertAndJoinVertices(from, to, EdgeLabel.AST)

    override fun maxOrder() = (get("query/$GRAPH_NAME/maxOrder").first() as JSONObject)["@@maxAstOrder"] as Int

    override fun updateASTVertexProperty(order: Int, key: String, value: String) {
        val result = (get("query/$GRAPH_NAME/findVertexByAstOrder?astOrder=$order").first() as JSONObject)["result"] as JSONArray
        if (result.length() > 0) {
            val vertexMap = result.first() as JSONObject
            val flatMap = flattenVertexResult(vertexMap)
            // Update key-value pair and reconstruct as PlumeVertex
            flatMap[key] = value
            val vertex = VertexMapper.mapToVertex(flatMap)
            // Upsert vertex
            createVertex(vertex, vertexMap["v_id"] as String)
        }
    }

    override fun isASTVertex(blockOrder: Int): Boolean = ((get("query/$GRAPH_NAME/isASTVertex?astOrder=$blockOrder")).first() as JSONObject)["result"] as Boolean

    override fun createAndAssignToBlock(parentVertex: MethodVertex, newVertex: PlumeVertex) = upsertAndJoinVertices(parentVertex, newVertex, EdgeLabel.AST)

    override fun createAndAssignToBlock(newVertex: PlumeVertex, blockOrder: Int) {
        val from = getVertexByASTOrder(blockOrder) ?: return
        upsertAndJoinVertices(from, newVertex, EdgeLabel.AST)
    }

    override fun createVertex(plumeVertex: PlumeVertex) {
        val payload = mutableMapOf<String, Any>(
                "vertices" to createVertexPayload(plumeVertex)
        )
        post("graph/$GRAPH_NAME", payload)
    }

    private fun createVertex(plumeVertex: PlumeVertex, id: String) {
        val payload = mutableMapOf<String, Any>(
                "vertices" to createVertexPayload(plumeVertex, id)
        )
        post("graph/$GRAPH_NAME", payload)
    }

    private fun upsertAndJoinVertices(from: PlumeVertex, to: PlumeVertex, edgeLabel: EdgeLabel) {
        val fromPayload = createVertexPayload(from)
        val toPayload = createVertexPayload(to)
        val vertexPayload = if (fromPayload.keys.first() == toPayload.keys.first()) mapOf(
                fromPayload.keys.first() to mapOf(
                        from.hashCode().toString() to (fromPayload.values.first() as Map<*, *>)[from.hashCode().toString()],
                        to.hashCode().toString() to (toPayload.values.first() as Map<*, *>)[to.hashCode().toString()]
                ))
        else mapOf(
                fromPayload.keys.first() to fromPayload.values.first(),
                toPayload.keys.first() to toPayload.values.first()
        )
        val payload = mutableMapOf(
                "vertices" to vertexPayload,
                "edges" to createEdgePayload(from, to, edgeLabel)
        )
        post("graph/$GRAPH_NAME", payload)
    }

    private fun createVertexPayload(plumeVertex: PlumeVertex): Map<String, Any> {
        val propertyMap = VertexMapper.propertiesToMap(plumeVertex)
        val vertexLabel = propertyMap.remove("label")
        return mapOf("${vertexLabel}_VERT" to mapOf<String, Any>(
                plumeVertex.hashCode().toString() to extractAttributesFromMap(propertyMap)
        ))
    }

    private fun createVertexPayload(plumeVertex: PlumeVertex, id: String): Map<String, Any> {
        val propertyMap = VertexMapper.propertiesToMap(plumeVertex)
        val vertexLabel = propertyMap.remove("label")
        return mapOf("${vertexLabel}_VERT" to mapOf<String, Any>(
                id to extractAttributesFromMap(propertyMap)
        ))
    }

    private fun extractAttributesFromMap(propertyMap: MutableMap<String, Any>): MutableMap<String, Any> {
        val attributes = mutableMapOf<String, Any>()
        propertyMap.forEach {
            val key = if (it.key == "order") "astOrder" else it.key
            attributes[key] = mapOf("value" to it.value)
        }
        return attributes
    }


    private fun createEdgePayload(from: PlumeVertex, to: PlumeVertex, edge: EdgeLabel): Map<String, Any> {
        val fromPayload = createVertexPayload(from)
        val toPayload = createVertexPayload(to)
        val fromLabel = fromPayload.keys.first()
        val toLabel = toPayload.keys.first()
        return mapOf(
                fromLabel to mapOf(
                        from.hashCode().toString() to mapOf<String, Any>(
                                "${fromLabel.removeSuffix("_VERT")}_${toLabel.removeSuffix("_VERT")}" to mapOf<String, Any>(
                                        toLabel to mapOf<String, Any>(
                                                to.hashCode().toString() to mapOf<String, Any>(
                                                        "name" to mapOf("value" to edge.name)
                                                )
                                        )
                                )

                        )
                )
        )
    }

    fun flattenVertexResult(o: JSONObject): MutableMap<String, Any> {
        val map = mutableMapOf<String, Any>()
        val attributes = o["attributes"] as JSONObject
        map["label"] = (o["v_type"] as String).removeSuffix("_VERT")
        attributes.keys().forEach { k ->
            when (k.toString()) {
                "astOrder" -> map["order"] = attributes[k] as Int
                else -> map[k] = attributes[k]
            }
        }
        return map
    }

    private fun getVertexByASTOrder(order: Int): PlumeVertex? {
        val result = (get("query/$GRAPH_NAME/findVertexByAstOrder?astOrder=$order").first() as JSONObject)["result"] as JSONArray
        if (result.length() > 0) {
            val vertexMap = result.first() as JSONObject
            val flatMap = flattenVertexResult(vertexMap)
            // Update key-value pair and reconstruct as PlumeVertex
            return VertexMapper.mapToVertex(flatMap)
        }
        return null
    }

    override fun close() {
        /* No need to close anything - this hook uses REST */
    }

    override fun clearGraph() =
            EnumSet.allOf(VertexLabel::class.java).forEach {
                delete("graph/$GRAPH_NAME/delete_by_type/vertices/${it.name}_VERT")
            }

    private fun headers(): Map<String, String> = if (authKey == null) {
        mapOf("Content-Type" to "application/json")
    } else {
        mapOf("Content-Type" to "application/json", "Authorization" to "Bearer $authKey")
    }

    private fun get(endpoint: String): JSONArray {
        var tryCount = 0
        while (++tryCount < MAX_RETRY) {
            val response = khttp.get(
                    url = "${api()}/$endpoint",
                    headers = headers()
            )
            logger.debug("Get ${response.url}")
            logger.debug("Response ${response.text}")
            when {
                response.statusCode == 200 -> return response.jsonObject["results"] as JSONArray
                tryCount >= MAX_RETRY -> throw IOException("Could not complete get request due to status code ${response.statusCode} at ${api()}/$endpoint")
                else -> sleep(500)
            }
        }
        return JSONArray()
    }

    private fun post(endpoint: String, payload: Map<String, Any>) {
        var tryCount = 0

        while (++tryCount < MAX_RETRY) {
            val response = khttp.post(
                    url = "${api()}/$endpoint",
                    headers = headers(),
                    data = objectMapper.writeValueAsString(payload)
            )
            logger.debug("Post ${response.url} ${response.request.data}")
            logger.debug("Response ${response.text}")
            when {
                response.statusCode == 200 -> return
                tryCount >= MAX_RETRY -> throw IOException("Could not complete post request due to status code ${response.statusCode} at ${api()}/$endpoint")
                else -> sleep(500)
            }
        }
    }

    private fun delete(endpoint: String) {
        var tryCount = 0
        while (++tryCount < MAX_RETRY) {
            val response = khttp.delete(url = "${api()}/$endpoint", headers = headers())
            logger.debug("Delete ${response.url}")
            logger.debug("Response ${response.text}")
            when {
                response.statusCode == 200 -> return
                tryCount >= MAX_RETRY -> throw IOException("Could not complete delete request due to status code ${response.statusCode} at ${api()}/$endpoint")
                else -> sleep(500)
            }
        }
    }

    companion object {
        private const val DEFAULT_HOSTNAME = "127.0.0.1"
        private const val DEFAULT_PORT = 9000
        private const val GRAPH_NAME = "cpg"
        private const val MAX_RETRY = 5
    }
}