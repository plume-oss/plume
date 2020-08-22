package za.ac.sun.plume.drivers

import za.ac.sun.plume.domain.enums.EdgeLabel
import za.ac.sun.plume.domain.models.PlumeVertex

/**
 * The minimal interface for all graph drivers.
 *
 * @author David Baker Effendi
 */
interface IDriver : AutoCloseable {

    /**
     * Inserts a vertex in the graph database or updates it if the vertex is already present.
     *
     * @param v the [PlumeVertex] to upsert.
     */
    fun upsertVertex(v: PlumeVertex)

    /**
     * Checks if the given [PlumeVertex] exists in the database.
     *
     * @param v the [PlumeVertex] to check existence of.
     * @return true if the vertex exists, false if otherwise.
     */
    fun exists(v: PlumeVertex): Boolean

    /**
     * Checks if an edge of the given label exists between two [PlumeVertex] vertices.
     *
     * @param fromV the source [PlumeVertex].
     * @param toV the target [PlumeVertex].
     * @param edge the [EdgeLabel] to label the edge with.
     * @return true if the edge exists, false if otherwise.
     */
    fun exists(fromV: PlumeVertex, toV: PlumeVertex, edge: EdgeLabel): Boolean

    /**
     * Creates an edge with the label from enum [EdgeLabel] between two [PlumeVertex] vertices in the graph database.
     * If the given vertices are not already present in the database, they are created using [upsertVertex].
     *
     * @param fromV the source [PlumeVertex].
     * @param toV the target [PlumeVertex].
     * @param edge the [EdgeLabel] to label the edge with.
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
     */
    fun clearGraph()

}