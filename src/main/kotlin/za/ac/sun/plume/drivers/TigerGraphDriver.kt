package za.ac.sun.plume.drivers

import org.apache.logging.log4j.LogManager
import org.apache.tinkerpop.shaded.jackson.databind.ObjectMapper
import org.json.JSONArray
import org.json.JSONObject
import za.ac.sun.plume.domain.enums.EdgeLabel
import za.ac.sun.plume.domain.exceptions.PlumeSchemaViolationException
import za.ac.sun.plume.domain.exceptions.PlumeTransactionException
import za.ac.sun.plume.domain.mappers.VertexMapper
import za.ac.sun.plume.domain.mappers.VertexMapper.Companion.checkSchemaConstraints
import za.ac.sun.plume.domain.models.PlumeVertex
import za.ac.sun.plume.domain.models.vertices.MetaDataVertex
import java.io.IOException
import java.lang.Thread.sleep

/**
 * The driver used to connect to a remote TigerGraph instance.
 */
class TigerGraphDriver : IDriver {

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

    override fun addVertex(v: PlumeVertex) {
        val payload = mutableMapOf<String, Any>(
                "vertices" to createVertexPayload(v)
        )
        post("graph/$GRAPH_NAME", payload)
    }

    override fun exists(v: PlumeVertex): Boolean {
        val route = when (v) {
            is MetaDataVertex -> "graph/$GRAPH_NAME/vertices/META_DATA_VERT"
            else -> "graph/$GRAPH_NAME/vertices/CPG_VERT"
        }
        return try {
            get("$route/${v.hashCode()}")
            true
        } catch (e: PlumeTransactionException) {
            false
        }
    }

    override fun exists(fromV: PlumeVertex, toV: PlumeVertex, edge: EdgeLabel): Boolean {
        // No edge can be connected to a MetaDataVertex
        if (fromV is MetaDataVertex || toV is MetaDataVertex) return false
        return try {
            val response = get("query/$GRAPH_NAME/areVerticesJoinedByEdge?" +
                    "vFrom=${fromV.hashCode()}&vFrom.type=CPG_VERT&" +
                    "vTo=${toV.hashCode()}&vTo.type=CPG_VERT&" +
                    "edgeLabel=${edge.name}").firstOrNull() as JSONObject
            return response["result"] as Boolean
        } catch (e: PlumeTransactionException) {
            false
        }
    }

    override fun addEdge(fromV: PlumeVertex, toV: PlumeVertex, edge: EdgeLabel) {
        if (!checkSchemaConstraints(fromV, toV, edge)) throw PlumeSchemaViolationException(fromV, toV, edge)
        if (exists(fromV, toV, edge)) return
        val fromPayload = createVertexPayload(fromV)
        val toPayload = createVertexPayload(toV)
        val vertexPayload = if (fromPayload.keys.first() == toPayload.keys.first()) mapOf(
                fromPayload.keys.first() to mapOf(
                        fromV.hashCode().toString() to (fromPayload.values.first() as Map<*, *>)[fromV.hashCode().toString()],
                        toV.hashCode().toString() to (toPayload.values.first() as Map<*, *>)[toV.hashCode().toString()]
                ))
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

    private fun createVertexPayload(plumeVertex: PlumeVertex): Map<String, Any> {
        val propertyMap = VertexMapper.vertexToMap(plumeVertex)
        val vertexType = if (plumeVertex is MetaDataVertex) "META_DATA_VERT" else "CPG_VERT"
        return mapOf(vertexType to mapOf<String, Any>(
                plumeVertex.hashCode().toString() to extractAttributesFromMap(propertyMap)
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
                                edge.name to mapOf<String, Any>(
                                        toLabel to mapOf<String, Any>(
                                                to.hashCode().toString() to emptyMap<String, Any>()
                                        )
                                )

                        )
                )
        )
    }

    override fun close() {
        /* No need to close anything - this hook uses REST */
    }

    override fun clearGraph() = apply {
        delete("graph/$GRAPH_NAME/delete_by_type/vertices/META_DATA_VERT")
        delete("graph/$GRAPH_NAME/delete_by_type/vertices/CPG_VERT")
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
        var tryCount = 0
        while (++tryCount < MAX_RETRY) {
            val response = khttp.get(
                    url = "${api}/$endpoint",
                    headers = headers()
            )
            logger.debug("Get ${response.url}")
            logger.debug("Response ${response.text}")
            when {
                response.statusCode == 200 -> if (response.jsonObject["error"] as Boolean) throw PlumeTransactionException(response.jsonObject["message"] as String)
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
                response.statusCode == 200 -> if (response.jsonObject["error"] as Boolean) throw PlumeTransactionException(response.jsonObject["message"] as String)
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