package io.github.plume.oss.cache

import io.github.plume.oss.domain.model.DeltaGraph
import io.shiftleft.codepropertygraph.generated.nodes.*
import soot.SootClass
import soot.SootMethod
import soot.jimple.InvokeExpr
import java.util.concurrent.ConcurrentHashMap

/**
 * A node cache to reduce read calls to the database.
 */
object NodeCache {

    private val methodBodyCache = ConcurrentHashMap<SootMethod, MutableList<NewNodeBuilder>>()
    private val fileHashes = ConcurrentHashMap<SootClass, String>()
    private val savedCallGraphEdges = ConcurrentHashMap<String, MutableList<NewCallBuilder>>()
    private val typeCache = ConcurrentHashMap<String, NewTypeBuilder>()
    private val typeDeclCache = ConcurrentHashMap<String, NewTypeDeclBuilder>()
    private val fileCache = ConcurrentHashMap<String, NewFileBuilder>()
    private val callCache = mutableMapOf<InvokeExpr, NewCallBuilder>()
    private val methodCache = ConcurrentHashMap<String, NewMethodBuilder>()
    private val namespaceBlockCache = mutableMapOf<String, NewNamespaceBlockBuilder>()

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

    fun addMethod(m: NewMethodBuilder) {
        methodCache[m.build().fullName()] = m
    }

    fun getMethod(name: String): NewMethodBuilder? = methodCache[name]

    fun addNamespaceBlock(m: NewNamespaceBlockBuilder) {
        namespaceBlockCache[m.build().fullName()] = m
    }

    fun getNamespaceBlock(name: String): NewNamespaceBlockBuilder? = namespaceBlockCache[name]

    /**
     * Caches the given Soot method to the given [NewNodeBuilder].
     *
     * @param sootMtd The method from Soot to cache for.
     * @param node The [NewNodeBuilder] to associate to.
     * @param index The index to place the associated [NewNodeBuilder] at.
     */
    fun addToMethodCache(sootMtd: SootMethod, node: NewNodeBuilder, index: Int = -1) {
        if (!methodBodyCache.containsKey(sootMtd)) methodBodyCache[sootMtd] = mutableListOf(node)
        else if (index <= -1) methodBodyCache[sootMtd]?.add(node)
        else methodBodyCache[sootMtd]?.add(index, node)
    }

    /**
     * Caches the given Soot method to the given [NewNodeBuilder].
     *
     * @param sootMtd The method from Soot to cache for.
     * @param nodes The list of [NewNodeBuilder]s to associate to.
     * @param index The index to place the associated [NewNodeBuilder](s) at.
     */
    fun addToMethodCache(sootMtd: SootMethod, nodes: MutableList<NewNodeBuilder>, index: Int = -1) {
        if (!methodBodyCache.containsKey(sootMtd)) methodBodyCache[sootMtd] = nodes
        else if (index <= -1) methodBodyCache[sootMtd]?.addAll(nodes)
        else methodBodyCache[sootMtd]?.addAll(index, nodes)
    }

    /**
     * Retrieves the list of [NewNodeBuilder] associations to the given Soot method.
     *
     * @param sootMtd The method from Soot to cache for.
     */
    fun getMethodCache(sootMtd: SootMethod): List<NewNodeBuilder> = methodBodyCache[sootMtd] ?: emptyList()

    /**
     * Associates the given [SootClass] with its source file's hash.
     *
     * @param cls The [SootClass] to associate.
     * @param hash The hash for the file's contents.
     */
    fun putFileHash(cls: SootClass, hash: String) {
        fileHashes[cls] = hash
    }

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
        fileHashes.clear()
        methodBodyCache.clear()
        savedCallGraphEdges.clear()
        typeCache.clear()
        typeDeclCache.clear()
        fileCache.clear()
        callCache.clear()
        methodCache.clear()
        namespaceBlockCache.clear()
    }

    fun <K, V> createLRUMap(maxEntries: Int): Map<K, V> {
        return object : LinkedHashMap<K, V>(maxEntries * 10 / 7, 0.7f, true) {
            override fun removeEldestEntry(eldest: Map.Entry<K, V>?): Boolean {
                return size > maxEntries
            }
        }
    }
}