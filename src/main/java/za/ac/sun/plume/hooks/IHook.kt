package za.ac.sun.plume.hooks

import za.ac.sun.plume.domain.enums.EdgeLabels
import za.ac.sun.plume.domain.models.GraPLVertex
import za.ac.sun.plume.domain.models.MethodDescriptorVertex
import za.ac.sun.plume.domain.models.vertices.*

interface IHook {

    /**
     * Creates the given [MethodDescriptorVertex] in the database and joins it to the vertex associated with the
     * given [MethodVertex].
     *
     * @param from the [MethodVertex] in the database.
     * @param to   the [MethodDescriptorVertex] to create and join.
     */
    fun createAndAddToMethod(from: MethodVertex, to: MethodDescriptorVertex)

    /**
     * Creates the given [ModifierVertex] in the database and joins it to the vertex associated with the
     * given [MethodVertex].
     *
     * @param from the [MethodVertex] in the database.
     * @param to   the [ModifierVertex] to create and join.
     */
    fun createAndAddToMethod(from: MethodVertex, to: ModifierVertex)

    /**
     * Joins the vertex associated with the given [FileVertex] in the database and the vertex associated with the
     * given [NamespaceBlockVertex]. If there is no associated vertices then they will be created.
     *
     * @param to   the [FileVertex] in the database.
     * @param from the [NamespaceBlockVertex] in the database.
     */
    fun joinFileVertexTo(to: FileVertex, from: NamespaceBlockVertex)

    /**
     * Joins the vertex associated with the given [FileVertex] in the database and the vertex associated with the
     * given [MethodVertex]. If there is no associated vertices then they will be created.
     *
     * @param from the [FileVertex] in the database.
     * @param to   the [MethodVertex] in the database.
     */
    fun joinFileVertexTo(from: FileVertex, to: MethodVertex)

    /**
     * Joins two namespace block vertices associated with the given [NamespaceBlockVertex] parameters. If one or
     * both of the [NamespaceBlockVertex] parameters do no have an associated vertex in the database, they are
     * created.
     *
     * @param from the from vertex.
     * @param to   the to vertex.
     */
    fun joinNamespaceBlocks(from: NamespaceBlockVertex, to: NamespaceBlockVertex)

    /**
     * Creates and assigns the [GraPLVertex] to the associated [MethodVertex] vertex in the
     * database identified by the given [MethodVertex].
     *
     * @param parentVertex the [MethodVertex] to connect.
     * @param newVertex    the [GraPLVertex] to associate with the block.
     */
    fun createAndAssignToBlock(parentVertex: MethodVertex, newVertex: GraPLVertex)

    /**
     * Creates and assigns the [GraPLVertex] to the associated [BlockVertex] vertex in the
     * database identified purely by the order.
     *
     * @param newVertex  the [GraPLVertex] to associate with the block.
     * @param blockOrder the AST order under which this block occurs.
     */
    fun createAndAssignToBlock(newVertex: GraPLVertex, blockOrder: Int)

    /**
     * Updates a key-value pair on a [GraPLVertex] in the database identified by the given AST order of the block.
     *
     * @param order the AST order under which this block occurs.
     * @param key   the key of the property to upsert.
     * @param value the value to upsert the key with.
     */
    fun updateASTVertexProperty(order: Int, key: String, value: String)

    /**
     * Creates a free-floating [GraPLVertex]
     *
     * @param graPLVertex the [GraPLVertex] to create.
     */
    fun createVertex(graPLVertex: GraPLVertex)

    /**
     * Creates an edge between two [BlockVertex] objects.
     *
     * @param blockFrom AST order of the from block.
     * @param blockTo   AST order of the to block.
     * @param edgeLabel The label to be attached to the edge.
     */
    fun joinASTVerticesByOrder(blockFrom: Int, blockTo: Int, edgeLabel: EdgeLabels)

    /**
     * Checked if there is an edge between two [BlockVertex] objects.
     *
     * @param orderFrom AST order of the from block.
     * @param orderTo   AST order of the to block.
     * @param edgeLabel The label to be attached to the edge.
     * @return true if joined by an edge, false if otherwise.
     */
    fun areASTVerticesConnected(orderFrom: Int, orderTo: Int, edgeLabel: EdgeLabels): Boolean

    /**
     * Traverses the AST nodes to search for the largest order value.
     *
     * @return the largest order property.
     */
    fun maxOrder(): Int

    /**
     * Searches for a [GraPLVertex] associated with this order.
     *
     * @param blockOrder the [GraPLVertex] order.
     * @return true if there is a [GraPLVertex] with this order value, false if otherwise.
     */
    fun isASTVertex(blockOrder: Int): Boolean

    /**
     * Clears the current loaded graph of all vertices and edges.
     */
    fun clearGraph()

    /**
     * Closes the connection to the graph database.
     */
    fun close()
}