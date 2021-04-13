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

import io.github.plume.oss.domain.mappers.ListMapper
import io.github.plume.oss.domain.mappers.VertexMapper
import io.github.plume.oss.domain.model.DeltaGraph
import io.github.plume.oss.metrics.ExtractorTimeKey
import io.github.plume.oss.metrics.PlumeTimer
import io.github.plume.oss.store.LocalCache
import io.github.plume.oss.store.PlumeStorage
import io.shiftleft.codepropertygraph.generated.nodes.NewNodeBuilder
import org.apache.logging.log4j.LogManager
import org.apache.tinkerpop.gremlin.driver.Cluster
import org.apache.tinkerpop.gremlin.driver.remote.DriverRemoteConnection
import org.apache.tinkerpop.gremlin.process.traversal.AnonymousTraversalSource.traversal
import org.apache.tinkerpop.gremlin.process.traversal.Order
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal
import org.apache.tinkerpop.gremlin.process.traversal.step.util.WithOptions
import org.apache.tinkerpop.gremlin.structure.Graph
import org.apache.tinkerpop.gremlin.structure.T
import org.apache.tinkerpop.gremlin.structure.Vertex
import overflowdb.Node
import scala.collection.immutable.`$colon$colon`
import scala.collection.immutable.`Nil$`
import scala.jdk.CollectionConverters
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.`__` as un


/**
 * The driver used to connect to a remote Amazon Neptune instance.
 */
class NeptuneDriver internal constructor() : GremlinDriver() {
    private val logger = LogManager.getLogger(NeptuneDriver::class.java)

    private val builder: Cluster.Builder = Cluster.build()
    private lateinit var cluster: Cluster
    private val idMapper = mutableMapOf<Long, String>()
    private var id: Long = 0

    init {
        builder.port(DEFAULT_PORT).enableSsl(true)
    }

    /**
     * Add one or more the addresses of a Gremlin Servers to the list of servers a Client will try to contact to send
     * requests to. The address should be parseable by InetAddress.getByName(String). That's the only validation
     * performed at this point. No connection to the host is attempted.
     *
     * @param addresses the address(es) of Gremlin Servers to contact.
     */
    fun addHostnames(vararg addresses: String): NeptuneDriver = apply { builder.addContactPoints(*addresses) }

    /**
     * Set the port for the Neptune Gremlin server. Default port number is 8182.
     *
     * @param port the port number e.g. 8182
     */
    fun port(port: Int): NeptuneDriver = apply { builder.port(port) }

    /**
     * Sets the certificate to use by the [Cluster].
     *
     * @param keyChainFile The X.509 certificate chain file in PEM format.
     */
    @Suppress("DEPRECATION")
    fun keyCertChainFile(keyChainFile: String): NeptuneDriver = apply { builder.keyCertChainFile(keyChainFile) }

    /**
     * Connects to the graph database with the given configuration.
     * See [Amazon Documentation](https://docs.aws.amazon.com/neptune/latest/userguide/access-graph-gremlin-java.html).
     *
     * @throws IllegalArgumentException if the graph database is already connected.
     */
    override fun connect(): NeptuneDriver = apply {
        require(!connected) { "Please close the graph before trying to make another connection." }
        cluster = builder.create()
        super.g = traversal().withRemote(DriverRemoteConnection.using(cluster))
        graph = g.graph
        connected = true
        populateIdMapper()
    }

    private fun resetIdMapper() {
        idMapper.clear()
        idMapper[-1L] = "null"
    }

    /**
     * When connecting to a database with a subgraph already loaded, create a mapping for existing graph data.
     */
    private fun populateIdMapper() {
        resetIdMapper()
        val vCount = g.V().count().next()
        var inc = 0L
        val loadedIds = idMapper.values.toSet()
        (1..vCount).chunked(10000).map { Pair(it.minOrNull() ?: 0L, it.maxOrNull() ?: 10000) }
            .flatMap { (l, h) -> g.V().order().by(T.id, Order.asc).range(l, h).id().toList().map { it.toString() } }
            .filterNot(loadedIds::contains)
            .forEach { id -> idMapper[inc++] = id }
        id = idMapper.keys.maxOrNull() ?: 0L
    }

    /**
     * Attempts to close the graph database connection and resources.
     *
     * @throws IllegalArgumentException if one attempts to close an already closed graph.
     */
    override fun close() {
        require(connected) { "Cannot close a graph that is not already connected!" }
        try {
            cluster.close()
            connected = false
        } catch (e: Exception) {
            logger.warn("Exception thrown while attempting to close graph.", e)
        } finally {
            // Have to also clear the cache otherwise the IDs won't be mapped correctly
            LocalCache.clear()
            PlumeStorage.clear()
            resetIdMapper()
        }
    }

    override fun findVertexTraversal(v: NewNodeBuilder): GraphTraversal<Vertex, Vertex> {
        var result: GraphTraversal<Vertex, Vertex>? = null
        PlumeTimer.measure(ExtractorTimeKey.DATABASE_READ) {
            val strId = idMapper[v.id()]
            result = if (strId != null) g.V(strId)
            else g.V("null")
        }
        return result!!
    }

    /**
     * Given a [NewNodeBuilder], creates a [Vertex] and translates the object's field properties to key-value
     * pairs on the [Vertex] object. This is then added to this driver's [Graph].
     *
     * @param v the [NewNodeBuilder] to translate into a [Vertex].
     * @return the newly created [Vertex].
     */
    override fun createVertex(v: NewNodeBuilder): Vertex {
        val propertyMap = prepareVertexProperties(v)
        var traversalPointer = g.addV(v.build().label())
        for ((key, value) in propertyMap) traversalPointer = traversalPointer.property(key, value)
        return traversalPointer.next().apply {
            idMapper[id++] = this.id().toString()
        }
    }

    override fun deleteVertex(id: Long, label: String?) {
        val mappedId = idMapper[id]
        var res = false
        PlumeTimer.measure(ExtractorTimeKey.DATABASE_READ) { res = g.V(mappedId).hasNext() }
        if (!res) return
        PlumeTimer.measure(ExtractorTimeKey.DATABASE_WRITE) { g.V(mappedId).drop().iterate() }
        idMapper.remove(id)
    }

    override fun deleteEdge(src: NewNodeBuilder, tgt: NewNodeBuilder, edge: String) {
        if (!exists(src, tgt, edge)) return
        PlumeTimer.measure(ExtractorTimeKey.DATABASE_WRITE) {
            val srcId = idMapper[src.id()]
            val dstId = idMapper[tgt.id()]
            g.V(srcId).outE(edge).where(un.otherV().hasId(dstId)).drop().iterate()
        }
    }

    // This handles Neptune -> ODB
    override fun mapVertexKeys(props: Map<Any, Any>): Map<String, Any> {
        val outM = mutableMapOf<String, Any>()
        props.filterKeys { it != T.id }.mapKeys { it.key.toString() }.toMap(outM)
        val id = props.getOrDefault(T.id, "null")
        idMapper.values.find { it == id }?.let { idL -> outM["id"] = idL }
        println("------------")
        println(props)
        println(outM)
        return outM
    }

    // This handles ODB -> Neptune
    override fun prepareVertexProperties(v: NewNodeBuilder): Map<String, Any> {
        val outMap = CollectionConverters.MapHasAsJava(v.build().properties()).asJava()
            .mapValues { (_, value) ->
                when (value) {
                    is `$colon$colon`<*> -> ListMapper.scalaListToString(value)
                    is `Nil$` -> ListMapper.scalaListToString(value)
                    else -> value
                }
            }.toMutableMap()
        if (outMap.containsKey("id")) {
            outMap["id"] = idMapper[outMap["id"]]
        }
        return outMap
    }

    override fun bulkTxReads(
        dg: DeltaGraph,
        vAdds: MutableList<NewNodeBuilder>,
        eAdds: MutableList<DeltaGraph.EdgeAdd>,
        vDels: MutableList<DeltaGraph.VertexDelete>,
        eDels: MutableList<DeltaGraph.EdgeDelete>,
    ) {
        dg.changes.filterIsInstance<DeltaGraph.VertexAdd>().map { it.n }.toCollection(vAdds)
        dg.changes.filterIsInstance<DeltaGraph.EdgeAdd>().toCollection(eAdds)
        dg.changes.filterIsInstance<DeltaGraph.VertexDelete>().filter { g.V(idMapper[it.id]).hasNext() }
            .toCollection(vDels)
        dg.changes.filterIsInstance<DeltaGraph.EdgeDelete>().filter { exists(it.src, it.dst, it.e) }
            .toCollection(eDels)
    }

    override fun updateVertexProperty(id: Long, label: String?, key: String, value: Any) {
        var res = false
        PlumeTimer.measure(ExtractorTimeKey.DATABASE_READ) { res = g.V(idMapper[id]).hasNext() }
        if (!res) return
        PlumeTimer.measure(ExtractorTimeKey.DATABASE_WRITE) { g.V(idMapper[id]).property(key, value).iterate() }
    }

    override fun getWholeGraph(): overflowdb.Graph {
        val graph = newOverflowGraph()
        PlumeTimer.measure(ExtractorTimeKey.DATABASE_READ) {
            g.V().valueMap<Any>()
                .by(un.unfold<Any>())
                .with(WithOptions.tokens).toList()
                .map { VertexMapper.mapToVertex(mapVertexKeys(it)) }
                .forEach { addNodeToODB(graph, it) }
            g.E().toList()
                .map { e->
                    Triple(
                        graph.node(idMapper.entries.find { it.value == e.outVertex().id() }!!.key),
                        graph.node(idMapper.entries.find { it.value == e.inVertex().id() }!!.key),
                        e.label()
                    )
                }
                .forEach { (src, dst, e) ->
                    src?.addEdge(e, dst)
                }
        }
        return graph
    }

    override fun clearGraph(): GremlinDriver {
        resetIdMapper()
        return super.clearGraph()
    }

    companion object {
        /**
         * Default port number a remote Gremlin server.
         */
        private const val DEFAULT_PORT = 8182
    }
}