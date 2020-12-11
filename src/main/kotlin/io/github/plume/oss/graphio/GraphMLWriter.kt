package io.github.plume.oss.graphio

import io.github.plume.oss.domain.mappers.VertexMapper
import io.github.plume.oss.domain.models.PlumeGraph
import io.github.plume.oss.domain.models.PlumeVertex
import java.io.OutputStreamWriter
import java.util.*

/**
 * Responsible for writing [PlumeGraph] objects to an [OutputStreamWriter] in GraphML format.
 */
object GraphMLWriter {

    private const val DECLARATION = "<?xml version=\"1.0\" ?>"

    /**
     * Given a [PlumeGraph] object, serializes it to [GraphML](http://graphml.graphdrawing.org/specification/dtd.html)
     * format to the given [OutputStreamWriter]. This format is supported by
     * [TinkerGraph](https://tinkerpop.apache.org/docs/current/reference/#graphml) and
     * [Cytoscape](http://manual.cytoscape.org/en/stable/Supported_Network_File_Formats.html#graphml).
     *
     * @param graph The [PlumeGraph] to serialize.
     * @param writer The stream to write the serialized graph to.
     */
    fun write(graph: PlumeGraph, writer: OutputStreamWriter) {
        writer.use { w ->
            // Write header
            w.write(DECLARATION)
            w.write("<graphml ")
            w.write("xmlns=\"http://graphml.graphdrawing.org/xmlns\" ")
            w.write("xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" ")
            w.write("xsi:schemaLocation=\"http://graphml.graphdrawing.org/xmlns http://graphml.graphdrawing.org/xmlns/1.1/graphml.xsd\">")
            // Write keys
            writeKeys(w, graph.vertices())
            // Write graph
            w.write("<graph id=\"G\" edgedefault=\"directed\">")
            // Write vertices
            writeVertices(w, graph.vertices())
            // Write edges
            writeEdges(w, graph)
            // Close graph tags
            w.write("</graph>")
            w.write("</graphml>")
        }
    }

    private fun writeKeys(fw: OutputStreamWriter, vertices: Set<PlumeVertex>) {
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

    private fun writeVertices(fw: OutputStreamWriter, vertices: Set<PlumeVertex>) {
        vertices.forEach { v ->
            fw.write("<node id=\"${v.hashCode()}\">")
            VertexMapper.vertexToMap(v)
                .apply { fw.write("<data key=\"labelV\">${this.remove("label")}</data>") }
                .forEach { (t, u) -> fw.write("<data key=\"$t\">${escape(u)}</data>") }
            fw.write("</node>")
        }
    }

    private fun escape(o: Any) = if (o is String) o.replace("<", "&lt;").replace(">", "&gt;") else o.toString()

    private fun writeEdges(fw: OutputStreamWriter, graph: PlumeGraph) {
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