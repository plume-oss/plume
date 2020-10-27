package za.ac.sun.plume.drivers

import za.ac.sun.plume.domain.enums.EdgeLabel
import za.ac.sun.plume.domain.exceptions.PlumeSchemaViolationException
import za.ac.sun.plume.domain.models.PlumeGraph
import za.ac.sun.plume.domain.models.PlumeVertex

/**
 * The minimal interface for all graph drivers.
 *
 * @author David Baker Effendi
 */
interface IDriver : AutoCloseable {

    /**
     * Inserts a vertex in the graph database.
     *
     * @param v the [PlumeVertex] to insert.
     */
    fun addVertex(v: PlumeVertex)

    /**
     * Checks if the given [PlumeVertex] exists in the database.
     *
     * @param v the [PlumeVertex] to check existence of.
     * @return true if the vertex exists, false if otherwise.
     */
    fun exists(v: PlumeVertex): Boolean

    /**
     * Checks if there exists a directed edge of the given label between two [PlumeVertex] vertices.
     *
     * @param fromV the source [PlumeVertex].
     * @param toV the target [PlumeVertex].
     * @param edge the [EdgeLabel] to label the edge with.
     * @return true if the edge exists, false if otherwise.
     */
    fun exists(fromV: PlumeVertex, toV: PlumeVertex, edge: EdgeLabel): Boolean

    /**
     * Creates an edge with the label from enum [EdgeLabel] between two [PlumeVertex] vertices in the graph database.
     * If the given vertices are not already present in the database, they are created. If the edge already exists
     * it wil not be recreated.
     *
     * @param fromV the source [PlumeVertex].
     * @param toV the target [PlumeVertex].
     * @param edge the [EdgeLabel] to label the edge with.
     * @throws PlumeSchemaViolationException if the edge is illegal according to the CPG schema
     */
    fun addEdge(fromV: PlumeVertex, toV: PlumeVertex, edge: EdgeLabel)

    /**
     * Scans the AST vertices of the graph for the largest order property.
     *
     * @return the largest order value in the graph.
     */
    fun maxOrder(): Int

    /**
     * Clears the graph of all vertices and edges.
     *
     * @return itself as so to be chained in method calls.
     */
    fun clearGraph(): IDriver

    /**
     * Given the full signature of a method, returns the subgraph of the method body.
     *
     * @param fullName The fully qualified name e.g. interprocedural.basic.Basic4.f
     * @param signature The method signature e.g. int f(int, int)
     * @return The [PlumeGraph] containing the method graph.
     */
    fun getMethod(fullName: String, signature: String): PlumeGraph
}