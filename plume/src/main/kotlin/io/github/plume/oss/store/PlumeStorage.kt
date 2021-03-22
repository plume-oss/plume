package io.github.plume.oss.store

import io.github.plume.oss.domain.model.DeltaGraph
import io.shiftleft.codepropertygraph.generated.nodes.NewCallBuilder
import io.shiftleft.codepropertygraph.generated.nodes.NewMethod
import io.shiftleft.codepropertygraph.generated.nodes.NewMethodBuilder
import io.shiftleft.codepropertygraph.generated.nodes.NewNodeBuilder
import soot.SootClass
import soot.SootMethod
import soot.jimple.InvokeExpr
import java.util.concurrent.ConcurrentHashMap

/**
 * Used to temporarily store node and Soot objects. This is not to be used as a cache and the lifetime of these items
 * is per call to project.
 */
object PlumeStorage {

    private val methodBodyNodes = ConcurrentHashMap<SootMethod, MutableList<NewNodeBuilder>>()
    private val savedCallGraphEdges = ConcurrentHashMap<String, MutableList<NewCallBuilder>>()
    private val methods = ConcurrentHashMap<String, NewMethodBuilder>()
    private val fileHashes = ConcurrentHashMap<SootClass, String>()
    private val calls = mutableMapOf<InvokeExpr, NewCallBuilder>()

    /**
     * Stores already built method bodies to save database requests during SCPG passes.
     */
    val methodCpgs = mutableMapOf<String, DeltaGraph>()

    fun addMethod(m: NewMethodBuilder) = methods.put(m.build().fullName(), m)

    fun getMethod(name: String): NewMethodBuilder? = methods[name]

    /**
     * Retrieves the list of [NewNodeBuilder] associations to the given Soot method.
     *
     * @param sootMtd The method from Soot to cache for.
     */
    fun getMethodStore(sootMtd: SootMethod): List<NewNodeBuilder> = methodBodyNodes[sootMtd] ?: emptyList()

    /**
     * Caches the given Soot method to the given [NewNodeBuilder].
     *
     * @param sootMtd The method from Soot to cache for.
     * @param node The [NewNodeBuilder] to associate to.
     * @param index The index to place the associated [NewNodeBuilder] at.
     */
    fun storeMethodNode(sootMtd: SootMethod, node: NewNodeBuilder, index: Int = -1) {
        if (!methodBodyNodes.containsKey(sootMtd)) methodBodyNodes[sootMtd] = mutableListOf(node)
        else if (index <= -1) methodBodyNodes[sootMtd]?.add(node)
        else methodBodyNodes[sootMtd]?.add(index, node)
    }

    /**
     * Caches the given Soot method to the given [NewNodeBuilder].
     *
     * @param sootMtd The method from Soot to cache for.
     * @param nodes The list of [NewNodeBuilder]s to associate to.
     * @param index The index to place the associated [NewNodeBuilder](s) at.
     */
    fun storeMethodNode(sootMtd: SootMethod, nodes: List<NewNodeBuilder>, index: Int = -1) {
        if (!methodBodyNodes.containsKey(sootMtd)) methodBodyNodes[sootMtd] = nodes.toMutableList()
        else if (index <= -1) methodBodyNodes[sootMtd]?.addAll(nodes)
        else methodBodyNodes[sootMtd]?.addAll(index, nodes)
    }

    /**
     * Associates the given [SootClass] with its source file's hash.
     *
     * @param cls The [SootClass] to associate.
     * @param hash The hash for the file's contents.
     */
    fun storeFileHash(cls: SootClass, hash: String) = fileHashes.put(cls, hash)

    /**
     * Retrieves the original file's hash from the given [SootClass].
     *
     * @param cls The representative [SootClass].
     */
    fun getFileHash(cls: SootClass) = fileHashes[cls]

    /**
     * Saves call graph edges to the [NewMethod] from the [NewCallBuilder].
     *
     * @param fullName The method full name.
     * @param call The source [NewCallBuilder].
     */
    fun storeCallEdge(fullName: String, call: NewCallBuilder) {
        if (!savedCallGraphEdges.containsKey(fullName)) savedCallGraphEdges[fullName] = mutableListOf(call)
        else savedCallGraphEdges[fullName]?.add(call)
    }

    /**
     * Retrieves all the incoming [NewCallBuilder]s from the given [NewMethodBuilder].
     *
     * @param fullName The method full name.
     */
    fun getCallsIn(fullName: String): List<NewCallBuilder> = savedCallGraphEdges[fullName] ?: emptyList()

    fun addCall(i: InvokeExpr, c: NewCallBuilder) = calls.put(i, c)

    fun getCall(i: InvokeExpr): NewCallBuilder? = calls[i]

    fun clear() {
        fileHashes.clear()
        methodBodyNodes.clear()
        savedCallGraphEdges.clear()
        calls.clear()
        methods.clear()
    }
}