package za.ac.sun.plume

import za.ac.sun.plume.TestDomainResources.Companion.vertices
import za.ac.sun.plume.domain.enums.EdgeLabel
import za.ac.sun.plume.domain.enums.VertexLabel
import za.ac.sun.plume.domain.mappers.VertexMapper
import java.util.HashSet

fun main() {
    fun generateTigerGraphEdges() {
        val schemaLines = HashSet<String>();
        VertexLabel.values().forEach { v1 ->
            EdgeLabel.values().forEach { e ->
                VertexLabel.values().forEach { v2 ->
                    if (VertexMapper.checkSchemaConstraints(v1, e, v2)) {
                        schemaLines.add("CREATE DIRECTED EDGE ${v1}_$v2 (FROM ${v1}_VERT, TO ${v2}_VERT, name STRING)")
                    }
                }
            }
        }
        schemaLines.toList().sorted().forEach(::println)
    }

    fun generateTigerGraphVertices() {
        vertices.forEach { v ->
            val props =  VertexMapper.vertexToMap(v)
            val sb = StringBuilder("CREATE VERTEX ${props.remove("label")}_VERT (PRIMARY_ID id INT,")
            VertexMapper.vertexToMap(v).keys.filter { it != "label" }.map { if (it == "order") "astOrder" else it }.forEach { sb.append(" $it TYPE,") }
            println(sb.append(")").toString().replace(",)", ")"))
        }
    }

    println("======\nTigerGraph Schema\n======\n")
    generateTigerGraphVertices()
    generateTigerGraphEdges()
}