package io.github.plume.oss.drivers

import io.github.plume.oss.domain.exceptions.PlumeSchemaViolationException
import io.shiftleft.codepropertygraph.generated.nodes.NewMetaDataBuilder
import io.shiftleft.codepropertygraph.generated.nodes.NewNodeBuilder
import overflowdb.Graph

/**
 * The minimal interface for all graph drivers.
 *
 * @author David Baker Effendi
 */
interface IDriver : AutoCloseable {

    /**
     * Inserts a vertex in the graph database.
     *
     * @param v the [NewNodeBuilder] to insert.
     */
    fun addVertex(v: NewNodeBuilder)

    /**
     * Checks if the given [NewNodeBuilder] exists in the database.
     *
     * @param v the [NewNodeBuilder] to check existence of.
     * @return true if the vertex exists, false if otherwise.
     */
    fun exists(v: NewNodeBuilder): Boolean

    /**
     * Checks if there exists a directed edge of the given label between two [NewNodeBuilder] vertices.
     *
     * @param src the source [NewNodeBuilder].
     * @param tgt the target [NewNodeBuilder].
     * @param edge the edge label.
     * @return true if the edge exists, false if otherwise.
     */
    fun exists(src: NewNodeBuilder, tgt: NewNodeBuilder, edge: String): Boolean

    /**
     * Creates an edge with the given label between two [NewNodeBuilder] vertices in the graph database.
     * If the given vertices are not already present in the database, they are created. If the edge already exists
     * it wil not be recreated.
     *
     * @param src the source [NewNodeBuilder].
     * @param tgt the target [NewNodeBuilder].
     * @param edge the edge label.
     * @throws PlumeSchemaViolationException if the edge is illegal according to the CPG schema
     */
    fun addEdge(src: NewNodeBuilder, tgt: NewNodeBuilder, edge: String)

    /**
     * Clears the graph of all vertices and edges.
     *
     * @return itself as so to be chained in method calls.
     */
    fun clearGraph(): IDriver

    /**
     * Returns the whole CPG as a [Graph] object. Depending on the size of the CPG, this may be very memory
     * intensive.
     *
     * @return The whole CPG in the graph database.
     */
    fun getWholeGraph(): Graph

    /**
     * Given the full signature of a method, returns the subgraph of the method body.
     *
     * @param fullName The fully qualified name with signature e.g. interprocedural.basic.Basic4.f:int(int,int)
     * @param includeBody True if the body should be included, false if only method head should be included.
     * @return The [Graph] containing the method graph.
     */
    fun getMethod(fullName: String, includeBody: Boolean = false): Graph

    /**
     * Obtains all program structure related vertices. These are NAMESPACE_BLOCK, FILE, and TYPE_DECL vertices.
     *
     * @return The [Graph] containing the program structure related sub-graphs.
     */
    fun getProgramStructure(): Graph

    /**
     * Given a vertex, returns a [Graph] representation of neighbouring vertices.
     *
     * @param v The source vertex.
     * @return The [Graph] representation of the source vertex and its neighbouring vertices.
     */
    fun getNeighbours(v: NewNodeBuilder): Graph

    /**
     * Given a vertex, will remove it from the graph if present.
     *
     * @param id The id to remove.
     * @param label The label, if known.
     */
    fun deleteVertex(id: Long, label: String? = null)

    /**
     * Given the full signature of a method, removes the method and its body from the graph.
     *
     * @param fullName The fully qualified name with signature e.g. interprocedural.basic.Basic4.f:int(int,int)
     */
    fun deleteMethod(fullName: String)

    /**
     * Given two vertices and an edge label.
     *
     * @param src Outgoing vertex.
     * @param tgt Incoming vertex.
     * @param edge The edge label of the edge to remove.
     */
    fun deleteEdge(src: NewNodeBuilder, tgt: NewNodeBuilder, edge: String)

    /**
     * Updates a vertex's property if the node exists.
     *
     * @param id The ID of the vertex to update.
     * @param label The label of the node, if known.
     * @param key The key of the property to update.
     * @param value The updated value.
     */
    fun updateVertexProperty(id: Long, label: String?, key: String, value: Any)

    /**
     * Obtains the graph meta data information, if found.
     *
     * @return A [NewMetaDataBuilder] containing the meta data information, false if no information found.
     */
    fun getMetaData(): NewMetaDataBuilder?

}

/**
 * Interface for drivers on top of databases which don't use Long IDs by default and need to be overridden.
 */
interface IOverridenIdDriver : IDriver {

    /**
     * Given a lower bound and an upper bound, return all vertex IDs which fall between these ranges in the database.
     *
     * @param lowerBound The lower bound for the result set.
     * @param upperBound The upper bound for the result set.
     */
    fun getVertexIds(lowerBound: Long, upperBound: Long): Set<Long>

}

/**
 * Interface for drivers on top of databases that allow for schemas to be set.
 */
interface ISchemaSafeDriver : IDriver {
    /**
     * Builds and installs the CPG schema in the target database. The schema executed is from
     * [ISchemaSafeDriver.buildSchemaPayload].
     *
     * @see [ISchemaSafeDriver.buildSchemaPayload]
     */
    fun buildSchema()

    /**
     * Builds the schema from generated CPG code and returns it as a [String] to be executed on the database.
     */
    fun buildSchemaPayload(): String
}