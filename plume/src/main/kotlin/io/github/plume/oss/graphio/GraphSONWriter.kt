package io.github.plume.oss.graphio

import io.github.plume.oss.domain.mappers.VertexMapper.extractAttributesFromMap
import overflowdb.Graph
import overflowdb.Node
import scala.collection.immutable.`Nil$`
import java.io.OutputStreamWriter
import java.util.*

/**
 * Responsible for writing [Graph] objects to an [OutputStreamWriter] in GraphSON format. Note that GraphSON format
 * is a more space-expensive format.
 */
object GraphSONWriter {

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
            graph.nodes().forEach { v -> w.write(vertexToJSON(v, graph) + "\n") }
        }
    }

    private fun vertexToJSON(v: Node, graph: Graph): String {
        val sb = StringBuilder()
        val properties = extractAttributesFromMap(v.propertyMap())
        sb.append("{")
        sb.append("\"id\":\"${v.id()}\",")
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
        val edgesOut = mutableMapOf<String, MutableList<Node>>()
        val es = if (tgtDirection == "in") node.inE() else node.outE()

        es.forEach {
            val eList = edgesOut[it.label()]
            if (eList.isNullOrEmpty()) edgesOut[it.label()] = mutableListOf(it.inNode().get())
            else eList.add(it.inNode().get())
        }

        edgesOut.forEach { (edge, vertexList) ->
            sb.append("\"$edge\":[")
            vertexList.forEachIndexed { i, v ->
                sb.append("{")
                sb.append("\"id\":{")
                sb.append("\"@type\":\"g:Int64\",")
                sb.append("\"@value\":\"${edgeId++}\"")
                sb.append("}, \"${tgtDirection}V\":\"${v.id()}\"")
                sb.append("}")
                if (i < vertexList.size - 1) sb.append(",")
            }
            sb.append("]")
            if (++i < edgesOut.size) sb.append(",")
        }
        sb.append("}")
        return sb.toString()
    }

    private fun propertiesToJSON(properties: MutableMap<String, Any>): String {
        val sb = StringBuilder()
        sb.append("{")
        var i = 0
        extractAttributesFromMap(properties).forEach { (k, v) ->
            sb.append("\"$k\":[{")
            sb.append("\"id\":{\"@type\":\"g:Int64\",\"@value\":${propertyId++}}")
            when (v) {
                is String -> sb.append(",\"value\":\"$v\"")
                is Int -> sb.append(",\"value\":{\"@type\":\"g:Int32\",\"@value\":$v}")
                is Long -> sb.append(",\"value\":{\"@type\":\"g:Int64\",\"@value\":$v}")
                else -> println("Unsupported type $v ${v.javaClass}")
            }
            sb.append("}]")
            if (++i < properties.size) sb.append(",")
        }
        sb.append("}")
        return sb.toString()
    }

}