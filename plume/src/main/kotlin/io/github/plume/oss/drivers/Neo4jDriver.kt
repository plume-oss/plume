package io.github.plume.oss.drivers

import io.github.plume.oss.domain.exceptions.PlumeSchemaViolationException
import io.github.plume.oss.domain.mappers.VertexMapper
import io.github.plume.oss.domain.mappers.VertexMapper.checkSchemaConstraints
import io.github.plume.oss.domain.mappers.VertexMapper.mapToVertex
import io.shiftleft.codepropertygraph.generated.EdgeTypes.AST
import io.shiftleft.codepropertygraph.generated.NodeTypes.*
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
class Neo4jDriver : IDriver {

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

    fun connect() {
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
        val attributes = VertexMapper.extractAttributesFromMap(propertyMap)
        propertyMap.forEach { e ->
            if (attributes[e.key] is Int) attributes[e.key] = (attributes[e.key] as Int).toLong()
        }
        return attributes
    }

    override fun addVertex(v: NewNodeBuilder) {
        val node = v.build()
        val propertyMap = CollectionConverters.MapHasAsJava(node.properties()).asJava().toMutableMap()
        propertyMap["label"] = node.label()
        driver.session().use { session ->
            val payload = StringBuilder("{")
            val attributeList = extractAttributesFromMap(propertyMap).toList()
            attributeList.forEachIndexed { i: Int, e: Pair<String, Any> ->
                payload.append("${e.first}:")
                if (e.second is String) payload.append("\"${e.second}\"") else payload.append(e.second)
                if (i < attributeList.size - 1) payload.append(",")
            }
            payload.append("}")
            session.writeTransaction { tx ->
                val result = tx.run(
                    """
                    CREATE (n:${node.label()} $payload)
                    RETURN ID(n) as id
                    """.trimIndent()
                )
                v.id(result.next()["id"].toString().toLong())
            }

        }
    }

    override fun exists(v: NewNodeBuilder): Boolean = checkVertexExist(v.id(), v.build().label())

    private fun checkVertexExist(id: Long, label: String? = null): Boolean {
        driver.session().use { session ->
            return session.writeTransaction { tx ->
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

    override fun exists(src: NewNodeBuilder, tgt: NewNodeBuilder, edge: String): Boolean {
        if (!exists(src) || !exists(tgt)) return false
        val srcN = src.build()
        val tgtN = tgt.build()
        driver.session().use { session ->
            return session.writeTransaction { tx ->
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

    override fun addEdge(src: NewNodeBuilder, tgt: NewNodeBuilder, edge: String) {
        if (!checkSchemaConstraints(src, tgt, edge)) throw PlumeSchemaViolationException(src, tgt, edge)
        if (!exists(src)) addVertex(src)
        if (!exists(tgt)) addVertex(tgt)
        val srcN = src.build()
        val tgtN = tgt.build()
        driver.session().use { session ->
            return session.writeTransaction { tx ->
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

    override fun clearGraph(): IDriver {
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
        return this
    }

    override fun getWholeGraph(): Graph {
        val graph = newOverflowGraph()
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

    override fun getMethod(fullName: String, signature: String, includeBody: Boolean): Graph {
        val queryHead = if (!includeBody) """
            MATCH (root:$METHOD {FULL_NAME:'$fullName', SIGNATURE:'$signature'})-[r1:$AST]->(child)
                    WITH DISTINCT r1 AS coll
        """.trimIndent()
        else
            """
            MATCH (root:$METHOD {FULL_NAME:'$fullName', SIGNATURE:'$signature'})-[r1:$AST*0..]->(child)-[r2]->(n1) 
                WHERE NOT (child)-[:SOURCE_FILE]-(n1)
            OPTIONAL MATCH (root)-[r3]->(n2) WHERE NOT (root)-[:SOURCE_FILE]-(n2)  
            WITH DISTINCT (r1 + r2 + r3) AS coll
            """.trimIndent()
        val plumeGraph = newOverflowGraph()
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
                    MATCH (n:$TYPE_DECL)
                    RETURN n
                    """.trimIndent()
                ).list()
            }
            typeDecl.map { it["n"].asNode() }
                .map { mapToVertex(it.asMap() + mapOf("id" to it.id())) }
                .filter { graph.node(it.id()) == null }
                .forEach { addNodeToGraph(graph, it) }
        }
        return graph
    }

    override fun getNeighbours(v: NewNodeBuilder): Graph {
        val graph = newOverflowGraph()
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

    override fun deleteEdge(src: NewNodeBuilder, tgt: NewNodeBuilder, edge: String) {
        if (!exists(src, tgt, edge)) return
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

    override fun deleteMethod(fullName: String, signature: String) {
        driver.session().use { session ->
            session.writeTransaction { tx ->
                tx.run(
                    """
                    MATCH (a)-[r:${AST}*]->(t)
                    WHERE a.FULL_NAME = "$fullName" AND a.SIGNATURE = "$signature"
                    FOREACH (x IN r | DELETE x)
                    DETACH DELETE a, t
                    """.trimIndent()
                )
            }
        }
    }

    override fun updateVertexProperty(id: Long, label: String?, key: String, value: Any) {
        if (!checkVertexExist(id, label)) return
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
