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
import io.github.plume.oss.domain.model.DeltaGraph
import io.github.plume.oss.metrics.DriverTimeKey
import io.github.plume.oss.metrics.PlumeTimer
import io.shiftleft.codepropertygraph.generated.nodes.NewMetaDataBuilder
import io.shiftleft.codepropertygraph.generated.nodes.NewNodeBuilder
import org.apache.logging.log4j.LogManager
import org.neo4j.dbms.api.DatabaseManagementService
import org.neo4j.dbms.api.DatabaseManagementServiceBuilder
import org.neo4j.driver.Value
import org.neo4j.driver.types.Node
import org.neo4j.graphdb.GraphDatabaseService
import overflowdb.Graph
import java.io.File
import kotlin.streams.toList

class EmbeddedNeo4jDriver internal constructor() : IDriver, CypherDriverQueries() {

    private val logger = LogManager.getLogger(EmbeddedNeo4jDriver::class.java)
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
     * Sets the directory to store serialized embedded graphs. Only used when embedded mode is true.
     *
     * @value value the directory to store the graphs.
     */
    fun databaseDirectory(value: String) = apply { databaseDirectory = value }

    fun connect(): EmbeddedNeo4jDriver = apply {
        mgmtService = DatabaseManagementServiceBuilder(File(databaseDirectory).toPath())
            .build()
        embeddedDb = mgmtService.database(database)
        registerShutdownHook(mgmtService)
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

    override fun close() {
        PlumeTimer.measure(DriverTimeKey.DISCONNECT_SERIALIZE) {
            require(connected) { "Cannot close a graph that is not already connected!" }
            try {
                mgmtService.shutdown()
            } catch (e: Exception) {
                logger.warn("Exception thrown while attempting to close graph.", e)
            } finally {
                connected = false
            }
        }
    }

    override fun addVertex(v: NewNodeBuilder) {
        if (exists(v)) return
        PlumeTimer.measure(DriverTimeKey.DATABASE_WRITE) {
            embeddedDb.beginTx().use { tx ->
                val result = tx.execute(addVertexCypher(v, 0))
                v.id(result.next()["id0"].toString().toLong())
            }
        }
    }

    override fun exists(v: NewNodeBuilder): Boolean = checkVertexExist(v.id(), v.build().label())

    private fun checkVertexExist(id: Long, label: String? = null): Boolean {
        var res = false
        PlumeTimer.measure(DriverTimeKey.DATABASE_READ) {
            embeddedDb.beginTx().use { tx ->
                val result = tx.execute(checkVertexExistCypher(id, label))
                res = !result.hasNext()
            }
        }
        return res
    }

    override fun exists(src: NewNodeBuilder, tgt: NewNodeBuilder, edge: String): Boolean {
        var res = false
        PlumeTimer.measure(DriverTimeKey.DATABASE_READ) { res = exists(src) && exists(tgt) }
        if (!res) return false
        PlumeTimer.measure(DriverTimeKey.DATABASE_READ) {
            res = embeddedDb.beginTx().use { tx ->
                val result = tx.execute(checkEdgeExistCypher(src, tgt, edge))
                result.next()["edge_exists"].toString() == "TRUE"
            }
        }
        return res
    }

    override fun addEdge(src: NewNodeBuilder, tgt: NewNodeBuilder, edge: String) {
        if (!checkSchemaConstraints(src, tgt, edge)) throw PlumeSchemaViolationException(src, tgt, edge)
        if (!exists(src)) addVertex(src)
        if (!exists(tgt)) addVertex(tgt)
        PlumeTimer.measure(DriverTimeKey.DATABASE_WRITE) {
            embeddedDb.beginTx().use { tx ->
                tx.execute(addEdgeCypher(src, tgt, edge))
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
                    try {
                        embeddedDb.beginTx().use { tx ->
                            val result = tx.execute(
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
                    try {
                        embeddedDb.beginTx().use { tx ->
                            tx.execute(
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
            // TODO: This can be bulk but deletes are currently very uncommon
            vDels.forEach { deleteVertex(it.id, it.label) }
            eDels.forEach { deleteEdge(it.src, it.dst, it.e) }
        }
    }

    override fun clearGraph(): IDriver {
        PlumeTimer.measure(DriverTimeKey.DATABASE_WRITE) {
            embeddedDb.beginTx().use { tx ->
                tx.execute(clearGraphCypher())
            }
        }
        return this
    }

    override fun getWholeGraph(): Graph {
        val graph = newOverflowGraph()
        PlumeTimer.measure(DriverTimeKey.DATABASE_READ) {
            val orphanVertices =
                embeddedDb.beginTx().use { tx -> tx.execute(getAllVerticesCypher()) }.columnAs<Node>("n")
            orphanVertices.map { VertexMapper.mapToVertex(it.asMap() + mapOf("id" to it.id())) }
                .forEach { addNodeToGraph(graph, it) }
            val edgeResult = embeddedDb.beginTx().use { tx -> tx.execute(getAllEdgesCypher()) }.asSequence().toList()
            edgeResult.map { r -> Triple(r["src"] as Node, r["tgt"] as Node, r["rel"].toString()) }
                .map {
                    Triple(
                        VertexMapper.mapToVertex(it.first.asMap() + mapOf("id" to it.first.id())),
                        VertexMapper.mapToVertex(it.second.asMap() + mapOf("id" to it.second.id())),
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

    override fun getMethod(fullName: String, includeBody: Boolean): Graph {
        val queryHead = getMethodQueryHead(fullName, includeBody)
        val plumeGraph = newOverflowGraph()
        PlumeTimer.measure(DriverTimeKey.DATABASE_READ) {
            embeddedDb.beginTx().use { tx ->
                val result = tx.execute(getMethodCypher(queryHead)).columnAs<Value>("x").asSequence().toList()
                neo4jToOverflowGraph(result, plumeGraph)
            }
        }
        return plumeGraph
    }

    override fun getProgramStructure(): Graph {
        val graph = newOverflowGraph()
        val result = embeddedDb.beginTx().use { tx ->
            tx.execute(getProgramStructureCypher1()).columnAs<Value>("x").asSequence().toList()
        }
        neo4jToOverflowGraph(result, graph)
        val typeDecl = embeddedDb.beginTx().use { tx ->
            tx.execute(getProgramStructureCypher2()).asSequence().toList()
        }
        typeDecl.flatMap { listOf(it["m"] as Node, it["n"] as Node, it["o"] as Node) }
            .map { VertexMapper.mapToVertex(it.asMap() + mapOf("id" to it.id())) }
            .filter { graph.node(it.id()) == null }
            .forEach { addNodeToGraph(graph, it) }
        return graph
    }

    override fun getNeighbours(v: NewNodeBuilder): Graph {
        val graph = newOverflowGraph()
        PlumeTimer.measure(DriverTimeKey.DATABASE_READ) {
            val result = embeddedDb.beginTx().use { tx ->
                tx.execute(getNeighboursCypher(v)).columnAs<Value>("x").asSequence().toList()
            }
            neo4jToOverflowGraph(result, graph)
        }
        return graph
    }

    override fun deleteVertex(id: Long, label: String?) {
        if (!checkVertexExist(id, label)) return
        PlumeTimer.measure(DriverTimeKey.DATABASE_WRITE) {
            embeddedDb.beginTx().use { tx ->
                tx.execute(deleteVertexCypher(id, label))
            }
        }
    }

    override fun deleteEdge(src: NewNodeBuilder, tgt: NewNodeBuilder, edge: String) {
        if (!exists(src, tgt, edge)) return
        PlumeTimer.measure(DriverTimeKey.DATABASE_WRITE) {
            embeddedDb.beginTx().use { tx ->
                tx.execute(deleteEdgeCypher(src, tgt, edge))
            }
        }
    }

    override fun deleteMethod(fullName: String) {
        PlumeTimer.measure(DriverTimeKey.DATABASE_WRITE) {
            embeddedDb.beginTx().use { tx ->
                tx.execute(deleteMethodCypher(fullName))
            }
        }
    }

    override fun updateVertexProperty(id: Long, label: String?, key: String, value: Any) {
        if (!checkVertexExist(id, label)) return
        PlumeTimer.measure(DriverTimeKey.DATABASE_WRITE) {
            embeddedDb.beginTx().use { tx ->
                tx.execute(updateVertexPropertyCypher(id, label, key, value))
            }
        }
    }

    override fun getMetaData(): NewMetaDataBuilder? {
        var meta: NewMetaDataBuilder? = null
        PlumeTimer.measure(DriverTimeKey.DATABASE_WRITE) {
            meta = embeddedDb.beginTx().use { tx ->
                tx.execute(getMetaDataCypher()).columnAs<Node>("n")
                    .map { VertexMapper.mapToVertex(it.asMap() + mapOf("id" to it.id())) }
                    .stream().toList().firstOrNull() as NewMetaDataBuilder?
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
        PlumeTimer.measure(DriverTimeKey.DATABASE_READ) {
            embeddedDb.beginTx().use { tx ->
                tx.execute(getVerticesByPropertyCypher(propertyKey, propertyValue, label))
                    .columnAs<Node>("n")
                    .map { VertexMapper.mapToVertex(it.asMap() + mapOf("id" to it.id())) }
            }.stream().toList().toCollection(l)
        }
        return l
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> getPropertyFromVertices(propertyKey: String, label: String?): List<T> {
        val l = mutableListOf<T>()
        if (propertyKey.length != sanitizePayload(propertyKey).length || propertyKey.contains("[<|>]".toRegex())) return emptyList()
        PlumeTimer.measure(DriverTimeKey.DATABASE_READ) {
            embeddedDb.beginTx().use { tx ->
                tx.execute(getPropertyFromVerticesCypher(propertyKey, label)).columnAs<T>("p")
            }.stream().toList().toCollection(l)
        }
        return l.toList()
    }

    override fun getVerticesOfType(label: String): List<NewNodeBuilder> {
        val l = mutableListOf<NewNodeBuilder>()
        PlumeTimer.measure(DriverTimeKey.DATABASE_READ) {
            embeddedDb.beginTx().use { tx ->
                tx.execute(getVerticesOfTypeCypher(label)).columnAs<Node>("n")
                    .map { VertexMapper.mapToVertex(it.asMap() + mapOf("id" to it.id())) }
            }.stream().toList().toCollection(l)
        }
        return l
    }

    companion object {
        /**
         * Default database name for the Neo4j server.
         */
        private const val DEFAULT_DATABASE = "neo4j"

        /**
         * Default directory for where an embedded database will be serialized to.
         */
        private const val DEFAULT_DATABASE_DIRECTORY = "."
    }
}