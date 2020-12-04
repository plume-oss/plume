package za.ac.sun.plume.drivers

import io.shiftleft.codepropertygraph.generated.nodes.*
import overflowdb.Config
import overflowdb.Graph
import scala.Option
import scala.Some
import za.ac.sun.plume.domain.enums.EdgeLabel
import za.ac.sun.plume.domain.models.PlumeGraph
import za.ac.sun.plume.domain.models.PlumeVertex
import scala.collection.immutable.`List$`
import za.ac.sun.plume.domain.models.vertices.*

/**
 * Driver to create an overflowDB database file from Plume's domain classes.
 *
 * TODO: the Plume domain classes and those provided by io.shiftleft.codepropertygraph
 * are so similar that it is worth investigating whether they can be used as a
 * replacement for Plume's domain classes. The advantage would be that (a) the
 * importer is backed by disk, meaning that we do not run into memory pressure for
 * large input programs, and (b) that no conversion from the Plume domain classes
 * is necessary when exporting to overflowdb.
 * */
class OverflowDbDriver : IDriver {

    private var graph : Graph = createEmptyGraph()

    var dbfilename: String = ""
        private set

    fun dbfilename(value: String) = apply { dbfilename = value }

    fun connect() {
        graph = createEmptyGraph()
    }

    private fun createEmptyGraph() : Graph {
        val odbConfig = if (dbfilename != "") {
            Config.withDefaults().withStorageLocation(dbfilename)
        } else {
            Config.withDefaults()
        }
        return Graph.open(odbConfig,
                io.shiftleft.codepropertygraph.generated.nodes.Factories.allAsJava(),
                io.shiftleft.codepropertygraph.generated.edges.Factories.allAsJava())
    }

    override fun addVertex(v: PlumeVertex) {
        val id = v.hashCode()
        val node = convert(v)
        val newNode = graph.addNode(id.toLong(), node.label())
        node.properties().foreachEntry { key, value ->
            newNode.setProperty(key, value)
        }
    }

    private fun convert(v : PlumeVertex) : NewNode {
       return when(v) {
           is BindingVertex -> NewBinding(v.name, v.signature, Option.empty())
           // TODO this highlights a problem: since we can't make use of scala
           // default parameters from kotlin, it becomes painfully obvious that
           // we're dealing with cpg-internal here. We should make the necessary
           // extensions of fields such as `MetaData` at the SL side later in
           // the process to reduce the number of fields that need to be set in
           // OSS components despite not being meaningful there.
           is MetaDataVertex -> NewMetaData(v.language, v.version,
                   `List$`.`MODULE$`.empty(),
                   `List$`.`MODULE$`.empty(),
                   Some(""),
                   Option.empty())
           is TypeVertex -> NewType(v.name, v.fullName, v.typeDeclFullName)
           is ArrayInitializerVertex -> NewArrayInitializer(Option.empty(), Option.empty(), "",
                   v.order, -1 ,
                   Option.empty(),
                   Option.empty())
           is ControlStructureVertex -> NewControlStructure(v.code, Some(v.columnNumber), Some(v.lineNumber), v.order, "", -1,
                   Option.empty(),
                   Option.empty())
           is JumpTargetVertex -> NewJumpTarget(v.code, v.name, Some(v.columnNumber), Some(v.lineNumber), v.order, "", -1, Option.empty())
           else -> {
               println(v)
               TODO("Not implemented")
           }

       }
    }

    override fun exists(v: PlumeVertex): Boolean {
        return (graph.node(v.hashCode().toLong()) != null)
    }

    override fun exists(fromV: PlumeVertex, toV: PlumeVertex, edge: EdgeLabel): Boolean {
        val srcNode = graph.node(fromV.hashCode().toLong())
        val dstNode = graph.node(toV.hashCode().toLong())
        return srcNode != null && srcNode.out(edge.name).asSequence().toList().filter { node -> node.id().equals(dstNode.id()) }.isNotEmpty()
    }

    override fun addEdge(fromV: PlumeVertex, toV: PlumeVertex, edge: EdgeLabel) {
        var srcNode = graph.node(fromV.hashCode().toLong())
        if (srcNode == null) {
            addVertex(fromV)
            srcNode = graph.node(fromV.hashCode().toLong())
        }
        var dstNode = graph.node(toV.hashCode().toLong())
        if (dstNode == null) {
            addVertex(toV)
            dstNode = graph.node(toV.hashCode().toLong())
        }
        srcNode.addEdge(edge.name, dstNode)
    }

    override fun maxOrder(): Int {
        TODO("Not yet implemented")
    }

    override fun clearGraph(): IDriver {
        return this
    }

    override fun getWholeGraph(): PlumeGraph {
        TODO("Not yet implemented")
    }

    override fun getMethod(fullName: String, signature: String): PlumeGraph {
        TODO("Not yet implemented")
    }

    override fun getMethod(fullName: String, signature: String, includeBody: Boolean): PlumeGraph {
        TODO("Not yet implemented")
    }

    override fun getProgramStructure(): PlumeGraph {
        TODO("Not yet implemented")
    }

    override fun getNeighbours(v: PlumeVertex): PlumeGraph {
        TODO("Not yet implemented")
    }

    override fun deleteVertex(v: PlumeVertex) {
        TODO("Not yet implemented")
    }

    override fun deleteMethod(fullName: String, signature: String) {
        TODO("Not yet implemented")
    }

    override fun close() {
        graph.close()
    }

}