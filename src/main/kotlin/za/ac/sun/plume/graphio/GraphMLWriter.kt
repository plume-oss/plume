package za.ac.sun.plume.graphio

import za.ac.sun.plume.domain.mappers.VertexMapper
import za.ac.sun.plume.domain.models.PlumeGraph
import za.ac.sun.plume.domain.models.PlumeVertex
import java.io.FileWriter
import java.util.*

object GraphMLWriter {

    private const val DECLARATION = "<?xml version=\"1.0\" ?>"

    fun write(graph: PlumeGraph, path: String) {
        FileWriter(path).use { fw ->
            // Write header
            fw.write(DECLARATION)
            fw.write("<graphml ")
            fw.write("xmlns=\"http://graphml.graphdrawing.org/xmlns\" ")
            fw.write("xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" ")
            fw.write("xsi:schemaLocation=\"http://graphml.graphdrawing.org/xmlns http://graphml.graphdrawing.org/xmlns/1.1/graphml.xsd\">")
            // Write keys
            writeKeys(fw, graph.vertices())
            // Write graph
            fw.write("<graph id=\"G\" edgedefault=\"directed\">")
            // Write vertices
            writeVertices(fw, graph.vertices())
            // Write edges
            writeEdges(fw, graph)
            // Close graph tags
            fw.write("</graph>")
            fw.write("</graphml>")
        }
    }

    private fun writeKeys(fw: FileWriter, vertices: Set<PlumeVertex>) {
        val keySet = HashMap<String, String>()
        vertices.forEach { v ->
            VertexMapper.vertexToMap(v)
                    .apply { this.remove("label") }
                    .forEach { (t, u) ->
                        when (u) {
                            is String -> keySet[t] = "string"
                            is Int -> keySet[t] = "int"
                        }
                    }
        }
        fw.write("<key id=\"labelV\" for=\"node\" attr.name=\"labelV\" attr.type=\"string\"></key>")
        fw.write("<key id=\"labelE\" for=\"edge\" attr.name=\"labelE\" attr.type=\"string\"></key>")
        keySet.forEach { (t, u) ->
            fw.write("<key ")
            fw.write("id=\"$t\" ")
            fw.write("for=\"node\" ")
            fw.write("attr.name=\"$t\" ")
            fw.write("attr.type=\"$u\">")
            fw.write("</key>")
        }
    }

    private fun writeVertices(fw: FileWriter, vertices: Set<PlumeVertex>) {
        vertices.forEach { v ->
            fw.write("<node id=\"${v.hashCode()}\">")
            VertexMapper.vertexToMap(v)
                    .apply { fw.write("<data key=\"labelV\">${this.remove("label")}</data>") }
                    .forEach { (t, u) -> fw.write("<data key=\"$t\">$u</data>") }
            fw.write("</node>")
        }
    }

    private fun writeEdges(fw: FileWriter, graph: PlumeGraph) {
        val vertices = graph.vertices()
        vertices.forEach { srcV ->
            graph.edgesOut(srcV).forEach { (edgeLabel, vOut) ->
                vOut.forEach { tgtV ->
                    fw.write("<edge id=\"${UUID.randomUUID()}\" ")
                    fw.write("source=\"${srcV.hashCode()}\" ")
                    fw.write("target=\"${tgtV.hashCode()}\">")
                    fw.write("<data key=\"labelE\">${edgeLabel}</data>")
                    fw.write("</edge>")
                }
            }
        }
    }
}