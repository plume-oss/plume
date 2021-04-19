/*
 * Copyright 2021 Plume Authors
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.plume.oss.drivers

import io.github.plume.oss.domain.exceptions.PlumeSchemaViolationException
import io.github.plume.oss.domain.exceptions.PlumeTransactionException
import io.github.plume.oss.domain.mappers.VertexMapper
import io.github.plume.oss.domain.mappers.VertexMapper.checkSchemaConstraints
import io.github.plume.oss.domain.model.DeltaGraph
import io.github.plume.oss.metrics.ExtractorTimeKey
import io.github.plume.oss.metrics.PlumeTimer
import io.github.plume.oss.util.CodeControl
import io.github.plume.oss.util.ExtractorConst
import io.github.plume.oss.util.ExtractorConst.BOOLEAN_TYPES
import io.github.plume.oss.util.ExtractorConst.INT_TYPES
import io.github.plume.oss.util.PlumeKeyProvider
import io.shiftleft.codepropertygraph.generated.EdgeTypes
import io.shiftleft.codepropertygraph.generated.NodeTypes
import io.shiftleft.codepropertygraph.generated.NodeTypes.*
import io.shiftleft.codepropertygraph.generated.PropertyNames.*
import io.shiftleft.codepropertygraph.generated.nodes.NewMetaDataBuilder
import io.shiftleft.codepropertygraph.generated.nodes.NewNodeBuilder
import khttp.responses.Response
import org.apache.logging.log4j.LogManager
import org.apache.tinkerpop.shaded.jackson.databind.ObjectMapper
import org.json.JSONArray
import org.json.JSONObject
import overflowdb.Config
import overflowdb.Graph
import overflowdb.Node
import scala.jdk.CollectionConverters
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.PrintStream
import java.lang.Thread.sleep
import io.shiftleft.codepropertygraph.generated.edges.Factories as EdgeFactories
import io.shiftleft.codepropertygraph.generated.nodes.Factories as NodeFactories

/**
 * The driver used to connect to a remote TigerGraph instance.
 */
class TigerGraphDriver internal constructor() : IOverridenIdDriver, ISchemaSafeDriver {

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
    var authKey: String = ""
        private set

    /**
     * The query timeout in milliseconds. Default 30 seconds.
     */
    var timeout: Int = 30 * 100
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
    fun username(value: String): TigerGraphDriver = apply { username = value }

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
     *
     * @param value the token to be put in the request.
     */
    fun authKey(value: String): TigerGraphDriver = apply { authKey = value }

    /**
     * Sets the query timeout.
     *
     * @param value the timeout in milliseconds.
     */
    fun timeout(value: Int): TigerGraphDriver = apply { timeout = value }

    override fun addVertex(v: NewNodeBuilder) {
        val payload = mutableMapOf<String, Any>(
            "vertices" to createVertexPayload(v)
        )
        post("graph/$GRAPH_NAME", payload)
    }

    override fun exists(v: NewNodeBuilder): Boolean = checkVertexExists(v.id(), v.build().label())

    private fun checkVertexExists(id: Long, label: String?): Boolean {
        val route = "graph/$GRAPH_NAME/vertices/${label}_VERT"
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
                    "V_FROM.type" to "${src.build().label()}_VERT",
                    "V_TO.type" to "${tgt.build().label()}_VERT",
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
                src.id().toString() to (fromPayload.values.first())[src.id().toString()],
                tgt.id().toString() to (toPayload.values.first())[tgt.id().toString()]
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

    @Suppress("UNCHECKED_CAST")
    override fun bulkTransaction(dg: DeltaGraph) {
        val vAdds = mutableListOf<NewNodeBuilder>()
        val eAdds = mutableListOf<DeltaGraph.EdgeAdd>()
        val vDels = mutableListOf<DeltaGraph.VertexDelete>()
        val eDels = mutableListOf<DeltaGraph.EdgeDelete>()
        PlumeTimer.measure(ExtractorTimeKey.DATABASE_READ) {
            dg.changes.filterIsInstance<DeltaGraph.VertexAdd>().map { it.n }
                .filterNot(::exists)
                .forEachIndexed { i, va -> if (vAdds.none { va === it }) vAdds.add(va.id(-(i + 1).toLong())) }
            dg.changes.filterIsInstance<DeltaGraph.EdgeAdd>().distinct()
                .filterNot { exists(it.src, it.dst, it.e) }
                .toCollection(eAdds)
            dg.changes.filterIsInstance<DeltaGraph.VertexDelete>().filter { checkVertexExists(it.id, it.label) }
                .toCollection(vDels)
            dg.changes.filterIsInstance<DeltaGraph.EdgeDelete>().filter { exists(it.src, it.dst, it.e) }
                .toCollection(eDels)
        }
        PlumeTimer.measure(ExtractorTimeKey.DATABASE_WRITE) {
            // Aggregate all requests going into the add
            val payloadByType = mutableMapOf<String, Any>()
            vAdds.groupBy { it.build().label() }.forEach { (type, ns) ->
                payloadByType["${type}_VERT"] = ns.map {
                    Pair(
                        it.id(PlumeKeyProvider.getNewId(this)).id().toString(),
                        VertexMapper.stripUnusedProperties(
                            type,
                            CollectionConverters.MapHasAsJava(it.build().properties()).asJava().toMutableMap()
                        )
                    )
                }.foldRight(mutableMapOf<String, Any>()) { x, y ->
                    y.apply {
                        this[x.first] = extractAttributesFromMap(x.second.toMutableMap())
                    }
                }
            }
            val eAddPayload = eAdds.map { createEdgePayload(it.src, it.dst, it.e) }
                .foldRight(mutableMapOf<Any, Any>()) { x, y -> deepMerge(x as MutableMap<Any, Any>, y) }
            if (vAdds.size > 1 || eAdds.size > 1) {
                val payload = mapOf(
                    "vertices" to payloadByType,
                    "edges" to eAddPayload
                )
                post("graph/$GRAPH_NAME", payload)
            }
            // Aggregate all requests going into the delete
            vDels.forEach { deleteVertex(it.id, it.label) }
            eDels.forEach { deleteEdge(it.src, it.dst, it.e) }
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun deepMerge(map1: MutableMap<Any, Any>, map2: MutableMap<Any, Any>): MutableMap<Any, Any> {
        for (key in map2.keys) {
            val value2 = map2[key]
            if (map1.containsKey(key)) {
                val value1 = map1[key]
                if (value1 is MutableMap<*, *> && value2 is MutableMap<*, *>)
                    deepMerge(value1 as MutableMap<Any, Any>, value2 as MutableMap<Any, Any>)
                else if (value1 is MutableList<*> && value2 is MutableList<*>)
                    map1[key] = merge(value1 as MutableList<Any>, value2 as MutableList<Any>)
                else
                    map1[key] = value2 as Any
            } else {
                map1[key] = value2 as Any
            }
        }
        return map1
    }

    private fun merge(list1: MutableList<Any>, list2: MutableList<Any>): List<*> {
        list2.removeAll(list1)
        list1.addAll(list2)
        return list1
    }

    private fun createVertexPayload(v: NewNodeBuilder): MutableMap<String, MutableMap<String, Any>> {
        val node = v.build()
        val propertyMap = VertexMapper.stripUnusedProperties(
            v.build().label(),
            CollectionConverters.MapHasAsJava(node.properties()).asJava().toMutableMap()
        )
        val vertexType = "${v.build().label()}_VERT"
        if (v.id() < 0L) v.id(PlumeKeyProvider.getNewId(this))
        return mutableMapOf(
            vertexType to mutableMapOf(
                v.id().toString() to extractAttributesFromMap(propertyMap)
            )
        )
    }

    private fun extractAttributesFromMap(propertyMap: MutableMap<String, Any>): MutableMap<String, Any> =
        VertexMapper.prepareListsInMap(propertyMap)
            .mapKeys { "_${it.key}" }
            .mapValues { mapOf("value" to it.value) }
            .toMutableMap()

    private fun createEdgePayload(
        from: NewNodeBuilder,
        to: NewNodeBuilder,
        edge: String
    ): MutableMap<String, MutableMap<String, MutableMap<String, Any>>> {
        val fromLabel = "${from.build().label()}_VERT"
        val toLabel = "${to.build().label()}_VERT"
        return mutableMapOf(
            fromLabel to mutableMapOf(
                from.id().toString() to mutableMapOf(
                    "_$edge" to mutableMapOf(
                        toLabel to mutableMapOf<String, MutableMap<String, Any>>(
                            to.id().toString() to mutableMapOf()
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

    override fun getMethod(fullName: String, includeBody: Boolean): Graph {
        val path = if (!includeBody) "getMethodHead" else "getMethod"
        return try {
            val result = get("query/$GRAPH_NAME/$path", mapOf(FULL_NAME to fullName))
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
        val result = get(
            "query/$GRAPH_NAME/getNeighbours",
            mapOf(
                "SOURCE" to v.id().toString(),
                "SOURCE.type" to "${v.build().label()}_VERT"
            )
        )
        return payloadToGraph(result)
    }

    override fun deleteVertex(id: Long, label: String?) {
        if (!checkVertexExists(id, label)) return
        delete("graph/$GRAPH_NAME/vertices/${label}_VERT/$id")
    }

    override fun deleteEdge(src: NewNodeBuilder, tgt: NewNodeBuilder, edge: String) {
        if (!exists(src, tgt, edge)) return
        delete(
            "graph/$GRAPH_NAME/edges/${src.build().label()}_VERT/${src.id()}/_$edge/${
                tgt.build().label()
            }_VERT/${tgt.id()}"
        )
    }

    override fun deleteMethod(fullName: String) {
        try {
            get("query/$GRAPH_NAME/deleteMethod", mapOf(FULL_NAME to fullName))
        } catch (e: PlumeTransactionException) {
            logger.warn("${e.message}. This may be a result of the method not being present in the graph.")
        }
    }

    override fun updateVertexProperty(id: Long, label: String?, key: String, value: Any) {
        if (!checkVertexExists(id, label)) return
        val payload =
            mapOf("vertices" to mapOf("${label}_VERT" to mapOf(id to mapOf("_$key" to mapOf("value" to value)))))
        post("graph/$GRAPH_NAME", payload)
    }

    override fun getMetaData(): NewMetaDataBuilder? =
        get("graph/$GRAPH_NAME/vertices/META_DATA_VERT")
            .map { vertexPayloadToNode(it as JSONObject) }
            .filterIsInstance<NewMetaDataBuilder>()
            .firstOrNull()

    override fun getVerticesByProperty(
        propertyKey: String,
        propertyValue: Any,
        label: String?
    ): List<NewNodeBuilder> {
        val path = when {
            BOOLEAN_TYPES.contains(propertyKey) -> "getVerticesByBProperty"
            INT_TYPES.contains(propertyKey) -> "getVerticesByIProperty"
            else -> "getVerticesBySProperty"
        }
        val lbl = "${label}_VERT"
        val result = (get(
            endpoint = "query/$GRAPH_NAME/$path",
            params = mapOf(
                "PROPERTY_KEY" to "_$propertyKey",
                "PROPERTY_VALUE" to propertyValue.toString(),
                "LABEL" to (if (label != null) lbl else "null")
            )
        ).first() as JSONObject)["result"] as JSONArray
        return result.map { vertexPayloadToNode(it as JSONObject) }
            .filter { it.build().properties().keySet().contains(propertyKey) }
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> getPropertyFromVertices(propertyKey: String, label: String?): List<T> {
        val path = when {
            BOOLEAN_TYPES.contains(propertyKey) -> "getBPropertyFromVertices"
            INT_TYPES.contains(propertyKey) -> "getIPropertyFromVertices"
            else -> "getSPropertyFromVertices"
        }
        val lbl = "${label}_VERT"
        val result = (get(
            "query/$GRAPH_NAME/$path",
            params = mapOf(
                "PROPERTY_KEY" to "_$propertyKey",
                "LABEL" to (if (label != null) lbl else "null")
            )
        ).first() as JSONObject)["@@props"] as JSONArray
        return result.map { it as T }
    }

    override fun getVerticesOfType(label: String): List<NewNodeBuilder> {
        val result = (get(
            endpoint = "query/$GRAPH_NAME/getVerticesOfType",
            params = mapOf("LABEL" to "${label}_VERT")
        ).first() as JSONObject)["result"] as JSONArray
        return result.map { vertexPayloadToNode(it as JSONObject) }.toList()
    }

    override fun getVertexIds(lowerBound: Long, upperBound: Long): Set<Long> {
        val result = (get(
            endpoint = "query/$GRAPH_NAME/getVertexIds",
            params = mapOf("LOWER_BOUND" to lowerBound.toString(), "UPPER_BOUND" to upperBound.toString())
        ).first() as JSONObject)["@@ids"] as JSONArray
        return result.map { (it as Int).toLong() }.toSet()
    }

    private fun payloadToGraph(a: JSONArray): Graph {
        val graph = newOverflowGraph()
        PlumeTimer.measure(ExtractorTimeKey.DATABASE_READ) {
            val vs = mutableMapOf<Long, Node>()
            a[0]?.let { res ->
                val o = res as JSONObject
                val vertices = o["allVert"] as JSONArray
                vertices.map { vertexPayloadToNode(it as JSONObject) }.forEach {
                    val n = it.build()
                    if (!vs.containsKey(it.id())) {
                        val node = graph.addNode(it.id(), n.label())
                        n.properties().foreachEntry { key, value -> node.setProperty(key, value) }
                        vs[it.id()] = node
                    }
                }
            }
            a[1]?.let { res ->
                val o = res as JSONObject
                val edges = o["@@edges"] as JSONArray
                edges.forEach { connectEdge(vs, it as JSONObject) }
            }
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
        val vertexMap = mutableMapOf<String, Any>("label" to o["v_type"].toString().removeSuffix("_VERT"))
        attributes.keySet()
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
        NodeTypes.ALL.forEach { nodeType ->
            delete("graph/$GRAPH_NAME/delete_by_type/vertices/${nodeType}_VERT")
        }
        PlumeKeyProvider.clearKeyPools()
    }

    private fun headers(contentType: String = "application/json"): Map<String, String> = if (authKey.isBlank()) {
        mapOf("Content-Type" to contentType, "GSQL-TIMEOUT" to timeout.toString())
    } else {
        mapOf("Content-Type" to contentType, "GSQL-TIMEOUT" to timeout.toString(), "Authorization" to "Bearer $authKey")
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
        var res = JSONArray()
        PlumeTimer.measure(ExtractorTimeKey.DATABASE_READ) {
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
                if (handleResponse(response, tryCount, endpoint)) res = (response.jsonObject["results"] as JSONArray)
            }
        }
        return res
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
        PlumeTimer.measure(ExtractorTimeKey.DATABASE_WRITE) {
            var tryCount = 0
            while (++tryCount < MAX_RETRY) {
                val response = khttp.post(
                    url = "$api/$endpoint",
                    headers = headers(),
                    data = objectMapper.writeValueAsString(payload)
                )
                logger.debug("Post ${response.url} ${response.request.data}")
                logger.debug("Response ${response.text}")
                if (handleResponse(response, tryCount, endpoint)) break
            }
        }
    }

    private fun handleResponse(response: Response, tryCount: Int, endpoint: String): Boolean {
        when {
            response.statusCode == 200 -> if (response.jsonObject["error"] as Boolean) {
                val e = PlumeTransactionException(response.jsonObject["message"] as String)
                logger.debug("Response failed on endpoint $endpoint with response $response", e)
                throw e
            } else return true
            tryCount >= MAX_RETRY -> throw IOException("Could not complete request due to status code ${response.statusCode} at $api/$endpoint")
            else -> sleep(500)
        }
        return false
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
        PlumeTimer.measure(ExtractorTimeKey.DATABASE_WRITE) {
            while (++tryCount < MAX_RETRY) {
                val response = khttp.delete(url = "$api/$endpoint", headers = headers())
                logger.debug("Delete ${response.url}")
                logger.debug("Response ${response.text}")
                when {
                    response.statusCode == 200 -> break
                    tryCount >= MAX_RETRY -> throw IOException("Could not complete delete request due to status code ${response.statusCode} at $api/$endpoint")
                    else -> sleep(500)
                }
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
            logger.debug("Posting payload:\n$payload")
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

    override fun buildSchemaPayload(): String {
        val schema = StringBuilder("DROP ALL\n\n")
        // Handle vertices
        schema.append("$VERTICES\n\n")
        // Handle edges
        EdgeTypes.ALL.mapNotNull { e ->
            val validCombos = mutableListOf<Pair<String, String>>()
            NodeTypes.ALL.forEach { src ->
                NodeTypes.ALL.forEach { dst ->
                    if (checkSchemaConstraints(src, dst, e, true)) {
                        validCombos.add(Pair(src, dst))
                    }
                }
            }
            if (validCombos.isNotEmpty()) Pair(e, validCombos)
            else null
        }.toMap().forEach { (edge, nodePairs) ->
            schema.append("\nCREATE DIRECTED EDGE _$edge (")
            schema.append(nodePairs.joinToString(separator = "|") { pair ->
                val (src, dst) = pair
                "FROM ${src}_VERT, TO ${dst}_VERT"
            })
            schema.append(")")
        }
        schema.append("\nCREATE GRAPH cpg (*)\n")
        // Set queries
        schema.append(QUERIES.replace("<GRAPH_NAME>", GRAPH_NAME))
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

        private val VERTICES: String by lazy {
            """
CREATE VERTEX ${ARRAY_INITIALIZER}_VERT (
    PRIMARY_ID id UINT,
    _$ORDER INT,
    _$CODE STRING,
    _$LINE_NUMBER INT,
    _$COLUMN_NUMBER INT
) WITH primary_id_as_attribute="true"

CREATE VERTEX ${BINDING}_VERT (
    PRIMARY_ID id UINT,
    _$NAME STRING,
    _$SIGNATURE STRING
)

CREATE VERTEX ${META_DATA}_VERT (
    PRIMARY_ID id UINT,
    _$LANGUAGE STRING DEFAULT "${ExtractorConst.LANGUAGE_FRONTEND}",
    _$VERSION STRING DEFAULT "${ExtractorConst.plumeVersion}",
    _$HASH STRING DEFAULT "null"
) WITH primary_id_as_attribute="true"

CREATE VERTEX ${FILE}_VERT (
    PRIMARY_ID id UINT,
    _$NAME STRING,
    _$HASH STRING,
    _$ORDER INT
) WITH primary_id_as_attribute="true"

CREATE VERTEX ${METHOD}_VERT (
    PRIMARY_ID id UINT,
    _$AST_PARENT_FULL_NAME STRING,
    _$AST_PARENT_TYPE STRING,
    _$NAME STRING,
    _$CODE STRING,
    _$IS_EXTERNAL BOOL,
    _$FULL_NAME STRING,
    _$SIGNATURE STRING,
    _$LINE_NUMBER INT,
    _$COLUMN_NUMBER INT,
    _$ORDER INT,
    _$HASH STRING
) WITH primary_id_as_attribute="true"

CREATE VERTEX ${METHOD_PARAMETER_IN}_VERT (
    PRIMARY_ID id UINT,
    _$CODE STRING,
    _$NAME STRING,
    _$EVALUATION_STRATEGY STRING,
    _$TYPE_FULL_NAME STRING,
    _$LINE_NUMBER INT,
    _$COLUMN_NUMBER INT,
    _$ORDER INT
) WITH primary_id_as_attribute="true"

CREATE VERTEX ${METHOD_PARAMETER_OUT}_VERT (
    PRIMARY_ID id UINT,
    _$CODE STRING,
    _$NAME STRING,
    _$EVALUATION_STRATEGY STRING,
    _$TYPE_FULL_NAME STRING,
    _$LINE_NUMBER INT,
    _$COLUMN_NUMBER INT,
    _$ORDER INT
) WITH primary_id_as_attribute="true"

CREATE VERTEX ${METHOD_RETURN}_VERT (
    PRIMARY_ID id UINT,
    _$CODE STRING,
    _$EVALUATION_STRATEGY STRING,
    _$TYPE_FULL_NAME STRING,
    _$LINE_NUMBER INT,
    _$COLUMN_NUMBER INT,
    _$ORDER INT
) WITH primary_id_as_attribute="true"

CREATE VERTEX ${MODIFIER}_VERT (
    PRIMARY_ID id UINT,
    _$MODIFIER_TYPE STRING,
    _$ORDER INT
) WITH primary_id_as_attribute="true"

CREATE VERTEX ${TYPE}_VERT (
    PRIMARY_ID id UINT,
    _$NAME STRING,
    _$FULL_NAME STRING,
    _$TYPE_DECL_FULL_NAME STRING
) WITH primary_id_as_attribute="true"

CREATE VERTEX ${TYPE_DECL}_VERT (
    PRIMARY_ID id UINT,
    _$AST_PARENT_FULL_NAME STRING,
    _$AST_PARENT_TYPE STRING,
    _$NAME STRING,
    _$FULL_NAME STRING,
    _$ORDER INT,
    _$IS_EXTERNAL BOOL
) WITH primary_id_as_attribute="true"

CREATE VERTEX ${TYPE_PARAMETER}_VERT (
    PRIMARY_ID id UINT,
    _$NAME STRING,
    _$ORDER INT
) WITH primary_id_as_attribute="true"

CREATE VERTEX ${TYPE_ARGUMENT}_VERT (
    PRIMARY_ID id UINT,
    _$ORDER INT
) WITH primary_id_as_attribute="true"

CREATE VERTEX ${MEMBER}_VERT (
    PRIMARY_ID id UINT,
    _$NAME STRING,
    _$CODE STRING,
    _$TYPE_FULL_NAME STRING,
    _$ORDER INT
) WITH primary_id_as_attribute="true"

CREATE VERTEX ${NAMESPACE}_VERT (
    PRIMARY_ID id UINT,
    _$NAME STRING,
    _$ORDER INT
) WITH primary_id_as_attribute="true"

CREATE VERTEX ${NAMESPACE_BLOCK}_VERT (
    PRIMARY_ID id UINT,
    _$FULL_NAME STRING,
    _$FILENAME STRING,
    _$NAME STRING,
    _$ORDER INT
) WITH primary_id_as_attribute="true"

CREATE VERTEX ${LITERAL}_VERT (
    PRIMARY_ID id UINT,
    _$CODE STRING,
    _$ARGUMENT_INDEX INT,
    _$TYPE_FULL_NAME STRING,
    _$LINE_NUMBER INT,
    _$COLUMN_NUMBER INT,
    _$ORDER INT
) WITH primary_id_as_attribute="true"

CREATE VERTEX ${CALL}_VERT (
    PRIMARY_ID id UINT,
    _$CODE STRING,
    _$NAME STRING,
    _$LINE_NUMBER INT,
    _$COLUMN_NUMBER INT,
    _$ORDER INT,
    _$ARGUMENT_INDEX INT,
    _$SIGNATURE STRING,
    _$DISPATCH_TYPE STRING,
    _$METHOD_FULL_NAME STRING
) WITH primary_id_as_attribute="true"

CREATE VERTEX ${LOCAL}_VERT (
    PRIMARY_ID id UINT,
    _$CODE STRING,
    _$NAME STRING,
    _$TYPE_FULL_NAME STRING,
    _$LINE_NUMBER INT,
    _$COLUMN_NUMBER INT,
    _$ORDER INT
) WITH primary_id_as_attribute="true"

CREATE VERTEX ${IDENTIFIER}_VERT (
    PRIMARY_ID id UINT,
    _$CODE STRING,
    _$NAME STRING,
    _$TYPE_FULL_NAME STRING,
    _$LINE_NUMBER INT,
    _$COLUMN_NUMBER INT,
    _$ORDER INT,
    _$ARGUMENT_INDEX INT
) WITH primary_id_as_attribute="true"

CREATE VERTEX ${FIELD_IDENTIFIER}_VERT (
    PRIMARY_ID id UINT,
    _$CODE STRING,
    _$ARGUMENT_INDEX INT,
    _$CANONICAL_NAME STRING,
    _$LINE_NUMBER INT,
    _$COLUMN_NUMBER INT,
    _$ORDER INT
) WITH primary_id_as_attribute="true"

CREATE VERTEX ${RETURN}_VERT (
    PRIMARY_ID id UINT,
    _$CODE STRING,
    _$ARGUMENT_INDEX INT,
    _$LINE_NUMBER INT,
    _$COLUMN_NUMBER INT,
    _$ORDER INT
) WITH primary_id_as_attribute="true"

CREATE VERTEX ${BLOCK}_VERT (
    PRIMARY_ID id UINT,
    _$CODE STRING,
    _$ARGUMENT_INDEX INT,
    _$TYPE_FULL_NAME STRING,
    _$LINE_NUMBER INT,
    _$COLUMN_NUMBER INT,
    _$ORDER INT
) WITH primary_id_as_attribute="true"

CREATE VERTEX ${METHOD_REF}_VERT (
    PRIMARY_ID id UINT,
    _$CODE STRING,
    _$ARGUMENT_INDEX INT,
    _$METHOD_FULL_NAME STRING,
    _$LINE_NUMBER INT,
    _$COLUMN_NUMBER INT,
    _$ORDER INT
) WITH primary_id_as_attribute="true"

CREATE VERTEX ${TYPE_REF}_VERT (
    PRIMARY_ID id UINT,
    _$CODE STRING,
    _$ARGUMENT_INDEX INT,
    _$TYPE_FULL_NAME STRING,
    _$LINE_NUMBER INT,
    _$COLUMN_NUMBER INT,
    _$ORDER INT
) WITH primary_id_as_attribute="true"

CREATE VERTEX ${JUMP_TARGET}_VERT (
    PRIMARY_ID id UINT,
    _$CODE STRING,
    _$ARGUMENT_INDEX INT,
    _$NAME STRING,
    _$LINE_NUMBER INT,
    _$COLUMN_NUMBER INT,
    _$ORDER INT
) WITH primary_id_as_attribute="true"

CREATE VERTEX ${CONTROL_STRUCTURE}_VERT (
    PRIMARY_ID id UINT,
    _$CODE STRING,
    _$ARGUMENT_INDEX INT,
    _$CONTROL_STRUCTURE_TYPE STRING,
    _$LINE_NUMBER INT,
    _$COLUMN_NUMBER INT,
    _$ORDER INT
) WITH primary_id_as_attribute="true"

CREATE VERTEX ${UNKNOWN}_VERT (
    PRIMARY_ID id UINT,
    _$CODE STRING,
    _$ARGUMENT_INDEX INT,
    _$TYPE_FULL_NAME STRING,
    _$LINE_NUMBER INT,
    _$COLUMN_NUMBER INT,
    _$ORDER INT
) WITH primary_id_as_attribute="true"
        """.trimIndent()
        }

        private val QUERIES: String by lazy {
            """
CREATE QUERY areVerticesJoinedByEdge(VERTEX V_FROM, VERTEX V_TO, STRING EDGE_LABEL) FOR GRAPH <GRAPH_NAME> {
  bool result;
  setFrom = {ANY};
  temp = SELECT tgt
          FROM setFrom:src -(:e)- :tgt
          WHERE src == V_FROM
            AND tgt == V_TO
            AND e.type == EDGE_LABEL;
  result = (temp.size() > 0);
  PRINT result;
}

CREATE QUERY showAll() FOR GRAPH <GRAPH_NAME> {
  SetAccum<EDGE> @@edges;
  allVert = {ANY};
  result = SELECT s
           FROM allVert:s -(:e)-> :t
           ACCUM @@edges += e;
  PRINT allVert;
  PRINT @@edges;
}

CREATE QUERY getMethodHead(STRING FULL_NAME) FOR GRAPH <GRAPH_NAME> {
  SetAccum<EDGE> @@edges;
  allV = {METHOD_VERT.*};
  start = SELECT src
          FROM allV:src
          WHERE src._FULL_NAME == FULL_NAME;
  allVert = start;

  start = SELECT t
          FROM start:s -(_AST:e)-> :t
          ACCUM @@edges += e;
  allVert = allVert UNION start;

  PRINT allVert;
  PRINT @@edges;
}

CREATE QUERY getMethod(STRING FULL_NAME) FOR GRAPH <GRAPH_NAME> SYNTAX v2 {
  SetAccum<EDGE> @@edges;
  allV = {METHOD_VERT.*};
  # Get method
  start = SELECT src
          FROM allV:src
          WHERE src._FULL_NAME == FULL_NAME;
  allVert = start;
  # Get method's body vertices
  start = SELECT t
          FROM start:s -((_AST>|_REF>|_CFG>|_ARGUMENT>|_BINDS_TO>|_RECEIVER>|_CONDITION>|_BINDS>)*) - :t;
  allVert = allVert UNION start;
  # Get edges between body methods
  finalEdges = SELECT t
               FROM allVert -((_AST>|_REF>|_CFG>|_ARGUMENT>|_BINDS_TO>|_RECEIVER>|_CONDITION>|_BINDS>):e)-:t
               ACCUM @@edges += e;
  PRINT allVert;
  PRINT @@edges;
}

CREATE QUERY getProgramStructure() FOR GRAPH <GRAPH_NAME> SYNTAX v2 {
  SetAccum<EDGE> @@edges;

  start = {FILE_VERT.*, TYPE_DECL_VERT.*, NAMESPACE_BLOCK_VERT.*};
  namespaceBlockSeed = {NAMESPACE_BLOCK_VERT.*};
  
  start = SELECT s
          FROM start:s;
  allVert = start;
  
  start = SELECT t
          FROM namespaceBlockSeed:s -(_AST>*)- :t;
  allVert = allVert UNION start;

  finalEdges = SELECT t
               FROM allVert -(_AST>:e)- :t
               WHERE t.type == "NAMESPACE_BLOCK_VERT"
               ACCUM @@edges += e;

  PRINT allVert;
  PRINT @@edges;
}

CREATE QUERY getNeighbours(VERTEX SOURCE) FOR GRAPH <GRAPH_NAME> SYNTAX v2 {
  SetAccum<EDGE> @@edges;
  seed = {ANY};
  sourceSet = {SOURCE};
  outVert = SELECT tgt
            FROM seed:src -(:e)- :tgt
            WHERE src == SOURCE
            ACCUM @@edges += e;
  allVert = outVert UNION sourceSet;

  PRINT allVert;
  PRINT @@edges;
}

CREATE QUERY deleteMethod(STRING FULL_NAME) FOR GRAPH <GRAPH_NAME> SYNTAX v2 {
  allV = {METHOD_VERT.*};
  # Get method
  start = SELECT src
          FROM allV:src
          WHERE src._FULL_NAME == FULL_NAME;
  allVert = start;
  # Get method's body vertices
  start = SELECT t
          FROM start:s -((_AST>|_CONTAINS>|_REF>|_CFG>|_ARGUMENT>|_BINDS_TO>|_RECEIVER>|_CONDITION>|_BINDS>)*) - :t;
  allVert = allVert UNION start;

  DELETE s FROM allVert:s;
}

CREATE QUERY getVertexIds(INT LOWER_BOUND, INT UPPER_BOUND) FOR GRAPH <GRAPH_NAME> {
  SetAccum<INT> @@ids;
  start = {ANY};
  result = SELECT src
      FROM start:src
      WHERE src.id >= LOWER_BOUND AND src.id <= UPPER_BOUND
      ACCUM @@ids += src.id;
  PRINT @@ids;
}

${
                listOf("STRING", "BOOL", "INT").joinToString("\n\n") { t: String ->
                    """
CREATE QUERY getVerticesBy${t.first()}Property(STRING PROPERTY_KEY, $t PROPERTY_VALUE, STRING LABEL) FOR GRAPH <GRAPH_NAME> {
  start = {ANY};
  IF LABEL == "null" THEN
    result = SELECT src
      FROM start:src
      WHERE src.getAttr(PROPERTY_KEY, "$t") == PROPERTY_VALUE;
  ELSE
    result = SELECT src
      FROM start:src
      WHERE src.type == LABEL 
        AND src.getAttr(PROPERTY_KEY, "$t") == PROPERTY_VALUE;
  END;
  PRINT result;
}
        """.trimIndent()
                }
            }

${
                listOf("STRING", "BOOL", "INT").joinToString("\n\n") { t: String ->
                    """
CREATE QUERY get${t.first()}PropertyFromVertices(STRING PROPERTY_KEY, STRING LABEL) FOR GRAPH <GRAPH_NAME> {
  ListAccum<$t> @@props;
  start = {ANY};
  IF LABEL == "null" THEN
    result = SELECT src
      FROM start:src
      ACCUM @@props += src.getAttr(PROPERTY_KEY, "$t");
  ELSE
    result = SELECT src
      FROM start:src
      WHERE src.type == LABEL 
      ACCUM @@props += src.getAttr(PROPERTY_KEY, "$t");
  END;
  PRINT @@props;
}
        """.trimIndent()
                }
            }

CREATE QUERY getVerticesOfType(STRING LABEL) FOR GRAPH <GRAPH_NAME> {
  start = {ANY};
  result = SELECT src
           FROM start:src
           WHERE src.type == LABEL;
  PRINT result;
}

INSTALL QUERY ALL

        """
        }
    }
}