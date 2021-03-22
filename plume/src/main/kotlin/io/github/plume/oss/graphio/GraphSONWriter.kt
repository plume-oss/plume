package io.github.plume.oss.graphio

import io.github.plume.oss.domain.mappers.VertexMapper.prepareListsInMap
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import overflowdb.Graph
import overflowdb.Node
import java.io.OutputStreamWriter

/**
 * Responsible for writing [Graph] objects to an [OutputStreamWriter] in GraphSON format. Note that GraphSON format
 * is a more space-expensive format.
 */
object GraphSONWriter {

    private val logger: Logger = LogManager.getLogger(GraphSONWriter::javaClass)
    private var propertyId: Long = 0
    private var edgeId: Long = 0

    /**
     * Given a [Graph] object, serializes it to
     * [GraphSON](https://github.com/tinkerpop/blueprints/wiki/GraphSON-Reader-and-Writer-Libraryl)
     * format to the given [OutputStreamWriter]. This format is supported by
     * [TinkerGraph](https://tinkerpop.apache.org/docs/current/reference/#graphson) and suited for web responses.
     *
     * @param graph The [Graph] to serialize.
     * @param writer The stream to write the serialized graph to.
     */
    fun write(graph: Graph, writer: OutputStreamWriter) {
        propertyId = 0
        writer.use { w ->
            graph.nodes().forEach { v ->
                w.write(vertexToJSON(v, graph) + "\n")
            }
        }
    }

    private fun vertexToJSON(v: Node, graph: Graph): String {
        val sb = StringBuilder()
        val properties = prepareListsInMap(v.propertyMap())
        sb.append("{")
        sb.append("\"id\":{\"@type\":\"g:Int64\",\"@value\":${v.id()}},")
        sb.append("\"label\":\"${v.label()}\",")
        val node = graph.V(v.id()).next()
        if (node.outE().hasNext()) sb.append("\"outE\":${edgesToJSON(node, "in")},")
        if (node.inE().hasNext()) sb.append("\"inE\":${edgesToJSON(node, "out")},")
        properties.let { if (it.isNotEmpty()) sb.append("\"properties\":${propertiesToJSON(it)}") }
        sb.append("}")
        return sb.toString()
    }

    private fun edgesToJSON(node: Node, tgtDirection: String): String {
        val sb = StringBuilder()
        sb.append("{")
        var i = 0
        val edges = mutableMapOf<String, MutableList<Node>>()
        val es = if (tgtDirection == "out") node.inE() else node.outE()

        if (tgtDirection == "in") {
            es.forEach {
                val eList = edges[it.label()]
                if (eList.isNullOrEmpty()) edges[it.label()] = mutableListOf(it.inNode().get())
                else eList.add(it.inNode().get())
            }
        } else {
            es.forEach {
                val eList = edges[it.label()]
                if (eList.isNullOrEmpty()) edges[it.label()] = mutableListOf(it.outNode().get())
                else eList.add(it.outNode().get())
            }
        }

        edges.forEach { (edge, vertexList) ->
            sb.append("\"$edge\":[")
            vertexList.forEachIndexed { i, v ->
                sb.append("{")
                sb.append("\"id\":{")
                sb.append("\"@type\":\"g:Int64\",")
                sb.append("\"@value\":${edgeId++}")
                sb.append("},")
                sb.append("\"${tgtDirection}V\":{")
                sb.append("\"@type\":\"g:Int64\",")
                sb.append("\"@value\":${v.id()}}")
                sb.append("}")
                if (i < vertexList.size - 1) sb.append(",")
            }
            sb.append("]")
            if (++i < edges.size) sb.append(",")
        }
        sb.append("}")
        return sb.toString()
    }

    private fun propertiesToJSON(properties: MutableMap<String, Any>): String {
        val sb = StringBuilder()
        sb.append("{")
        var i = 0
        prepareListsInMap(properties).forEach { (k, v) ->
            sb.append("\"$k\":[{")
            sb.append("\"id\":{\"@type\":\"g:Int64\",\"@value\":${propertyId++}}")
            when (v) {
                is String -> sb.append(",\"value\":\"$v\"")
                is Int -> sb.append(",\"value\":{\"@type\":\"g:Int32\",\"@value\":$v}")
                is Long -> sb.append(",\"value\":{\"@type\":\"g:Int64\",\"@value\":$v}")
                is Boolean -> sb.append(",\"value\":$v")
                else -> logger.warn("Unsupported type $v ${v.javaClass}")
            }
            sb.append("}]")
            if (++i < properties.size) sb.append(",")
        }
        sb.append("}")
        return sb.toString()
    }

}