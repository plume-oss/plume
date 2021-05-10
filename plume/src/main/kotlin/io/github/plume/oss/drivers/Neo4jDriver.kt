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
import io.github.plume.oss.domain.mappers.VertexMapper
import io.github.plume.oss.domain.mappers.VertexMapper.checkSchemaConstraints
import io.github.plume.oss.domain.mappers.VertexMapper.mapToVertex
import io.github.plume.oss.domain.model.DeltaGraph
import io.github.plume.oss.metrics.DriverTimeKey
import io.github.plume.oss.metrics.PlumeTimer
import io.shiftleft.codepropertygraph.generated.EdgeTypes.AST
import io.shiftleft.codepropertygraph.generated.EdgeTypes.SOURCE_FILE
import io.shiftleft.codepropertygraph.generated.NodeTypes.*
import io.shiftleft.codepropertygraph.generated.nodes.NewMetaDataBuilder
import io.shiftleft.codepropertygraph.generated.nodes.NewNodeBuilder
import org.apache.logging.log4j.LogManager
import org.neo4j.dbms.api.DatabaseManagementService
import org.neo4j.dbms.api.DatabaseManagementServiceBuilder
import org.neo4j.driver.AuthTokens
import org.neo4j.driver.Driver
import org.neo4j.driver.GraphDatabase
import org.neo4j.driver.Value
import org.neo4j.graphdb.GraphDatabaseService
import overflowdb.Config
import overflowdb.Graph
import overflowdb.Node
import scala.jdk.CollectionConverters
import java.io.File
import io.shiftleft.codepropertygraph.generated.edges.Factories as EdgeFactories
import io.shiftleft.codepropertygraph.generated.nodes.Factories as NodeFactories

/**
 * The driver used to connect to a remote Neo4j instance.
 */
class Neo4jDriver internal constructor() : IDriver, CypherDriverQueries() {

    private val logger = LogManager.getLogger(Neo4jDriver::class.java)
    private lateinit var driver: Driver
    private lateinit var mgmtService: DatabaseManagementService
    private lateinit var embeddedDb: GraphDatabaseService

    /**
     * Indicates whether the driver is connected to the graph database or not.
     */
    var connected = false
        private set

    /**
     * The Neo4j server database name.
     * @see DEFAULT_DATABASE
     */
    var database: String = DEFAULT_DATABASE
        private set

    /**
     * The Neo4j server username.
     * @see DEFAULT_USERNAME
     */
    var username: String = DEFAULT_USERNAME
        private set

    /**
     * The Neo4j server password.
     * @see DEFAULT_PASSWORD
     */
    var password: String = DEFAULT_PASSWORD
        private set

    /**
     * The Neo4j server hostname.
     * @see DEFAULT_HOSTNAME
     */
    var hostname: String = DEFAULT_HOSTNAME
        private set

    /**
     * The Neo4j server port.
     * @see DEFAULT_PORT
     */
    var port: Int = DEFAULT_PORT
        private set

    /**
     * Whether to run Neo4j in embedded mode or not. Default is false.
     */
    var embedded: Boolean = false
        private set

    /**
     * The database directory for serialized graphs. Only used when embedded mode used.
     * @see DEFAULT_DATABASE_DIRECTORY
     */
    var databaseDirectory: String = DEFAULT_DATABASE_DIRECTORY
        private set

    /**
     * Set the database name for the Neo4j server.
     *
     * @param value the database name e.g. "graph.db", "neo4j"
     */
    fun database(value: String) = apply { database = value }

    /**
     * Set the username for the Neo4j server.
     *
     * @param value the username e.g. "neo4j_user"
     */
    fun username(value: String) = apply { username = value }

    /**
     * Set the password for the Neo4j server.
     *
     * @param value the password e.g. "neo4j123"
     */
    fun password(value: String) = apply { password = value }

    /**
     * Set the hostname for the Neo4j server.
     *
     * @param value the hostname e.g. 127.0.0.1, www.neoserver.com, etc.
     */
    fun hostname(value: String) = apply { hostname = value }

    /**
     * Set the port for the Neo4j server.
     *
     * @param value the port number e.g. 7687
     */
    fun port(value: Int) = apply { port = value }

    /**
     * Sets whether Neo4j will run in embedded mode or not.
     *
     * @param value the flag to set embedded mode or not.
     */
    fun embedded(value: Boolean) = apply { embedded = value }

    /**
     * Sets the directory to store serialized embedded graphs. Only used when embedded mode is true.
     *
     * @value value the directory to store the graphs.
     */
    fun databaseDirectory(value: String) = apply { databaseDirectory = value }

    fun connect(): Neo4jDriver = apply {
        PlumeTimer.measure(DriverTimeKey.CONNECT_DESERIALIZE) {
            require(!connected) { "Please close the graph before trying to make another connection." }
            if (!embedded) {
                driver = GraphDatabase.driver("bolt://$hostname:$port", AuthTokens.basic(username, password))
            } else {
                mgmtService = DatabaseManagementServiceBuilder(File(databaseDirectory).toPath()).build()
                embeddedDb = mgmtService.database(database)
                registerShutdownHook(mgmtService)
            }
            connected = true
        }
    }

    override fun close() {
        PlumeTimer.measure(DriverTimeKey.DISCONNECT_SERIALIZE) {
            require(connected) { "Cannot close a graph that is not already connected!" }
            try {
                if (!embedded) driver.close()
                else mgmtService.shutdown()
            } catch (e: Exception) {
                logger.warn("Exception thrown while attempting to close graph.", e)
            } finally {
                connected = false
            }
        }
    }

    private fun registerShutdownHook(managementService: DatabaseManagementService) {
        Runtime.getRuntime().addShutdownHook(object : Thread() {
            override fun run() {
                try {
                    managementService.shutdown()
                } catch (ignored: Exception) {
                }
            }
        })
    }

    override fun addVertex(v: NewNodeBuilder) {
        if (exists(v)) return
        PlumeTimer.measure(DriverTimeKey.DATABASE_WRITE) {
            driver.session().use { session ->
                session.writeTransaction { tx ->
                    val result = tx.run(addVertexCypher(v, 0))
                    v.id(result.next()["id0"].toString().toLong())
                }
            }
        }
    }

    override fun exists(v: NewNodeBuilder): Boolean = checkVertexExist(v.id(), v.build().label())

    private fun checkVertexExist(id: Long, label: String? = null): Boolean {
        var res = false
        PlumeTimer.measure(DriverTimeKey.DATABASE_READ) {
            driver.session().use { session ->
                res = session.writeTransaction { tx ->
                    val result = tx.run(checkVertexExistCypher(id, label))
                    result.list().isNotEmpty()
                }
            }
        }
        return res
    }

    override fun exists(src: NewNodeBuilder, tgt: NewNodeBuilder, edge: String): Boolean {
        var res = false
        PlumeTimer.measure(DriverTimeKey.DATABASE_READ) { res = exists(src) && exists(tgt) }
        if (!res) return false
        PlumeTimer.measure(DriverTimeKey.DATABASE_READ) {
            driver.session().use { session ->
                res = session.writeTransaction { tx ->
                    val result = tx.run(checkEdgeExistCypher(src, tgt, edge))
                    result.next()["edge_exists"].toString() == "TRUE"
                }
            }
        }
        return res
    }

    override fun addEdge(src: NewNodeBuilder, tgt: NewNodeBuilder, edge: String) {
        if (!checkSchemaConstraints(src, tgt, edge)) throw PlumeSchemaViolationException(src, tgt, edge)
        if (!exists(src)) addVertex(src)
        if (!exists(tgt)) addVertex(tgt)
        PlumeTimer.measure(DriverTimeKey.DATABASE_WRITE) {
            driver.session().use { session ->
                session.writeTransaction { tx ->
                    val result = tx.run(addEdgeCypher(src, tgt, edge))
                    result.list().isNotEmpty()
                }
            }
        }
    }

    override fun bulkTransaction(dg: DeltaGraph) {
        val vAdds = mutableListOf<NewNodeBuilder>()
        val eAdds = mutableListOf<DeltaGraph.EdgeAdd>()
        val vDels = mutableListOf<DeltaGraph.VertexDelete>()
        val eDels = mutableListOf<DeltaGraph.EdgeDelete>()
        PlumeTimer.measure(DriverTimeKey.DATABASE_READ) {
            dg.changes.filterIsInstance<DeltaGraph.VertexAdd>().map { it.n }.filterNot(::exists)
                .forEachIndexed { i, va -> if (vAdds.none { va === it }) vAdds.add(va.id(-(i + 1).toLong())) }
            dg.changes.filterIsInstance<DeltaGraph.EdgeAdd>().distinct().filterNot { exists(it.src, it.dst, it.e) }
                .toCollection(eAdds)
            dg.changes.filterIsInstance<DeltaGraph.VertexDelete>().filter { checkVertexExist(it.id, it.label) }
                .toCollection(vDels)
            dg.changes.filterIsInstance<DeltaGraph.EdgeDelete>().filter { exists(it.src, it.dst, it.e) }
                .toCollection(eDels)
        }
        PlumeTimer.measure(DriverTimeKey.DATABASE_WRITE) {
            val idToAlias = mutableMapOf<NewNodeBuilder, String>()
            val vPayloads = mutableMapOf<NewNodeBuilder, String>()
            if (vAdds.isNotEmpty()) {
                vAdds.distinctBy { it.id() }.chunked(50).forEach { vs ->
                    idToAlias.clear()
                    vPayloads.clear()
                    vs.mapIndexed { i, v ->
                        idToAlias[v] = "n$i"
                        vPayloads[v] = createVertexPayload(v, i)
                    }
                    val create = vPayloads.values.joinToString("\n") { it }
                    val ret = "RETURN " + vs
                        .joinToString(", ") { v -> "ID(${idToAlias[v]}) as id${idToAlias[v]}" }
                    driver.session().use { session ->
                        try {
                            session.writeTransaction { tx ->
                                val result = tx.run(
                                    """
                                $create
                                $ret
                                """.trimIndent()
                                )
                                val row = result.next()
                                vs.forEach { v -> v.id(row["id${idToAlias[v]}"].toString().toLong()) }
                            }
                        } catch (e: Exception) {
                            logger.error("Exception occurred while writing tx with body: $create $ret")
                            throw e
                        }
                    }
                }
            }
            if (eAdds.isNotEmpty()) {
                eAdds.chunked(50).forEach { es ->
                    idToAlias.clear()
                    // Separate the vs attached to es
                    es.flatMap { listOf(it.src, it.dst) }.distinct().forEachIndexed { i, v -> idToAlias[v] = "n$i" }
                    val match = "MATCH " + idToAlias.keys
                        .mapIndexed { i, v -> idToAlias[v] = "n$i"; "(n$i:${v.build().label()})" }
                        .joinToString(", ")
                    val where = "WHERE " + idToAlias.keys.joinToString(" AND ") { v -> "id(${idToAlias[v]})=${v.id()}" }
                    val create = "CREATE " + es
                        .mapIndexed { i, eAdd -> "(${idToAlias[eAdd.src]})-[r$i:${eAdd.e}]->(${idToAlias[eAdd.dst]})" }
                        .joinToString(", ")
                    driver.session().use { session ->
                        try {
                            session.writeTransaction { tx ->
                                tx.run(
                                    """
                                    $match
                                    $where
                                    $create
                                """.trimIndent()
                                )
                            }
                        } catch (e: Exception) {
                            logger.error("Exception occurred while writing tx with body: $match $where $create")
                            throw e
                        }
                    }
                }

            }
            // TODO: This can be bulk but deletes are currently very uncommon
            vDels.forEach { deleteVertex(it.id, it.label) }
            eDels.forEach { deleteEdge(it.src, it.dst, it.e) }
        }
    }

    override fun clearGraph(): IDriver {
        PlumeTimer.measure(DriverTimeKey.DATABASE_WRITE) {
            driver.session().use { session ->
                session.writeTransaction { tx ->
                    tx.run(clearGraphCypher())
                }
            }
        }
        return this
    }

    override fun getWholeGraph(): Graph {
        val graph = newOverflowGraph()
        PlumeTimer.measure(DriverTimeKey.DATABASE_READ) {
            driver.session().use { session ->
                val orphanVertices = session.writeTransaction { tx -> tx.run(getAllVerticesCypher()).list() }
                orphanVertices.map { it["n"].asNode() }
                    .map { mapToVertex(it.asMap() + mapOf("id" to it.id())) }
                    .forEach { addNodeToGraph(graph, it) }
                val edgeResult = session.writeTransaction { tx -> tx.run(getAllEdgesCypher()).list() }
                edgeResult.map { r -> Triple(r["src"].asNode(), r["tgt"].asNode(), r["rel"].asString()) }
                    .map {
                        Triple(
                            mapToVertex(it.first.asMap() + mapOf("id" to it.first.id())),
                            mapToVertex(it.second.asMap() + mapOf("id" to it.second.id())),
                            it.third
                        )
                    }.forEach {
                        val src = addNodeToGraph(graph, it.first)
                        val tgt = addNodeToGraph(graph, it.second)
                        src.addEdge(it.third, tgt)
                    }
            }
        }
        return graph
    }

    private fun addNodeToGraph(graph: Graph, v: NewNodeBuilder): Node {
        val maybeExistingNode = graph.node(v.id())
        if (maybeExistingNode != null) return maybeExistingNode

        val bNode = v.build()
        val sNode = graph.addNode(v.id(), bNode.label())
        bNode.properties().foreachEntry { key, value -> sNode.setProperty(key, value) }
        return sNode
    }

    override fun getMethod(fullName: String, includeBody: Boolean): Graph {
        val queryHead = getMethodQueryHead(fullName, includeBody)
        val plumeGraph = newOverflowGraph()
        PlumeTimer.measure(DriverTimeKey.DATABASE_READ) {
            driver.session().use { session ->
                session.writeTransaction { tx ->
                    val result = tx.run(getMethodCypher(queryHead)).list().map { it["x"] }
                    neo4jToOverflowGraph(result, plumeGraph)
                }
            }
        }
        return plumeGraph
    }

    override fun getProgramStructure(): Graph {
        val graph = newOverflowGraph()
        driver.session().use { session ->
            val result = session.writeTransaction { tx ->
                tx.run(getProgramStructureCypher1()).list().map { it["x"] }
            }
            neo4jToOverflowGraph(result, graph)
            val typeDecl = session.writeTransaction { tx ->
                tx.run(getProgramStructureCypher2()).list()
            }
            typeDecl.flatMap { listOf(it["m"].asNode(), it["n"].asNode(), it["o"].asNode()) }
                .map { mapToVertex(it.asMap() + mapOf("id" to it.id())) }
                .filter { graph.node(it.id()) == null }
                .forEach { addNodeToGraph(graph, it) }
        }
        return graph
    }

    override fun getNeighbours(v: NewNodeBuilder): Graph {
        val graph = newOverflowGraph()
        PlumeTimer.measure(DriverTimeKey.DATABASE_READ) {
            driver.session().use { session ->
                val result = session.writeTransaction { tx ->
                    tx.run(getNeighboursCypher(v)).list().map { it["x"] }
                }
                neo4jToOverflowGraph(result, graph)
            }
        }
        return graph
    }

    private fun neo4jToOverflowGraph(
        result: List<Value>,
        graph: Graph
    ) {
        result.map { r -> Triple(r["src"].asNode(), r["tgt"].asNode(), r["rel"].asString()) }
            .map { p ->
                Triple(
                    mapToVertex(p.first.asMap() + mapOf("id" to p.first.id())),
                    mapToVertex(p.second.asMap() + mapOf("id" to p.second.id())),
                    p.third
                )
            }.forEach {
                val src = addNodeToGraph(graph, it.first)
                val tgt = addNodeToGraph(graph, it.second)
                src.addEdge(it.third, tgt)
            }
    }

    override fun deleteVertex(id: Long, label: String?) {
        if (!checkVertexExist(id, label)) return
        PlumeTimer.measure(DriverTimeKey.DATABASE_WRITE) {
            driver.session().use { session ->
                session.writeTransaction { tx ->
                    tx.run(deleteVertexCypher(id, label))
                }
            }
        }
    }

    override fun deleteEdge(src: NewNodeBuilder, tgt: NewNodeBuilder, edge: String) {
        if (!exists(src, tgt, edge)) return
        PlumeTimer.measure(DriverTimeKey.DATABASE_WRITE) {
            driver.session().use { session ->
                session.writeTransaction { tx ->
                    tx.run(deleteEdgeCypher(src, tgt, edge))
                }
            }
        }
    }

    override fun deleteMethod(fullName: String) {
        PlumeTimer.measure(DriverTimeKey.DATABASE_WRITE) {
            driver.session().use { session ->
                session.writeTransaction { tx ->
                    tx.run(deleteMethodCypher(fullName))
                }
            }
        }
    }

    override fun updateVertexProperty(id: Long, label: String?, key: String, value: Any) {
        if (!checkVertexExist(id, label)) return
        PlumeTimer.measure(DriverTimeKey.DATABASE_WRITE) {
            driver.session().use { session ->
                session.writeTransaction { tx ->
                    tx.run(updateVertexPropertyCypher(id, label, key, value))
                }
            }
        }
    }

    override fun getMetaData(): NewMetaDataBuilder? {
        var meta: NewMetaDataBuilder? = null
        PlumeTimer.measure(DriverTimeKey.DATABASE_WRITE) {
            driver.session().use { session ->
                meta = session.writeTransaction { tx ->
                    tx.run(getMetaDataCypher()).list().map { it["n"].asNode() }
                        .map { mapToVertex(it.asMap() + mapOf("id" to it.id())) }
                        .firstOrNull() as NewMetaDataBuilder?
                }
            }
        }
        return meta
    }

    override fun getVerticesByProperty(
        propertyKey: String,
        propertyValue: Any,
        label: String?
    ): List<NewNodeBuilder> {
        val l = mutableListOf<NewNodeBuilder>()
        if (propertyKey.length != CypherDriverQueries.sanitizePayload(propertyKey).length || propertyKey.contains("[<|>]".toRegex())) return emptyList()
        PlumeTimer.measure(DriverTimeKey.DATABASE_READ) {
            driver.session().use { session ->
                session.writeTransaction { tx ->
                    tx.run(getVerticesByPropertyCypher(propertyKey, propertyValue, label)).list()
                        .map { it["n"].asNode() }
                        .map { mapToVertex(it.asMap() + mapOf("id" to it.id())) }
                }.toCollection(l)
            }
        }
        return l
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> getPropertyFromVertices(propertyKey: String, label: String?): List<T> {
        val l = mutableListOf<T>()
        if (propertyKey.length != CypherDriverQueries.sanitizePayload(propertyKey).length || propertyKey.contains("[<|>]".toRegex())) return emptyList()
        PlumeTimer.measure(DriverTimeKey.DATABASE_READ) {
            driver.session().use { session ->
                session.writeTransaction { tx ->
                    tx.run(getPropertyFromVerticesCypher(propertyKey, label)).list().map { it["p"].asObject() as T }
                }.toCollection(l)
            }
        }
        return l
    }

    override fun getVerticesOfType(label: String): List<NewNodeBuilder> {
        val l = mutableListOf<NewNodeBuilder>()
        PlumeTimer.measure(DriverTimeKey.DATABASE_READ) {
            driver.session().use { session ->
                session.writeTransaction { tx ->
                    tx.run(getVerticesOfTypeCypher(label)).list().map { it["n"].asNode() }
                        .map { mapToVertex(it.asMap() + mapOf("id" to it.id())) }
                }.toCollection(l)
            }
        }
        return l
    }

    private fun newOverflowGraph(): Graph = Graph.open(
        Config.withDefaults(),
        NodeFactories.allAsJava(),
        EdgeFactories.allAsJava()
    )

    companion object {
        /**
         * Default username for the Neo4j server.
         */
        private const val DEFAULT_USERNAME = "neo4j"

        /**
         * Default password for the Neo4j server.
         */
        private const val DEFAULT_PASSWORD = "neo4j"

        /**
         * Default hostname for the Neo4j server.
         */
        private const val DEFAULT_HOSTNAME = "localhost"

        /**
         * Default database name for the Neo4j server.
         */
        private const val DEFAULT_DATABASE = "neo4j"

        /**
         * Default port number a remote Bolt server.
         */
        private const val DEFAULT_PORT = 7687

        /**
         * Default directory for where an embedded database will be serialized to.
         */
        private const val DEFAULT_DATABASE_DIRECTORY = "."
    }
}
