package za.ac.sun.plume.drivers

import org.apache.logging.log4j.LogManager
import org.apache.tinkerpop.shaded.jackson.databind.ObjectMapper
import org.json.JSONArray
import org.json.JSONObject
import za.ac.sun.plume.domain.enums.EdgeLabel
import za.ac.sun.plume.domain.enums.VertexLabel
import za.ac.sun.plume.domain.mappers.VertexMapper
import za.ac.sun.plume.domain.models.PlumeVertex
import java.io.IOException
import java.lang.Thread.sleep
import java.util.*

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
        api = "http://$hostname:$port/"
    }

    /**
     * Recreates the API variable based on saved configurations.
     */
    private fun setApi() = run { api = "http${if (secure) "s" else ""}://$hostname:$port/" }

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
        val vLabel = v.javaClass.getDeclaredField("LABEL").get(v).toString()
        println(get("graph/$GRAPH_NAME/vertices/$vLabel/${v.hashCode()}"))
        return true
    }

    override fun exists(fromV: PlumeVertex, toV: PlumeVertex, edge: EdgeLabel): Boolean {
        TODO("Not yet implemented")
    }

    override fun addEdge(fromV: PlumeVertex, toV: PlumeVertex, edge: EdgeLabel) {
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
        val vertexLabel = propertyMap.remove("label")
        return mapOf("${vertexLabel}_VERT" to mapOf<String, Any>(
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

    override fun close() {
        /* No need to close anything - this hook uses REST */
    }

    override fun clearGraph() = apply {
        EnumSet.allOf(VertexLabel::class.java).forEach {
            delete("graph/$GRAPH_NAME/delete_by_type/vertices/${it.name}_VERT")
        }
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
                    url = "${api}/$endpoint",
                    headers = headers()
            )
            logger.debug("Get ${response.url}")
            logger.debug("Response ${response.text}")
            when {
                response.statusCode == 200 -> return response.jsonObject["results"] as JSONArray
                tryCount >= MAX_RETRY -> throw IOException("Could not complete get request due to status code ${response.statusCode} at $api/$endpoint")
                else -> sleep(500)
            }
        }
        return JSONArray()
    }

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
                response.statusCode == 200 -> return
                tryCount >= MAX_RETRY -> throw IOException("Could not complete post request due to status code ${response.statusCode} at $api/$endpoint")
                else -> sleep(500)
            }
        }
    }

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