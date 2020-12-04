package za.ac.sun.plume.drivers

import io.shiftleft.codepropertygraph.generated.nodes.NewNode
import overflowdb.Config
import overflowdb.Graph
import za.ac.sun.plume.domain.enums.EdgeLabel
import za.ac.sun.plume.domain.models.PlumeGraph
import za.ac.sun.plume.domain.models.PlumeVertex

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

    val graph : Graph = createEmptyGraph()

    private fun createEmptyGraph() : Graph {
        // TODO pass file name from the outside
        val dbFileName = "/tmp/foo.odb"
        val odbConfig = Config.withDefaults().withStorageLocation(dbFileName)
        return Graph.open(odbConfig,
                io.shiftleft.codepropertygraph.generated.nodes.Factories.allAsJava(),
                io.shiftleft.codepropertygraph.generated.edges.Factories.allAsJava())
    }

    override fun addVertex(v: PlumeVertex) {
        val id = v.hashCode()
        val newNode = graph.addNode(id.toLong(), "label")
    }

    private fun convert(v : PlumeVertex) : NewNode {
        TODO("foo")
    }

    override fun exists(v: PlumeVertex): Boolean {
        TODO("Not yet implemented")
    }

    override fun exists(fromV: PlumeVertex, toV: PlumeVertex, edge: EdgeLabel): Boolean {
        TODO("Not yet implemented")
    }

    override fun addEdge(fromV: PlumeVertex, toV: PlumeVertex, edge: EdgeLabel) {
        TODO("Not yet implemented")
    }

    override fun maxOrder(): Int {
        TODO("Not yet implemented")
    }

    override fun clearGraph(): IDriver {
        TODO("Not yet implemented")
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