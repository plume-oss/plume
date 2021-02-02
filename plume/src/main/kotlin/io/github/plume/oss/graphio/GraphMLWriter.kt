package io.github.plume.oss.graphio

import io.github.plume.oss.domain.mappers.VertexMapper
import overflowdb.Graph
import overflowdb.Node
import scala.collection.immutable.`$colon$colon`
import scala.collection.immutable.`Nil$`
import java.io.OutputStreamWriter
import java.util.*

/**
 * Responsible for writing [Graph] objects to an [OutputStreamWriter] in GraphML format.
 */
object GraphMLWriter {

    private const val DECLARATION = "<?xml version=\"1.0\" ?>"
    private var edgeId: Long = 0

    /**
     * Given a [Graph] object, serializes it to [GraphML](http://graphml.graphdrawing.org/specification/dtd.html)
     * format to the given [OutputStreamWriter]. This format is supported by
     * [TinkerGraph](https://tinkerpop.apache.org/docs/current/reference/#graphml) and
     * [Cytoscape](http://manual.cytoscape.org/en/stable/Supported_Network_File_Formats.html#graphml).
     *
     * @param g The [Graph] to serialize.
     * @param writer The stream to write the serialized graph to.
     */
    fun write(g: Graph, writer: OutputStreamWriter) {
        writer.use { w ->
            // Write header
            w.write(DECLARATION)
            w.write("<graphml ")
            w.write("xmlns=\"http://graphml.graphdrawing.org/xmlns\" ")
            w.write("xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" ")
            w.write("xsi:schemaLocation=\"http://graphml.graphdrawing.org/xmlns http://graphml.graphdrawing.org/xmlns/1.1/graphml.xsd\">")
            // Write keys
            writeKeys(w, g.nodes())
            // Write graph
            w.write("<graph id=\"G\" edgedefault=\"directed\">")
            // Write vertices
            writeVertices(w, g.nodes())
            // Write edges
            writeEdges(w, g)
            // Close graph tags
            w.write("</graph>")
            w.write("</graphml>")
        }
    }

    private fun writeKeys(fw: OutputStreamWriter, vertices: MutableIterator<Node>) {
        val keySet = HashMap<String, String>()
        vertices.forEach { v ->
            v.propertyMap().forEach { (t, u) ->
                when (u) {
                    is String -> keySet[t] = "string"
                    is Int -> keySet[t] = "int"
                    is `$colon$colon`<*> -> keySet[t] = "string"
                    is `Nil$` -> keySet[t] = "string"
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

    private fun writeVertices(fw: OutputStreamWriter, vertices: MutableIterator<Node>) {
        vertices.forEach { v ->
            fw.write("<node id=\"${v.id()}\">")
            VertexMapper.extractAttributesFromMap(v.propertyMap())
                .apply { fw.write("<data key=\"labelV\">${v.label()}</data>") }
                .forEach { (t, u) -> fw.write("<data key=\"$t\">${escape(u)}</data>") }
            fw.write("</node>")
        }
    }

    private fun escape(o: Any) =
        when (o) {
            is String -> o.replace("<", "&lt;").replace(">", "&gt;")
            is `$colon$colon`<*> -> o.head()
            is `Nil$` -> ""
            else -> o.toString()
        }

    private fun writeEdges(fw: OutputStreamWriter, g: Graph) {
        val vertices = g.nodes()
        vertices.forEach { srcV ->
            g.node(srcV.id()).outE().asSequence().map { Pair(it.label(), it.inNode().get()) }
                .forEach { (edgeLabel, tgtV) ->
                    fw.write("<edge id=\"${edgeId++}\" ")
                    fw.write("source=\"${srcV.id()}\" ")
                    fw.write("target=\"${tgtV.id()}\">")
                    fw.write("<data key=\"labelE\">${edgeLabel}</data>")
                    fw.write("</edge>")
                }
        }
    }
}