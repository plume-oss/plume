package io.github.plume.oss.graphio

import io.github.plume.oss.domain.enums.EdgeLabel
import io.github.plume.oss.domain.mappers.VertexMapper.extractAttributesFromMap
import io.github.plume.oss.domain.models.PlumeGraph
import io.shiftleft.codepropertygraph.generated.nodes.NewNodeBuilder
import scala.jdk.CollectionConverters
import java.io.OutputStreamWriter
import java.util.*

/**
 * Responsible for writing [PlumeGraph] objects to an [OutputStreamWriter] in GraphSON format. Note that GraphSON format
 * is a more space-expensive format.
 */
object GraphSONWriter {

    private var propertyId: Long = 0
    private var edgeId: Long = 0

    /**
     * Given a [PlumeGraph] object, serializes it to
     * [GraphSON](https://github.com/tinkerpop/blueprints/wiki/GraphSON-Reader-and-Writer-Libraryl)
     * format to the given [OutputStreamWriter]. This format is supported by
     * [TinkerGraph](https://tinkerpop.apache.org/docs/current/reference/#graphson) and suited for web responses.
     *
     * @param graph The [PlumeGraph] to serialize.
     * @param writer The stream to write the serialized graph to.
     */
    fun write(graph: PlumeGraph, writer: OutputStreamWriter) {
        propertyId = 0
        writer.use { w ->
            graph.vertices().forEach { v -> w.write(vertexToJSON(v, graph) + "\n") }
        }
    }

    private fun vertexToJSON(v: NewNodeBuilder, graph: PlumeGraph): String {
        val sb = StringBuilder()
        val builtNode = v.build()
        val properties = extractAttributesFromMap(CollectionConverters.MapHasAsJava(builtNode.properties()).asJava().toMutableMap())
        sb.append("{")
        sb.append("\"id\":\"${v.id()}\",")
        sb.append("\"label\":\"${builtNode.label()}\",")
        graph.edgesOut(v).let { if (it.isNotEmpty()) sb.append("\"outE\":${edgesToJSON(it, "in")},") }
        graph.edgesIn(v).let { if (it.isNotEmpty()) sb.append("\"inE\":${edgesToJSON(it, "out")},") }
        properties.let { if (it.isNotEmpty()) sb.append("\"properties\":${propertiesToJSON(it)}") }
        sb.append("}")
        return sb.toString()
    }

    private fun edgesToJSON(edgesOut: HashMap<EdgeLabel, HashSet<NewNodeBuilder>>, tgtDirection: String): String {
        val sb = StringBuilder()
        sb.append("{")
        var i = 0
        edgesOut.forEach { (edgeLabel, vertexList) ->
            sb.append("\"$edgeLabel\":[")
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
        properties.forEach { (k, v) ->
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