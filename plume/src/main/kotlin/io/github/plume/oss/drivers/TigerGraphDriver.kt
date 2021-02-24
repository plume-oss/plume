package io.github.plume.oss.drivers

import io.github.plume.oss.domain.exceptions.PlumeSchemaViolationException
import io.github.plume.oss.domain.exceptions.PlumeTransactionException
import io.github.plume.oss.domain.mappers.VertexMapper
import io.github.plume.oss.domain.mappers.VertexMapper.checkSchemaConstraints
import io.github.plume.oss.util.CodeControl
import io.github.plume.oss.util.ExtractorConst
import io.github.plume.oss.util.PlumeKeyProvider
import io.shiftleft.codepropertygraph.generated.EdgeTypes
import io.shiftleft.codepropertygraph.generated.NodeKeyNames.*
import io.shiftleft.codepropertygraph.generated.NodeKeys
import io.shiftleft.codepropertygraph.generated.NodeTypes.META_DATA
import io.shiftleft.codepropertygraph.generated.NodeTypes.UNKNOWN
import io.shiftleft.codepropertygraph.generated.nodes.NewMetaDataBuilder
import io.shiftleft.codepropertygraph.generated.nodes.NewNodeBuilder
import org.apache.logging.log4j.LogManager
import org.apache.tinkerpop.shaded.jackson.databind.ObjectMapper
import org.json.JSONArray
import org.json.JSONObject
import overflowdb.Config
import overflowdb.Graph
import overflowdb.Node
import overflowdb.PropertyKey
import scala.jdk.CollectionConverters
import java.io.*
import java.lang.Thread.sleep
import io.shiftleft.codepropertygraph.generated.edges.Factories as EdgeFactories
import io.shiftleft.codepropertygraph.generated.nodes.Factories as NodeFactories

/**
 * The driver used to connect to a remote TigerGraph instance.
 */
class TigerGraphDriver : IOverridenIdDriver, ISchemaSafeDriver {

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
     * The TigerGraph GSQL server password.
     * @see DEFAULT_USERNAME
     */
    var username: String = DEFAULT_USERNAME
        private set

    /**
     * The TigerGraph GSQL server username.
     * @see DEFAULT_PASSWORD
     */
    var password: String = DEFAULT_PASSWORD
        private set

    /**
     * The TigerGraph REST++ server port number.
     * @see DEFAULT_RESTPP_PORT
     */
    var restPpPort: Int = DEFAULT_RESTPP_PORT
        private set

    /**
     * The TigerGraph GSQL server port number.
     * @see DEFAULT_GSQL_PORT
     */
    var gsqlPort: Int = DEFAULT_GSQL_PORT
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
        api = "http://$hostname:$restPpPort"
    }

    /**
     * Recreates the API variable based on saved configurations.
     */
    private fun setApi() = run { api = "http${if (secure) "s" else ""}://$hostname:$restPpPort" }

    /**
     * Set the hostname for the TigerGraph REST++ server.
     *
     * @param value the hostname e.g. 127.0.0.1, www.tgserver.com, etc.
     */
    fun hostname(value: String): TigerGraphDriver = apply { hostname = value; setApi() }

    /**
     * Set the hostname for the TigerGraph GSQL server.
     *
     * @param value the username e.g. "tigergraph".
     */
    fun username(value: String): TigerGraphDriver = apply { hostname = value }

    /**
     * Set the password for the TigerGraph GSQL server.
     *
     * @param value the password e.g. "tigergraph"
     */
    fun password(value: String): TigerGraphDriver = apply { password = value }

    /**
     * Set the port for the TigerGraph REST++ server.
     *
     * @param value the port number e.g. 9000
     */
    fun restPpPort(value: Int): TigerGraphDriver = apply { restPpPort = value; setApi() }

    /**
     * Set the port for the TigerGraph GSQL server.
     *
     * @param value the port number e.g. 14240
     */
    fun gsqlPort(value: Int): TigerGraphDriver = apply { gsqlPort = value }

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

    override fun exists(v: NewNodeBuilder): Boolean = checkVertexExists(v.id(), v.build().label())

    private fun checkVertexExists(id: Long, label: String?): Boolean {
        val route = when (label) {
            META_DATA -> "graph/$GRAPH_NAME/vertices/META_DATA_VERT"
            else -> "graph/$GRAPH_NAME/vertices/CPG_VERT"
        }
        return try {
            get("$route/$id")
            true
        } catch (e: PlumeTransactionException) {
            false
        }
    }

    override fun exists(src: NewNodeBuilder, tgt: NewNodeBuilder, edge: String): Boolean {
        // No edge can be connected to a MetaDataVertex
        if (src is NewMetaDataBuilder || tgt is NewMetaDataBuilder) return false
        return try {
            val response = get(
                "query/$GRAPH_NAME/areVerticesJoinedByEdge",
                mapOf(
                    "V_FROM" to src.id().toString(),
                    "V_TO" to tgt.id().toString(),
                    "EDGE_LABEL" to "_$edge"
                )
            ).firstOrNull()
            return if (response == null) {
                throw PlumeTransactionException("Null response for exists query between $src and $tgt with edge label $edge")
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

    override fun addEdge(src: NewNodeBuilder, tgt: NewNodeBuilder, edge: String) {
        if (!checkSchemaConstraints(src, tgt, edge)) throw PlumeSchemaViolationException(src, tgt, edge)
        if (exists(src, tgt, edge)) return
        val fromPayload = createVertexPayload(src)
        val toPayload = createVertexPayload(tgt)
        val vertexPayload = if (fromPayload.keys.first() == toPayload.keys.first()) mapOf(
            fromPayload.keys.first() to mapOf(
                src.id().toString() to (fromPayload.values.first() as Map<*, *>)[src.id().toString()],
                tgt.id().toString() to (toPayload.values.first() as Map<*, *>)[tgt.id().toString()]
            )
        )
        else mapOf(
            fromPayload.keys.first() to fromPayload.values.first(),
            toPayload.keys.first() to toPayload.values.first()
        )
        val payload = mapOf(
            "vertices" to vertexPayload,
            "edges" to createEdgePayload(src, tgt, edge)
        )
        post("graph/$GRAPH_NAME", payload)
    }

    private fun createVertexPayload(v: NewNodeBuilder): Map<String, Any> {
        val node = v.build()
        val propertyMap = CollectionConverters.MapHasAsJava(node.properties()).asJava().toMutableMap()
        propertyMap["label"] = node.label()
        val vertexType = if (v is NewMetaDataBuilder) "META_DATA_VERT" else "CPG_VERT"
        if (v.id() < 0L) v.id(PlumeKeyProvider.getNewId(this))
        return mapOf(
            vertexType to mapOf<String, Any>(
                v.id().toString() to extractAttributesFromMap(propertyMap)
            )
        )
    }

    private fun extractAttributesFromMap(propertyMap: MutableMap<String, Any>): MutableMap<String, Any> =
        VertexMapper.extractAttributesFromMap(propertyMap)
            .mapKeys { if (it.key != "label") "_${it.key}" else it.key }
            .mapValues { mapOf("value" to it.value) }
            .toMutableMap()

    private fun createEdgePayload(from: NewNodeBuilder, to: NewNodeBuilder, edge: String): Map<String, Any> {
        val fromPayload = createVertexPayload(from)
        val toPayload = createVertexPayload(to)
        val fromLabel = fromPayload.keys.first()
        val toLabel = toPayload.keys.first()
        return mapOf(
            fromLabel to mapOf(
                from.id().toString() to mapOf<String, Any>(
                    "_$edge" to mapOf<String, Any>(
                        toLabel to mapOf<String, Any>(
                            to.id().toString() to emptyMap<String, Any>()
                        )
                    )

                )
            )
        )
    }

    override fun getWholeGraph(): Graph {
        val result = get("query/$GRAPH_NAME/showAll")
        return payloadToGraph(result)
    }

    override fun getMethod(fullName: String, signature: String, includeBody: Boolean): Graph {
        val path = if (!includeBody) "getMethodHead" else "getMethod"
        return try {
            val result = get("query/$GRAPH_NAME/$path", mapOf(FULL_NAME to fullName, SIGNATURE to signature))
            payloadToGraph(result)
        } catch (e: PlumeTransactionException) {
            logger.warn("${e.message}. This may be a result of the method not being present in the graph.")
            newOverflowGraph()
        }
    }

    override fun getProgramStructure(): Graph {
        val result = get("query/$GRAPH_NAME/getProgramStructure")
        return payloadToGraph(result)
    }

    override fun getNeighbours(v: NewNodeBuilder): Graph {
        val n = v.build()
        if (v is NewMetaDataBuilder) return newOverflowGraph().apply {
            val newNode = this.addNode(n.label())
            n.properties().foreachEntry { key, value -> newNode.setProperty(key, value) }
        }
        val result = get("query/$GRAPH_NAME/getNeighbours", mapOf("SOURCE" to v.id().toString()))
        return payloadToGraph(result)
    }

    override fun deleteVertex(id: Long, label: String?) {
        if (!checkVertexExists(id, label)) return
        val lbl = if (label == META_DATA) "META_DATA_VERT" else "CPG_VERT"
        delete("graph/$GRAPH_NAME/vertices/$lbl/$id")
    }

    override fun deleteEdge(src: NewNodeBuilder, tgt: NewNodeBuilder, edge: String) {
        if (!exists(src, tgt, edge)) return
        delete("graph/$GRAPH_NAME/edges/CPG_VERT/${src.id()}/_$edge/CPG_VERT/${tgt.id()}")
    }

    override fun deleteMethod(fullName: String, signature: String) {
        try {
            get("query/$GRAPH_NAME/deleteMethod", mapOf(FULL_NAME to fullName, SIGNATURE to signature))
        } catch (e: PlumeTransactionException) {
            logger.warn("${e.message}. This may be a result of the method not being present in the graph.")
        }
    }

    override fun updateVertexProperty(id: Long, label: String?, key: String, value: Any) {
        if (!checkVertexExists(id, label)) return
        val lbl = if (label == META_DATA) "META_DATA_VERT" else "CPG_VERT"
        val payload = mapOf("vertices" to mapOf(lbl to mapOf(id to mapOf("_$key" to mapOf("value" to value)))))
        post("graph/$GRAPH_NAME", payload)
    }

    override fun getMetaData(): NewMetaDataBuilder? =
        get("graph/$GRAPH_NAME/vertices/META_DATA_VERT")
            .map { vertexPayloadToNode(it as JSONObject) }
            .filterIsInstance<NewMetaDataBuilder>()
            .firstOrNull()


    override fun getVertexIds(lowerBound: Long, upperBound: Long): Set<Long> {
        val result = (get(
            endpoint = "query/$GRAPH_NAME/getVertexIds",
            params = mapOf("LOWER_BOUND" to lowerBound.toString(), "UPPER_BOUND" to upperBound.toString())
        ).first() as JSONObject)["@@ids"] as JSONArray
        return result.map { (it as Int).toLong() }.toSet()
    }

    private fun payloadToGraph(a: JSONArray): Graph {
        val graph = newOverflowGraph()
        val vs = mutableMapOf<Long, Node>()
        a[0]?.let { res ->
            val o = res as JSONObject
            val vertices = o["allVert"] as JSONArray
            vertices.map { vertexPayloadToNode(it as JSONObject) }.forEach {
                val n = it.build()
                val node = graph.addNode(it.id(), n.label())
                n.properties().foreachEntry { key, value -> node.setProperty(key, value) }
                vs[it.id()] = node
            }
        }
        a[1]?.let { res ->
            val o = res as JSONObject
            val edges = o["@@edges"] as JSONArray
            edges.forEach { connectEdge(vs, it as JSONObject) }
        }
        return graph
    }

    private fun connectEdge(vertices: Map<Long, Node>, edgePayload: JSONObject) {
        val src = vertices[edgePayload["from_id"].toString().toLong()]
        val tgt = vertices[edgePayload["to_id"].toString().toLong()]
        val edge = edgePayload["e_type"].toString().removePrefix("_")
        if (src != null && tgt != null) src.addEdge(edge, tgt)
    }

    private fun vertexPayloadToNode(o: JSONObject): NewNodeBuilder {
        val attributes = o["attributes"] as JSONObject
        val vertexMap = HashMap<String, Any>()
        attributes.keySet().filter { attributes[it] != "" }
            .map {
                if (it == "id") Pair(it, attributes[it].toString().toLong())
                else Pair(it.removePrefix("_"), attributes[it])
            }
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

    private fun headers(contentType: String = "application/json"): Map<String, String> = if (authKey == null) {
        mapOf("Content-Type" to contentType)
    } else {
        mapOf("Content-Type" to contentType, "Authorization" to "Bearer $authKey")
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
    private fun get(endpoint: String): JSONArray = get(endpoint, emptyMap())

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

    private fun postGSQL(payload: String) {
        val args = arrayOf(
            "-ip", "$hostname:$gsqlPort",
            "-u", username, "-p", password, payload
        )
        val codeControl = CodeControl()
        runCatching {
            logger.debug("Posting payload \"${payload.replace("\\s".toRegex(), " ").subSequence(0, 40)}...\"")
            codeControl.disableSystemExit()
            val output = executeGsqlClient(args)
            logger.debug(output)
        }.onFailure { e -> logger.error("Unable to post GSQL payload! Payload $payload", e) }
        codeControl.enableSystemExit()
    }

    private fun executeGsqlClient(args: Array<String>): String {
        val originalOut = System.out
        val originalErr = System.err
        val out = ByteArrayOutputStream()
        val err = ByteArrayOutputStream()
        System.setOut(PrintStream(out))
        System.setErr(PrintStream(err))
        com.tigergraph.v3_0_5.client.Driver.main(args)
        System.setOut(originalOut)
        System.setErr(originalErr)
        if (err.toString().isNotBlank()) throw PlumeTransactionException(err.toString())
        return out.toString()
    }

    private fun newOverflowGraph(): Graph = Graph.open(
        Config.withDefaults(),
        NodeFactories.allAsJava(),
        EdgeFactories.allAsJava()
    )

    /**
     * Uses the generated schema from [TigerGraphDriver.buildSchemaPayload] and remotely executes it on the database.
     * This is done via the GSQL server using the GSQL client.
     *
     * **See Also:** [Using a Remote GSQL Client](https://docs.tigergraph.com/dev/using-a-remote-gsql-client)
     */
    override fun buildSchema() = postGSQL(buildSchemaPayload())

    fun buildSchemaPayload(): String {
        val schema = StringBuilder(
            """
            DROP ALL
            
            CREATE VERTEX CPG_VERT (
                PRIMARY_ID id UINT,
                label STRING DEFAULT "$UNKNOWN",
        """.trimIndent()
        )
        schema.append("\n")
        val booleanTypes = setOf(
            HAS_MAPPING,
            IS_METHOD_NEVER_OVERRIDDEN,
            IS_EXTERNAL
        )
        val intTypes = setOf(
            COLUMN_NUMBER,
            DEPTH_FIRST_ORDER,
            ARGUMENT_INDEX,
            ORDER,
            LINE_NUMBER,
            LINE_NUMBER_END,
            INTERNAL_FLAGS,
            COLUMN_NUMBER_END
        )
        // Handle vertices
        val metaDataSpecificProps = listOf(LANGUAGE, VERSION, OVERLAYS)
        val propertiesList =
            NodeKeys.ALL.filterNot { metaDataSpecificProps.contains(it.name) || it.name == NODE_LABEL }.toList()
        propertiesList.forEachIndexed { i: Int, key: PropertyKey<Any> ->
            val k = key.name
            when {
                booleanTypes.contains(k) -> schema.append("\t_$k BOOL DEFAULT \"TRUE\"")
                intTypes.contains(k) -> schema.append("\t_$k INT DEFAULT -1")
                else -> schema.append("\t_$k STRING DEFAULT \"null\"")
            }
            if (i < propertiesList.size - 1) schema.append(",\n") else schema.append("\n")
        }
        schema.append(
            """
            ) WITH primary_id_as_attribute="true"
            
            CREATE VERTEX META_DATA_VERT (
                PRIMARY_ID id UINT,
                label STRING DEFAULT "$META_DATA",
                _$LANGUAGE STRING DEFAULT "${ExtractorConst.LANGUAGE_FRONTEND}",
                _$VERSION STRING DEFAULT "${ExtractorConst.LANGUAGE_FRONTEND_VERSION}",
                _$OVERLAYS STRING DEFAULT "null",
                _$HASH STRING DEFAULT "null"
            ) WITH primary_id_as_attribute="true"
        """.trimIndent()
        )
        schema.append("\n\n")
        // Handle edges
        EdgeTypes.ALL.forEach { schema.append("CREATE DIRECTED EDGE _$it (FROM CPG_VERT, TO CPG_VERT)\n") }
        schema.append("\nCREATE GRAPH cpg (*)\n")
        // Get queries
        val payload = javaClass.getResource("/schema/tg_queries.gsql")
        if (payload == null)
            logger.warn("TigerGraph queries are missing in the resource bundle, only schema will be installed.")
        else
            FileReader(File(payload.toURI())).use { fr ->
                fr.readLines()
                    .map { it.replace("<GRAPH_NAME>", GRAPH_NAME).plus("\n") }
                    .forEach(schema::append)
            }
        return schema.toString()
    }

    companion object {
        /**
         * Default hostname for the TigerGraph server with a value of 127.0.0.1.
         */
        private const val DEFAULT_HOSTNAME = "127.0.0.1"

        /**
         * Default username for the TigerGraph server with a value of "tigergraph".
         */
        private const val DEFAULT_USERNAME = "tigergraph"

        /**
         * Default hostname for the TigerGraph server with a value of 127.0.0.1.
         */
        private const val DEFAULT_PASSWORD = "tigergraph"

        /**
         * Default Rest++ port number the TigerGraph server with a value of 9000.
         */
        private const val DEFAULT_RESTPP_PORT = 9000

        /**
         * Default GSQL port number for the TigerGraph server with a value of 14240.
         */
        private const val DEFAULT_GSQL_PORT = 14240

        /**
         * Default graph name for the TigerGraph server a with value of cpg.
         */
        private const val GRAPH_NAME = "cpg"

        /**
         * Default number of request retries for the TigerGraph server with a value of 5.
         */
        private const val MAX_RETRY = 5
    }
}