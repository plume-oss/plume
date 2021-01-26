package io.github.plume.oss.drivers

import io.github.plume.oss.domain.enums.EdgeLabel
import io.github.plume.oss.domain.enums.VertexLabel
import io.github.plume.oss.domain.exceptions.PlumeSchemaViolationException
import io.github.plume.oss.domain.mappers.VertexMapper.checkSchemaConstraints
import io.github.plume.oss.domain.mappers.VertexMapper.mapToVertex
import io.github.plume.oss.domain.models.PlumeGraph
import io.shiftleft.codepropertygraph.generated.EdgeTypes.AST
import io.shiftleft.codepropertygraph.generated.NodeTypes.FILE
import io.shiftleft.codepropertygraph.generated.NodeTypes.METHOD
import io.shiftleft.codepropertygraph.generated.nodes.NewNodeBuilder
import org.apache.logging.log4j.LogManager
import org.apache.tinkerpop.shaded.jackson.databind.ObjectMapper
import org.neo4j.driver.AuthTokens
import org.neo4j.driver.Driver
import org.neo4j.driver.GraphDatabase
import org.neo4j.driver.Value
import scala.jdk.CollectionConverters
import java.util.*


/**
 * The driver used to connect to a remote Neo4j instance.
 */
class Neo4jDriver : IDriver {

    private val logger = LogManager.getLogger(Neo4jDriver::class.java)
    private val objectMapper = ObjectMapper()
    private var connected = false
    private lateinit var driver: Driver

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
        val attributes = mutableMapOf<String, Any>()
        propertyMap.computeIfPresent("DYNAMIC_TYPE_HINT_FULL_NAME") { _, value ->
            when (value) {
                is scala.collection.immutable.`$colon$colon`<*> -> value.head()
                else -> value
            }
        }
        propertyMap.forEach {
            val key: Optional<String> = when (it.key) {
                "PARSER_TYPE_NAME" -> Optional.empty()
                "AST_PARENT_TYPE" -> Optional.empty()
                "AST_PARENT_FULL_NAME" -> Optional.empty()
                "FILENAME" -> Optional.empty()
                "IS_EXTERNAL" -> Optional.empty()
                else -> Optional.of(it.key)
            }
            if (key.isPresent) attributes[key.get()] = if (it.value is Int) (it.value as Int).toLong() else it.value
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

    override fun exists(v: NewNodeBuilder): Boolean {
        val node = v.build()
        driver.session().use { session ->
            return session.writeTransaction { tx ->
                val result = tx.run(
                    """
                    MATCH (n:${node.label()})
                    WHERE id(n) = ${v.id()}
                    RETURN n
                    """.trimIndent()
                )
                result.list().isNotEmpty()
            }
        }
    }

    override fun exists(fromV: NewNodeBuilder, toV: NewNodeBuilder, edge: EdgeLabel): Boolean {
        if (!exists(fromV) || !exists(toV)) return false
        val src = fromV.build()
        val tgt = toV.build()
        driver.session().use { session ->
            return session.writeTransaction { tx ->
                val result = tx.run(
                    """
                    MATCH (a:${src.label()}), (b:${tgt.label()})
                    WHERE id(a) = ${fromV.id()} AND id(b) = ${toV.id()}
                    RETURN EXISTS ((a)-[:$edge]->(b)) as edge_exists
                    """.trimIndent()
                )
                result.next()["edge_exists"].toString() == "TRUE"
            }
        }
    }

    override fun addEdge(fromV: NewNodeBuilder, toV: NewNodeBuilder, edge: EdgeLabel) {
        if (!checkSchemaConstraints(fromV, toV, edge)) throw PlumeSchemaViolationException(fromV, toV, edge)
        if (!exists(fromV)) addVertex(fromV)
        if (!exists(toV)) addVertex(toV)
        val src = fromV.build()
        val tgt = toV.build()
        driver.session().use { session ->
            return session.writeTransaction { tx ->
                val result = tx.run(
                    """
                    MATCH (a:${src.label()}), (b:${tgt.label()})
                    WHERE id(a) = ${fromV.id()} AND id(b) = ${toV.id()}
                    CREATE (a)-[r:$edge]->(b)
                    RETURN r
                    """.trimIndent()
                )
                result.list().isNotEmpty()
            }
        }
    }

    override fun maxOrder(): Int =
        TODO("Not yet implemented")

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

    override fun getWholeGraph(): PlumeGraph {
        val plumeGraph = PlumeGraph()
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
                .forEach { plumeGraph.addVertex(it) }
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
                }.forEach { plumeGraph.addEdge(it.first, it.second, EdgeLabel.valueOf(it.third)) }
        }
        return plumeGraph
    }

    override fun getMethod(fullName: String, signature: String, includeBody: Boolean): PlumeGraph {
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
        val plumeGraph = PlumeGraph()
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
                neo4jToPlumeGraph(result, plumeGraph)
            }
        }
        return plumeGraph
    }

    override fun getProgramStructure(): PlumeGraph {
        val plumeGraph = PlumeGraph()
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
            neo4jToPlumeGraph(result, plumeGraph)
        }
        return plumeGraph
    }

    override fun getNeighbours(v: NewNodeBuilder): PlumeGraph {
        val plumeGraph = PlumeGraph()
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
            neo4jToPlumeGraph(result, plumeGraph)
        }
        return plumeGraph
    }

    private fun neo4jToPlumeGraph(
        result: List<Value>,
        plumeGraph: PlumeGraph
    ) {
        result.map { r -> Triple(r["src"].asNode(), r["tgt"].asNode(), r["rel"].asString()) }
            .map { p ->
                Triple(
                    mapToVertex(p.first.asMap() + mapOf("id" to p.first.id())),
                    mapToVertex(p.second.asMap() + mapOf("id" to p.second.id())),
                    p.third
                )
            }.forEach { plumeGraph.addEdge(it.first, it.second, EdgeLabel.valueOf(it.third)) }
    }

    override fun deleteVertex(v: NewNodeBuilder) {
        driver.session().use { session ->
            session.writeTransaction { tx ->
                tx.run(
                    """
                    MATCH (n)
                    WHERE ID(n) = ${v.id()}
                    DETACH DELETE n
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
