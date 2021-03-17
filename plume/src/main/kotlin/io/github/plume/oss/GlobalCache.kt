package io.github.plume.oss

import io.github.plume.oss.domain.model.DeltaGraph
import io.shiftleft.codepropertygraph.generated.nodes.*
import soot.SootClass
import soot.SootMethod
import soot.jimple.InvokeExpr
import java.util.concurrent.ConcurrentHashMap

/**
 * A cache to reduce read calls to the database.
 */
object GlobalCache {

    private val methodCache = ConcurrentHashMap<SootMethod, MutableList<NewNodeBuilder>>()
    private val fHashes = ConcurrentHashMap<SootClass, String>()
    private val savedCallGraphEdges = ConcurrentHashMap<String, MutableList<NewCallBuilder>>()
    private val typeCache = ConcurrentHashMap<String, NewTypeBuilder>()
    private val typeDeclCache = ConcurrentHashMap<String, NewTypeDeclBuilder>()
    private val fileCache = ConcurrentHashMap<String, NewFileBuilder>()
    private val callCache = mutableMapOf<InvokeExpr, NewCallBuilder>()

    /**
     * Caches already built method bodies to save database requests during SCPG passes.
     */
    val methodBodies = mutableMapOf<String, DeltaGraph>()

    fun addType(t: NewTypeBuilder) {
        typeCache[t.build().fullName()] = t
    }

    fun getType(fullName: String): NewTypeBuilder? = typeCache[fullName]

    fun addTypeDecl(td: NewTypeDeclBuilder) {
        typeDeclCache[td.build().fullName()] = td
    }

    fun getTypeDecl(fullName: String): NewTypeDeclBuilder? = typeDeclCache[fullName]

    fun addFile(f: NewFileBuilder) {
        fileCache[f.build().name()] = f
    }

    fun getFile(name: String): NewFileBuilder? = fileCache[name]

    fun addCall(i: InvokeExpr, c: NewCallBuilder) {
        callCache[i] = c
    }

    fun getCall(i: InvokeExpr): NewCallBuilder? = callCache[i]
    /**
     * Caches the given Soot method to the given [NewNodeBuilder].
     *
     * @param sootMtd The method from Soot to cache for.
     * @param node The [NewNodeBuilder] to associate to.
     * @param index The index to place the associated [NewNodeBuilder] at.
     */
    fun addToMethodCache(sootMtd: SootMethod, node: NewNodeBuilder, index: Int = -1) {
        if (!methodCache.containsKey(sootMtd)) methodCache[sootMtd] = mutableListOf(node)
        else if (index <= -1) methodCache[sootMtd]?.add(node)
        else methodCache[sootMtd]?.add(index, node)
    }

    /**
     * Caches the given Soot method to the given [NewNodeBuilder].
     *
     * @param sootMtd The method from Soot to cache for.
     * @param nodes The list of [NewNodeBuilder]s to associate to.
     * @param index The index to place the associated [NewNodeBuilder](s) at.
     */
    fun addToMethodCache(sootMtd: SootMethod, nodes: MutableList<NewNodeBuilder>, index: Int = -1) {
        if (!methodCache.containsKey(sootMtd)) methodCache[sootMtd] = nodes
        else if (index <= -1) methodCache[sootMtd]?.addAll(nodes)
        else methodCache[sootMtd]?.addAll(index, nodes)
    }

    /**
     * Retrieves the list of [NewNodeBuilder] associations to the given Soot method.
     *
     * @param sootMtd The method from Soot to cache for.
     */
    fun getMethodCache(sootMtd: SootMethod): List<NewNodeBuilder>? = methodCache[sootMtd]

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
        methodCache.clear()
        savedCallGraphEdges.clear()
        typeCache.clear()
        typeDeclCache.clear()
        fileCache.clear()
        callCache.clear()
    }
}