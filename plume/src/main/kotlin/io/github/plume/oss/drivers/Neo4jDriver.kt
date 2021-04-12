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
import io.github.plume.oss.metrics.ExtractorTimeKey
import io.github.plume.oss.metrics.PlumeTimer
import io.shiftleft.codepropertygraph.generated.EdgeTypes.AST
import io.shiftleft.codepropertygraph.generated.EdgeTypes.SOURCE_FILE
import io.shiftleft.codepropertygraph.generated.NodeTypes.*
import io.shiftleft.codepropertygraph.generated.nodes.NewMetaDataBuilder
import io.shiftleft.codepropertygraph.generated.nodes.NewNodeBuilder
import org.apache.logging.log4j.LogManager
import org.neo4j.driver.AuthTokens
import org.neo4j.driver.Driver
import org.neo4j.driver.GraphDatabase
import org.neo4j.driver.Value
import overflowdb.Config
import overflowdb.Graph
import overflowdb.Node
import scala.jdk.CollectionConverters
import io.shiftleft.codepropertygraph.generated.edges.Factories as EdgeFactories
import io.shiftleft.codepropertygraph.generated.nodes.Factories as NodeFactories

/**
 * The driver used to connect to a remote Neo4j instance.
 */
class Neo4jDriver internal constructor() : IDriver {

    private val logger = LogManager.getLogger(Neo4jDriver::class.java)
    private lateinit var driver: Driver

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

    fun connect(): Neo4jDriver = apply {
        require(!connected) { "Please close the graph before trying to make another connection." }
        driver = GraphDatabase.driver("bolt://$hostname:$port", AuthTokens.basic(username, password))
        connected = true
    }

    override fun close() {
        require(connected) { "Cannot close a graph that is not already connected!" }
        try {
            driver.close()
            connected = false
        } catch (e: Exception) {
            logger.warn("Exception thrown while attempting to close graph.", e)
        }
    }

    private fun extractAttributesFromMap(propertyMap: MutableMap<String, Any>): MutableMap<String, Any> {
        val attributes = VertexMapper.prepareListsInMap(propertyMap)
        propertyMap.forEach { e ->
            if (attributes[e.key] is Int) attributes[e.key] = (attributes[e.key] as Int).toLong()
        }
        return attributes
    }

    private fun sanitizePayload(p: String): String =
        p.replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\'", "\\\'")
            .replace("\n", "\\\n")
            .replace("\t", "\\\t")
            .replace("\b", "\\\b")
            .replace("\r", "\\\r")
            .replace("\\f", "\\\\f")

    override fun addVertex(v: NewNodeBuilder) {
        if (exists(v)) return
        PlumeTimer.measure(ExtractorTimeKey.DATABASE_WRITE) {
            driver.session().use { session ->
                session.writeTransaction { tx ->
                    val idx = 0
                    val result = tx.run(
                        """
                        ${createVertexPayload(v, idx)}
                        RETURN ID(n$idx) as id$idx
                    """.trimIndent()
                    )
                    v.id(result.next()["id$idx"].toString().toLong())
                }
            }
        }
    }

    private fun createVertexPayload(v: NewNodeBuilder, idx: Int): String {
        val node = v.build()
        val propertyMap = CollectionConverters.MapHasAsJava(node.properties()).asJava().toMutableMap()
        propertyMap["label"] = node.label()
        val payload = StringBuilder("{")
        val attributeList = extractAttributesFromMap(propertyMap).toList()
        attributeList.forEachIndexed { i: Int, e: Pair<String, Any> ->
            payload.append("${e.first}:")
            val p = e.second
            if (p is String) payload.append("\"${sanitizePayload(p)}\"")
            else payload.append(p)
            if (i < attributeList.size - 1) payload.append(",")
        }
        payload.append("}")
        return "CREATE (n$idx:${node.label()} $payload)"
    }

    override fun exists(v: NewNodeBuilder): Boolean = checkVertexExist(v.id(), v.build().label())

    private fun checkVertexExist(id: Long, label: String? = null): Boolean {
        var res = false
        PlumeTimer.measure(ExtractorTimeKey.DATABASE_READ) {
            driver.session().use { session ->
                res = session.writeTransaction { tx ->
                    val result = tx.run(
                        """
                    MATCH (n${if (label != null) ":$label" else ""})
                    WHERE id(n) = $id
                    RETURN n
                    """.trimIndent()
                    )
                    result.list().isNotEmpty()
                }
            }
        }
        return res
    }

    override fun exists(src: NewNodeBuilder, tgt: NewNodeBuilder, edge: String): Boolean {
        var res = false
        PlumeTimer.measure(ExtractorTimeKey.DATABASE_READ) { res = exists(src) && exists(tgt) }
        if (!res) return false
        PlumeTimer.measure(ExtractorTimeKey.DATABASE_READ) {
            val srcN = src.build()
            val tgtN = tgt.build()
            driver.session().use { session ->
                res = session.writeTransaction { tx ->
                    val result = tx.run(
                        """
                    MATCH (a:${srcN.label()}), (b:${tgtN.label()})
                    WHERE id(a) = ${src.id()} AND id(b) = ${tgt.id()}
                    RETURN EXISTS ((a)-[:$edge]->(b)) as edge_exists
                    """.trimIndent()
                    )
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
        PlumeTimer.measure(ExtractorTimeKey.DATABASE_WRITE) {
            val srcN = src.build()
            val tgtN = tgt.build()
            driver.session().use { session ->
                session.writeTransaction { tx ->
                    val result = tx.run(
                        """
                    MATCH (a:${srcN.label()}), (b:${tgtN.label()})
                    WHERE id(a) = ${src.id()} AND id(b) = ${tgt.id()}
                    CREATE (a)-[r:$edge]->(b)
                    RETURN r
                    """.trimIndent()
                    )
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
        PlumeTimer.measure(ExtractorTimeKey.DATABASE_READ) {
            dg.changes.filterIsInstance<DeltaGraph.VertexAdd>().map { it.n }.filterNot(::exists)
                .forEachIndexed { i, va -> if (vAdds.none { va === it }) vAdds.add(va.id(-(i + 1).toLong())) }
            dg.changes.filterIsInstance<DeltaGraph.EdgeAdd>().filter { !exists(it.src, it.dst, it.e) }
                .toCollection(eAdds)
            dg.changes.filterIsInstance<DeltaGraph.VertexDelete>().filter { checkVertexExist(it.id, it.label) }
                .toCollection(vDels)
            dg.changes.filterIsInstance<DeltaGraph.EdgeDelete>().filter { exists(it.src, it.dst, it.e) }
                .toCollection(eDels)
        }
        PlumeTimer.measure(ExtractorTimeKey.DATABASE_WRITE) {
            val idToAlias = mutableMapOf<NewNodeBuilder, String>()
            val vPayloads = mutableMapOf<NewNodeBuilder, String>()
            if (vAdds.isNotEmpty()) {
                val temp = vAdds.distinctBy { it.id() }.toMutableList()
                vAdds.clear()
                vAdds.addAll(temp)
                vAdds.mapIndexed { i, v ->
                    idToAlias[v] = "n$i"
                    vPayloads[v] = createVertexPayload(v, i)
                }
                val create = vPayloads.values.joinToString("\n") { it }
                val ret = "RETURN " + vAdds
                    .joinToString(", ") { v -> "ID(${idToAlias[v]}) as id${idToAlias[v]}" }
                driver.session().use { session ->
                    session.writeTransaction { tx ->
                        val result = tx.run(
                            """
                            $create
                            $ret
                        """.trimIndent()
                        )
                        val row = result.next()
                        vAdds.forEach { v -> v.id(row["id${idToAlias[v]}"].toString().toLong()) }
                    }
                }
            }
            if (eAdds.isNotEmpty()) {
                val match = "MATCH " + vAdds
                    .mapIndexed { i, v -> idToAlias[v] = "n$i"; "(n$i:${v.build().label()})" }
                    .joinToString(", ")
                val where = "WHERE " + vAdds.joinToString(" AND ") { v -> "id(${idToAlias[v]})=${v.id()}" }
                val create = "CREATE " + eAdds
                    .mapIndexed { i, eAdd -> "(${idToAlias[eAdd.src]})-[r$i:${eAdd.e}]->(${idToAlias[eAdd.dst]})" }
                    .joinToString(", ")
                driver.session().use { session ->
                    session.writeTransaction { tx ->
                        tx.run(
                            """
                            $match
                            $where
                            $create
                        """.trimIndent()
                        )
                    }
                }
            }
            // TODO: This can be bulk but deletes are currently very uncommon
            vDels.forEach { deleteVertex(it.id, it.label) }
            eDels.forEach { deleteEdge(it.src, it.dst, it.e) }
        }
    }

    override fun clearGraph(): IDriver {
        PlumeTimer.measure(ExtractorTimeKey.DATABASE_WRITE) {
            driver.session().use { session ->
                session.writeTransaction { tx ->
                    tx.run(
                        """
                    MATCH (n)
                    DETACH DELETE n
                    """.trimIndent()
                    )
                }
            }
        }
        return this
    }

    override fun getWholeGraph(): Graph {
        val graph = newOverflowGraph()
        PlumeTimer.measure(ExtractorTimeKey.DATABASE_READ) {
            driver.session().use { session ->
                val orphanVertices = session.writeTransaction { tx ->
                    tx.run(
                        """
                    MATCH (n)
                    WHERE NOT (n)-[]-()
                    RETURN n
                    """.trimIndent()
                    ).list()
                }
                orphanVertices.map { it["n"].asNode() }
                    .map { mapToVertex(it.asMap() + mapOf("id" to it.id())) }
                    .forEach { addNodeToGraph(graph, it) }
                val edgeResult = session.writeTransaction { tx ->
                    tx.run(
                        """
                    MATCH (n)-[r]->(m)
                    RETURN n AS src, m AS tgt, type(r) AS rel 
                    """.trimIndent()
                    ).list()
                }
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
        val queryHead = if (!includeBody) """
            MATCH (root:$METHOD {FULL_NAME:'$fullName'})-[r1:$AST]->(child)
                    WITH DISTINCT r1 AS coll
        """.trimIndent()
        else
            """
            MATCH (root:$METHOD {FULL_NAME:'$fullName'})-[r1:$AST*0..]->(child)-[r2]->(n1) 
                WHERE NOT (child)-[:$SOURCE_FILE]-(n1)
            OPTIONAL MATCH (root)-[r3]->(n2) WHERE NOT (root)-[:$SOURCE_FILE]-(n2)  
            WITH DISTINCT (r1 + r2 + r3) AS coll
            """.trimIndent()
        val plumeGraph = newOverflowGraph()
        PlumeTimer.measure(ExtractorTimeKey.DATABASE_READ) {
            driver.session().use { session ->
                session.writeTransaction { tx ->
                    val result = tx.run(
                        """
                    $queryHead
                    UNWIND coll AS e1
                    WITH DISTINCT e1
                    WITH [r in collect(e1) | {rel: type(r), src: startNode(r), tgt: endNode(r)} ] as e2
                    UNWIND e2 as x
                    RETURN x
                    """.trimIndent()
                    ).list().map { it["x"] }
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
                tx.run(
                    """
                    MATCH (n:$FILE)-[r1:$AST*0..]->(m)-[r2]->(o) 
                    WITH DISTINCT (r1 + r2) AS coll
                    UNWIND coll AS e1
                    WITH DISTINCT e1
                    WITH [r in collect(e1) | {rel: type(r), src: startNode(r), tgt: endNode(r)} ] as e2
                    UNWIND e2 as x
                    RETURN x
                    """.trimIndent()
                ).list().map { it["x"] }
            }
            neo4jToOverflowGraph(result, graph)
            val typeDecl = session.writeTransaction { tx ->
                tx.run(
                    """
                    MATCH (m:$TYPE_DECL)
                    MATCH (n:$FILE)
                    MATCH (o:$NAMESPACE_BLOCK)
                    RETURN m, n, o
                    """.trimIndent()
                ).list()
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
        PlumeTimer.measure(ExtractorTimeKey.DATABASE_READ) {
            driver.session().use { session ->
                val result = session.writeTransaction { tx ->
                    tx.run(
                        """
                    MATCH (n:${v.build().label()})-[r1]-(m)
                    WHERE ID(n) = ${v.id()}
                    WITH DISTINCT r1 AS coll
                    UNWIND coll AS e1
                    WITH DISTINCT e1
                    WITH [r in collect(e1) | {rel: type(r), src: startNode(r), tgt: endNode(r)} ] as e2
                    UNWIND e2 as x
                    RETURN x
                    """.trimIndent()
                    ).list().map { it["x"] }
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
        PlumeTimer.measure(ExtractorTimeKey.DATABASE_WRITE) {
            driver.session().use { session ->
                session.writeTransaction { tx ->
                    tx.run(
                        """
                    MATCH (n${if (label != null) ":$label" else ""})
                    WHERE ID(n) = $id
                    DETACH DELETE n
                    """.trimIndent()
                    )
                }
            }
        }
    }

    override fun deleteEdge(src: NewNodeBuilder, tgt: NewNodeBuilder, edge: String) {
        if (!exists(src, tgt, edge)) return
        PlumeTimer.measure(ExtractorTimeKey.DATABASE_WRITE) {
            val srcN = src.build()
            val tgtN = tgt.build()
            driver.session().use { session ->
                session.writeTransaction { tx ->
                    tx.run(
                        """
                    MATCH (s:${srcN.label()})-[r:$edge]->(t:${tgtN.label()})
                    WHERE ID(s) = ${src.id()} AND ID(t) = ${tgt.id()}  
                    DELETE r
                    """.trimIndent()
                    )
                }
            }
        }
    }

    override fun deleteMethod(fullName: String) {
        PlumeTimer.measure(ExtractorTimeKey.DATABASE_WRITE) {
            driver.session().use { session ->
                session.writeTransaction { tx ->
                    tx.run(
                        """
                    MATCH (a)-[r:${AST}*]->(t)
                    WHERE a.FULL_NAME = "$fullName"
                    FOREACH (x IN r | DELETE x)
                    DETACH DELETE a, t
                    """.trimIndent()
                    )
                }
            }
        }
    }

    override fun updateVertexProperty(id: Long, label: String?, key: String, value: Any) {
        if (!checkVertexExist(id, label)) return
        PlumeTimer.measure(ExtractorTimeKey.DATABASE_WRITE) {
            driver.session().use { session ->
                session.writeTransaction { tx ->
                    tx.run(
                        """
                    MATCH (n${if (label != null) ":$label" else ""})
                    WHERE ID(n) = $id
                    SET n.$key = ${if (value is String) "\"$value\"" else value}
                    """.trimIndent()
                    )
                }
            }
        }
    }

    override fun getMetaData(): NewMetaDataBuilder? {
        var meta: NewMetaDataBuilder? = null
        PlumeTimer.measure(ExtractorTimeKey.DATABASE_WRITE) {
            driver.session().use { session ->
                meta = session.writeTransaction { tx ->
                    tx.run(
                        """
                    MATCH (n:$META_DATA)
                    RETURN n
                    LIMIT 1
                    """.trimIndent()
                    ).list().map { it["n"].asNode() }
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
        if (propertyKey.length != sanitizePayload(propertyKey).length || propertyKey.contains("[<|>]".toRegex())) return emptyList()
        PlumeTimer.measure(ExtractorTimeKey.DATABASE_READ) {
            driver.session().use { session ->
                session.writeTransaction { tx ->
                    tx.run(
                        """
                    MATCH (n${if (label != null) ":$label" else ""})
                    WHERE n.$propertyKey = ${if (propertyValue is String) "\"$propertyValue\"" else propertyValue}
                    RETURN n
                    """.trimIndent()
                    ).list().map { it["n"].asNode() }
                        .map { mapToVertex(it.asMap() + mapOf("id" to it.id())) }
                }.toCollection(l)
            }
        }
        return l
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> getPropertyFromVertices(propertyKey: String, label: String?): List<T> {
        val l = mutableListOf<T>()
        if (propertyKey.length != sanitizePayload(propertyKey).length || propertyKey.contains("[<|>]".toRegex())) return emptyList()
        PlumeTimer.measure(ExtractorTimeKey.DATABASE_READ) {
            driver.session().use { session ->
                session.writeTransaction { tx ->
                    tx.run(
                        """
                    MATCH (n${if (label != null) ":$label" else ""})
                    RETURN n.$propertyKey AS p
                    """.trimIndent()
                    ).list().map { it["p"].asObject() as T }
                }.toCollection(l)
            }
        }
        return l
    }

    override fun getVerticesOfType(label: String): List<NewNodeBuilder> {
        val l = mutableListOf<NewNodeBuilder>()
        PlumeTimer.measure(ExtractorTimeKey.DATABASE_READ) {
            driver.session().use { session ->
                session.writeTransaction { tx ->
                    tx.run(
                        """
                    MATCH (n:$label)
                    RETURN n
                    """.trimIndent()
                    ).list().map { it["n"].asNode() }
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
    }
}
