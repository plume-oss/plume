package io.github.plume.oss.drivers

import io.github.plume.oss.domain.enums.EdgeLabel
import io.github.plume.oss.domain.exceptions.PlumeSchemaViolationException
import io.github.plume.oss.domain.exceptions.PlumeTransactionException
import io.github.plume.oss.domain.mappers.VertexMapper
import io.github.plume.oss.domain.mappers.VertexMapper.checkSchemaConstraints
import io.github.plume.oss.domain.models.PlumeGraph
import io.github.plume.oss.util.PlumeKeyProvider
import io.shiftleft.codepropertygraph.generated.nodes.NewMetaDataBuilder
import io.shiftleft.codepropertygraph.generated.nodes.NewNodeBuilder
import io.shiftleft.codepropertygraph.generated.nodes.NewUnknownBuilder
import org.apache.logging.log4j.LogManager
import org.apache.tinkerpop.shaded.jackson.databind.ObjectMapper
import org.json.JSONArray
import org.json.JSONObject
import scala.jdk.CollectionConverters
import java.io.IOException
import java.lang.Thread.sleep
import java.util.*
import kotlin.collections.HashMap

/**
 * The driver used to connect to a remote TigerGraph instance.
 */
class TigerGraphDriver : IOverridenIdDriver {

    private val logger = LogManager.getLogger(TigerGraphDriver::class.java)
    private val objectMapper = ObjectMapper()
    private var api: String

    /**
     * The TigerGraph REST++ server hostname.
     * @see DEFAULT_HOSTNAME
     */
    var hostname: String = DEFAULT_HOSTNAME
        private set

    /**
     * The TigerGraph REST++ server port number.
     * @see DEFAULT_PORT
     */
    var port: Int = DEFAULT_PORT
        private set

    /**
     * The TigerGraph REST++ server security status. Used for determining the protocol used between HTTPS or HTTP.
     */
    var secure: Boolean = false
        private set

    /**
     * The authorization key used for TigerGraph servers with token authorization turned on. This is placed under
     * the Authorization header when making requests.
     */
    var authKey: String? = null
        private set

    init {
        api = "http://$hostname:$port"
    }

    /**
     * Recreates the API variable based on saved configurations.
     */
    private fun setApi() = run { api = "http${if (secure) "s" else ""}://$hostname:$port" }

    /**
     * Set the hostname for the TigerGraph REST++ server.
     *
     * @param value the hostname e.g. 127.0.0.1, www.tgserver.com, etc.
     */
    fun hostname(value: String): TigerGraphDriver = apply { hostname = value; setApi() }

    /**
     * Set the port for the TigerGraph REST++ server.
     *
     * @param value the port number e.g. 9000
     */
    fun port(value: Int): TigerGraphDriver = apply { port = value; setApi() }

    /**
     * Sets the secure flag when building the API path.
     *
     * @param value set to true if using HTTPS and false if using HTTP.
     */
    fun secure(value: Boolean): TigerGraphDriver = apply { secure = value; setApi() }

    /**
     * Sets the authorization token used in requests.
     *
     * An example of where this is used is in
     * [TigerGraph Cloud](https://docs-beta.tigergraph.com/cloud/tigergraph-cloud-faqs).
     */
    fun authKey(value: String): TigerGraphDriver = apply { authKey = value }

    override fun addVertex(v: NewNodeBuilder) {
        val payload = mutableMapOf<String, Any>(
            "vertices" to createVertexPayload(v)
        )
        post("graph/$GRAPH_NAME", payload)
    }

    override fun exists(v: NewNodeBuilder): Boolean {
        val route = when (v) {
            is NewMetaDataBuilder -> "graph/$GRAPH_NAME/vertices/META_DATA_VERT"
            else -> "graph/$GRAPH_NAME/vertices/CPG_VERT"
        }
        return try {
            get("$route/${v.id()}")
            true
        } catch (e: PlumeTransactionException) {
            false
        }
    }

    override fun exists(fromV: NewNodeBuilder, toV: NewNodeBuilder, edge: EdgeLabel): Boolean {
        // No edge can be connected to a MetaDataVertex
        if (fromV is NewMetaDataBuilder || toV is NewMetaDataBuilder) return false
        return try {
            val response = get(
                "query/$GRAPH_NAME/areVerticesJoinedByEdge",
                mapOf(
                    "V_FROM" to fromV.id().toString(),
                    "V_TO" to toV.id().toString(),
                    "EDGE_LABEL" to edge.name
                )
            ).firstOrNull()
            return if (response == null) {
                throw PlumeTransactionException("Null response for exists query between $fromV and $toV with edge label $edge")
            } else {
                (response as JSONObject)["result"] as Boolean
            }
        } catch (e: PlumeTransactionException) {
            if (e.message?.contains("Failed to convert user vertex id") == true) {
                false // Then one of the vertices does not exist, so simply return false
            } else {
                logger.error(e.message)
                throw e
            }
        }
    }

    override fun addEdge(fromV: NewNodeBuilder, toV: NewNodeBuilder, edge: EdgeLabel) {
        if (!checkSchemaConstraints(fromV, toV, edge)) throw PlumeSchemaViolationException(fromV, toV, edge)
        if (exists(fromV, toV, edge)) return
        val fromPayload = createVertexPayload(fromV)
        val toPayload = createVertexPayload(toV)
        val vertexPayload = if (fromPayload.keys.first() == toPayload.keys.first()) mapOf(
            fromPayload.keys.first() to mapOf(
                fromV.id().toString() to (fromPayload.values.first() as Map<*, *>)[fromV.id().toString()],
                toV.id().toString() to (toPayload.values.first() as Map<*, *>)[toV.id().toString()]
            )
        )
        else mapOf(
            fromPayload.keys.first() to fromPayload.values.first(),
            toPayload.keys.first() to toPayload.values.first()
        )
        val payload = mutableMapOf(
            "vertices" to vertexPayload,
            "edges" to createEdgePayload(fromV, toV, edge)
        )
        post("graph/$GRAPH_NAME", payload)
    }

    override fun maxOrder() = (get("query/$GRAPH_NAME/maxOrder").first() as JSONObject)["@@maxAstOrder"] as Int

    private fun createVertexPayload(v: NewNodeBuilder): Map<String, Any> {
        val propertyMap = CollectionConverters.MapHasAsJava(v.build().properties()).asJava().toMutableMap()
        val vertexType = if (v is NewMetaDataBuilder) "META_DATA_VERT" else "CPG_VERT"
        propertyMap["label"] = v.build().label()
        if (v.id() < 0L) v.id(PlumeKeyProvider.getNewId(this))
        return mapOf(
            vertexType to mapOf<String, Any>(
                v.id().toString() to extractAttributesFromMap(propertyMap)
            )
        )
    }

    private fun extractAttributesFromMap(propertyMap: MutableMap<String, Any>): MutableMap<String, Any> {
        val attributes = mutableMapOf<String, Any>()
        propertyMap.computeIfPresent("DYNAMIC_TYPE_HINT_FULL_NAME") { _, value ->
            when (value) {
                is scala.collection.immutable.`$colon$colon`<*> -> value.head()
                else -> value
            }
        }
        propertyMap.forEach {
            val key: Optional<String> = when (it.key) {
                "ORDER" -> Optional.of("AST_ORDER")
                "PARSER_TYPE_NAME" -> Optional.empty()
                "AST_PARENT_TYPE" -> Optional.empty()
                "AST_PARENT_FULL_NAME" -> Optional.empty()
                "FILENAME" -> Optional.empty()
                "IS_EXTERNAL" -> Optional.empty()
                else -> Optional.of(it.key)
            }
            if (key.isPresent) attributes[key.get()] = mapOf("value" to it.value)
        }
        return attributes
    }

    private fun createEdgePayload(from: NewNodeBuilder, to: NewNodeBuilder, edge: EdgeLabel): Map<String, Any> {
        val fromPayload = createVertexPayload(from)
        val toPayload = createVertexPayload(to)
        val fromLabel = fromPayload.keys.first()
        val toLabel = toPayload.keys.first()
        return mapOf(
            fromLabel to mapOf(
                from.id().toString() to mapOf<String, Any>(
                    edge.name to mapOf<String, Any>(
                        toLabel to mapOf<String, Any>(
                            to.id().toString() to emptyMap<String, Any>()
                        )
                    )

                )
            )
        )
    }

    override fun getWholeGraph(): PlumeGraph {
        val result = get("query/$GRAPH_NAME/showAll")
        return graphPayloadToPlumeGraph(result)
    }

    override fun getMethod(fullName: String, signature: String, includeBody: Boolean): PlumeGraph {
        val path = if (!includeBody) "getMethodHead" else "getMethod"
        return try {
            val result = get("query/$GRAPH_NAME/$path", mapOf("FULL_NAME" to fullName, "SIGNATURE" to signature))
            graphPayloadToPlumeGraph(result)
        } catch (e: PlumeTransactionException) {
            logger.warn("${e.message}. This may be a result of the method not being present in the graph.")
            PlumeGraph()
        }
    }

    override fun getProgramStructure(): PlumeGraph {
        val result = get("query/$GRAPH_NAME/getProgramStructure")
        return graphPayloadToPlumeGraph(result)
    }

    override fun getNeighbours(v: NewNodeBuilder): PlumeGraph {
        if (v is NewMetaDataBuilder) return PlumeGraph().apply { addVertex(v) }
        val result = get("query/$GRAPH_NAME/getNeighbours", mapOf("SOURCE" to v.id().toString()))
        return graphPayloadToPlumeGraph(result)
    }

    override fun deleteVertex(v: NewNodeBuilder) {
        val label = if (v is NewMetaDataBuilder) "META_DATA_VERT" else "CPG_VERT"
        delete("graph/$GRAPH_NAME/vertices/$label/${v.id()}")
    }

    override fun deleteMethod(fullName: String, signature: String) {
        try {
            get("query/$GRAPH_NAME/deleteMethod", mapOf("FULL_NAME" to fullName, "SIGNATURE" to signature))
        } catch (e: PlumeTransactionException) {
            logger.warn("${e.message}. This may be a result of the method not being present in the graph.")
        }
    }

    override fun getVertexIds(lowerBound: Long, upperBound: Long): Set<Long> {
        val result = (get(
            endpoint = "query/$GRAPH_NAME/getVertexIds",
            params = mapOf("LOWER_BOUND" to lowerBound.toString(), "UPPER_BOUND" to upperBound.toString())
        ).first() as JSONObject)["@@ids"] as JSONArray
        return result.map { (it as Int).toLong() }.toSet()
    }

    private fun graphPayloadToPlumeGraph(a: JSONArray): PlumeGraph {
        val plumeGraph = PlumeGraph()
        a[0]?.let { res ->
            val o = res as JSONObject
            val vertices = o["allVert"] as JSONArray
            vertices.map { vertexPayloadToPlumeGraph(it as JSONObject) }.forEach { plumeGraph.addVertex(it) }
        }
        a[1]?.let { res ->
            val o = res as JSONObject
            val edges = o["@@edges"] as JSONArray
            edges.forEach { connectEdgeResult(plumeGraph, it as JSONObject) }
        }
        return plumeGraph
    }

    private fun connectEdgeResult(plumeGraph: PlumeGraph, edgePayload: JSONObject) {
        val fromV = plumeGraph.vertices().find { it.id() == edgePayload["from_id"].toString().toLong() }
        val toV = plumeGraph.vertices().find { it.id() == edgePayload["to_id"].toString().toLong() }
        val edge = EdgeLabel.valueOf(edgePayload["e_type"].toString())
        if (fromV != null && toV != null) {
            plumeGraph.addEdge(fromV, toV, edge)
        }
    }

    private fun vertexPayloadToPlumeGraph(o: JSONObject): NewNodeBuilder {
        val attributes = o["attributes"] as JSONObject
        val vertexMap = HashMap<String, Any>()
        attributes.keySet().filter { attributes[it] != "" }
            .map { Pair(if (it == "AST_ORDER") "ORDER" else it, attributes[it]) }
            .forEach { vertexMap[it.first] = it.second }
        return VertexMapper.mapToVertex(vertexMap)
    }

    override fun close() {
        /* No need to close anything - this hook uses REST */
    }

    override fun clearGraph() = apply {
        delete("graph/$GRAPH_NAME/delete_by_type/vertices/META_DATA_VERT")
        delete("graph/$GRAPH_NAME/delete_by_type/vertices/CPG_VERT")
        PlumeKeyProvider.clearKeyPools()
    }

    private fun headers(): Map<String, String> = if (authKey == null) {
        mapOf("Content-Type" to "application/json")
    } else {
        mapOf("Content-Type" to "application/json", "Authorization" to "Bearer $authKey")
    }

    /**
     * Sends a GET request to the configured REST++ server.
     *
     * @param endpoint The endpoint to send the request to. This is appended after [api].
     * @return the value for the "results" key of the REST++ response as a [JSONArray]
     * @throws PlumeTransactionException if the REST++ response returned a query related error.
     * @throws IOException if the request could not be sent based on an I/O communication failure.
     */
    @Throws(PlumeTransactionException::class)
    private fun get(endpoint: String): JSONArray {
        return get(endpoint, emptyMap())
    }

    /**
     * Sends a GET request to the configured REST++ server.
     *
     * @param endpoint The endpoint to send the request to. This is appended after [api].
     * @param params Optional GET parameters.
     * @return the value for the "results" key of the REST++ response as a [JSONArray]
     * @throws PlumeTransactionException if the REST++ response returned a query related error.
     * @throws IOException if the request could not be sent based on an I/O communication failure.
     */
    @Throws(PlumeTransactionException::class)
    private fun get(endpoint: String, params: Map<String, String>): JSONArray {
        var tryCount = 0
        val response = if (params.isEmpty()) khttp.get(
            url = "${api}/$endpoint",
            headers = headers()
        ) else khttp.get(
            url = "${api}/$endpoint",
            headers = headers(),
            params = params
        )
        while (++tryCount < MAX_RETRY) {
            logger.debug("Get ${response.url}")
            logger.debug("Response ${response.text}")
            when {
                response.statusCode == 200 -> if (response.jsonObject["error"] as Boolean) throw PlumeTransactionException(
                    response.jsonObject["message"] as String
                )
                else return response.jsonObject["results"] as JSONArray
                tryCount >= MAX_RETRY -> throw IOException("Could not complete get request due to status code ${response.statusCode} at $api/$endpoint")
                else -> sleep(500)
            }
        }
        return JSONArray()
    }

    /**
     * Sends a POST request to the configured REST++ server.
     *
     * @param endpoint The endpoint to send the request to. This is appended after [api].
     * @param payload the JSON payload to send with the POST request.
     * @throws PlumeTransactionException if the REST++ response returned a query related error.
     * @throws IOException if the request could not be sent based on an I/O communication failure.
     */
    @Throws(PlumeTransactionException::class)
    private fun post(endpoint: String, payload: Map<String, Any>) {
        var tryCount = 0
        while (++tryCount < MAX_RETRY) {
            val response = khttp.post(
                url = "$api/$endpoint",
                headers = headers(),
                data = objectMapper.writeValueAsString(payload)
            )
            logger.debug("Post ${response.url} ${response.request.data}")
            logger.debug("Response ${response.text}")
            when {
                response.statusCode == 200 -> if (response.jsonObject["error"] as Boolean) throw PlumeTransactionException(
                    response.jsonObject["message"] as String
                )
                else return
                tryCount >= MAX_RETRY -> throw IOException("Could not complete post request due to status code ${response.statusCode} at $api/$endpoint")
                else -> sleep(500)
            }
        }
    }

    /**
     * Sends a DELETE request to the configured REST++ server.
     *
     * @param endpoint The endpoint to send the request to. This is appended after [api].
     * @throws PlumeTransactionException if the REST++ response returned a query related error.
     * @throws IOException if the request could not be sent based on an I/O communication failure.
     */
    private fun delete(endpoint: String) {
        var tryCount = 0
        while (++tryCount < MAX_RETRY) {
            val response = khttp.delete(url = "$api/$endpoint", headers = headers())
            logger.debug("Delete ${response.url}")
            logger.debug("Response ${response.text}")
            when {
                response.statusCode == 200 -> return
                tryCount >= MAX_RETRY -> throw IOException("Could not complete delete request due to status code ${response.statusCode} at $api/$endpoint")
                else -> sleep(500)
            }
        }
    }

    private fun removeUnsupportedKeys(v: NewNodeBuilder) {
        val propertyMap = CollectionConverters.MapHasAsJava(v.build().properties()).asJava().toMutableMap()
        propertyMap.computeIfPresent("DYNAMIC_TYPE_HINT_FULL_NAME") { _, value ->
            when (value) {
                is scala.collection.immutable.`$colon$colon`<*> -> value.head()
                else -> value
            }
        }
    }

    companion object {
        /**
         * Default hostname for a locally running TigerGraph server with a value of 127.0.0.1.
         */
        private const val DEFAULT_HOSTNAME = "127.0.0.1"

        /**
         * Default port number for a locally running TigerGraph server with a value of 9000.
         */
        private const val DEFAULT_PORT = 9000

        /**
         * Default graph name for a TigerGraph server a with value of cpg.
         */
        private const val GRAPH_NAME = "cpg"

        /**
         * Default number of request retries for a TigerGraph server with a value of 5.
         */
        private const val MAX_RETRY = 5
    }
}