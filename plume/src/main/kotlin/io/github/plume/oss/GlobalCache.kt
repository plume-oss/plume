package io.github.plume.oss

import io.shiftleft.codepropertygraph.generated.nodes.*
import overflowdb.Graph
import soot.SootClass
import soot.SootMethod
import soot.toolkits.graph.BriefUnitGraph
import java.util.concurrent.ConcurrentHashMap

/**
 * A cache to reduce read calls to the database.
 */
object GlobalCache {

    private val sootToPlume = ConcurrentHashMap<Any, MutableList<NewNodeBuilder>>()
    private val fHashes = ConcurrentHashMap<SootClass, String>()
    private val savedCallGraphEdges = ConcurrentHashMap<String, MutableList<NewCallBuilder>>()

    /**
     * Caches already built method bodies to save database requests during SCPG passes.
     */
    val methodBodies = mutableMapOf<String, Graph>()

    /**
     * Associates the given Soot object to the given [NewNode].
     *
     * @param sootObject The object from a Soot [BriefUnitGraph] to associate from.
     * @param node The [NewNode] to associate to.
     * @param index The index to place the associated [NewNode] at.
     */
    fun addSootAssoc(sootObject: Any, node: NewNodeBuilder, index: Int = -1) {
        if (!sootToPlume.containsKey(sootObject)) sootToPlume[sootObject] = mutableListOf(node)
        else if (index <= -1) sootToPlume[sootObject]?.add(node)
        else sootToPlume[sootObject]?.add(index, node)
    }

    /**
     * Associates the given Soot object to the given list of [NewNode]s.
     *
     * @param sootObject The object from a Soot [BriefUnitGraph] to associate from.
     * @param nodes The list of [NewNode]s to associate to.
     * @param index The index to place the associated [PlumeVertex](s) at.
     */
    fun addSootAssoc(sootObject: Any, nodes: MutableList<NewNodeBuilder>, index: Int = -1) {
        if (!sootToPlume.containsKey(sootObject)) sootToPlume[sootObject] = nodes
        else if (index <= -1) sootToPlume[sootObject]?.addAll(nodes)
        else sootToPlume[sootObject]?.addAll(index, nodes)
    }

    /**
     * Retrieves the list of [NewNode] associations to the given Soot object.
     *
     * @param sootObject The object from a Soot [BriefUnitGraph] to get associations from.
     */
    fun getSootAssoc(sootObject: Any): List<NewNodeBuilder>? = sootToPlume[sootObject]

    /**
     * Associates the given [SootClass] with its source file's hash.
     *
     * @param cls The [SootClass] to associate.
     * @param hash The hash for the file's contents.
     */
    fun putFileHash(cls: SootClass, hash: String) {
        fHashes[cls] = hash
    }

    /**
     * Retrieves the original file's hash from the given [SootClass].
     *
     * @param cls The representative [SootClass].
     */
    fun getFileHash(cls: SootClass) = fHashes[cls]

    /**
     * Saves call graph edges to the [NewMethod] from the [NewCallBuilder].
     *
     * @param fullName The method full name.
     * @param call The source [NewCallBuilder].
     */
    fun saveCallEdge(fullName: String, call: NewCallBuilder) {
        if (!savedCallGraphEdges.containsKey(fullName)) savedCallGraphEdges[fullName] = mutableListOf(call)
        else savedCallGraphEdges[fullName]?.add(call)
    }

    /**
     * Retrieves all the incoming [NewCall]s from the given [NewMethod].
     *
     * @param fullName The method full name.
     */
    fun getCallEdgeIn(fullName: String) = savedCallGraphEdges[fullName]

    fun clear() {
        fHashes.clear()
        sootToPlume.clear()
        savedCallGraphEdges.clear()
    }
}