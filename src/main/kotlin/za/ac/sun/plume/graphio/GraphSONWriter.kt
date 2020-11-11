package za.ac.sun.plume.graphio

import za.ac.sun.plume.domain.enums.EdgeLabel
import za.ac.sun.plume.domain.mappers.VertexMapper
import za.ac.sun.plume.domain.models.PlumeGraph
import za.ac.sun.plume.domain.models.PlumeVertex
import java.io.FileWriter
import java.util.*

object GraphSONWriter {

    private var propertyId: Int = 0

    fun write(graph: PlumeGraph, path: String) {
        propertyId = 0
        FileWriter(path).use { fw ->
            graph.vertices().forEach { v -> fw.write(vertexToJSON(v, graph) + "\n") }
        }
    }

    private fun vertexToJSON(v: PlumeVertex, graph: PlumeGraph): String {
        val sb = StringBuilder()
        val properties = VertexMapper.vertexToMap(v)
        sb.append("{")
        sb.append("\"id\":\"${v.hashCode()}\",")
        sb.append("\"label\":\"${properties.remove("label")}\",")
        graph.edgesOut(v).let { if (it.isNotEmpty()) sb.append("\"outE\":${edgesToJSON(it, "in")},") }
        graph.edgesIn(v).let { if (it.isNotEmpty()) sb.append("\"inE\":${edgesToJSON(it, "out")},") }
        properties.let { if (it.isNotEmpty()) sb.append("\"properties\":${propertiesToJSON(it)}") }
        sb.append("}")
        return sb.toString()
    }

    private fun edgesToJSON(edgesOut: HashMap<EdgeLabel, HashSet<PlumeVertex>>, tgtDirection: String): String {
        val sb = StringBuilder()
        sb.append("{")
        var i = 0
        edgesOut.forEach { (edgeLabel, vertexList) ->
            var j = 0
            sb.append("\"$edgeLabel\":[")
            vertexList.forEach {
                sb.append("{")
                sb.append("\"id\":{")
                sb.append("\"@type\":\"g:UUID\",")
                sb.append("\"@value\":\"${UUID.randomUUID()}\"")
                sb.append("}, \"${tgtDirection}V\":\"${it.hashCode()}\"")
                sb.append("}")
                if (++j < vertexList.size) sb.append(",")
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
            sb.append("\"id\":{\"@type\":\"g:Int64\",\"@value\":${propertyId++}},")
            when (v) {
                is String -> {
                    sb.append("\"value\":\"$v\"")
                }
                is Int -> {
                    sb.append("\"value\":{\"@type\":\"g:Int32\",\"@value\":$v}")
                }
            }
            sb.append("}]")
            if (++i < properties.size) sb.append(",")
        }
        sb.append("}")
        return sb.toString()
    }

}