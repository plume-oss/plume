package io.github.plume.oss.passes.type

import io.github.plume.oss.drivers.IDriver
import io.github.plume.oss.passes.ITypePass
import io.github.plume.oss.passes.structure.TypePass
import io.github.plume.oss.util.ExtractorConst.GLOBAL
import io.github.plume.oss.util.ExtractorConst.UNKNOWN
import io.shiftleft.codepropertygraph.generated.EdgeTypes.*
import io.shiftleft.codepropertygraph.generated.NodeKeyNames.*
import io.shiftleft.codepropertygraph.generated.NodeTypes.*
import io.shiftleft.codepropertygraph.generated.nodes.NewNodeBuilder
import io.shiftleft.codepropertygraph.generated.nodes.NewTypeBuilder
import io.shiftleft.codepropertygraph.generated.nodes.NewTypeDeclBuilder
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import soot.*

/**
 * Builds all types which will be considered as global types e.g. int, array types e.g. java.lang.String[].
 */
class GlobalTypePass(private val driver: IDriver) : ITypePass {

    private val logger: Logger = LogManager.getLogger(TypePass::javaClass)
    private val nodeCache = mutableSetOf<NewNodeBuilder>()

    /**
     * Creates a global TYPE_DECL and connects it to the global namespace block. i.e
     *
     *     NAMESPACE_BLOCK(<global>) -(AST)-> TYPE_DECL
     *     FILE(<unknown>) -(CONTAINS)-> TYPE_DECL
     *     FILE(<unknown>) <-(SOURCE_FILE)- TYPE_DECL
     *
     *     TYPE_DECL -(REF)-> TYPE
     *     TYPE_DECL -(AST)-> *MEMBER ? String[].length ?
     *     TYPE_DECL -(AST)-> *MODIFIER ?
     */
    override fun runPass(ts: List<Type>): List<Type> {
        val n = driver.getVerticesByProperty(NAME, GLOBAL, NAMESPACE_BLOCK).first()
        val f = driver.getVerticesByProperty(NAME, UNKNOWN, FILE).first()
        // Fill up cache
        nodeCache.addAll(driver.getVerticesByProperty(AST_PARENT_FULL_NAME, GLOBAL, TYPE_DECL))
        ts.filterNot { it is RefType }
            .map{Pair(getGlobalTypeDecl(it), it)}
            .forEach { (td, st) ->
                logger.debug("Upserting and linking for global type ${st.toQuotedString()}")
                driver.addEdge(n, td, AST)
                driver.addEdge(td, f, SOURCE_FILE)
                driver.addEdge(f, td, CONTAINS)
                getGlobalType(st.toQuotedString()).apply {
                    driver.addEdge(this, td, REF)
                }
            }
        return ts
    }

    private fun getGlobalTypeDecl(t: Type): NewNodeBuilder {
        return nodeCache
            .filterIsInstance<NewTypeDeclBuilder>()
            .find { it.build().properties().get(FULL_NAME).get() == t.toQuotedString() }
            ?: NewTypeDeclBuilder()
                .name(t.toQuotedString())
                .fullName(t.toQuotedString())
                .isExternal(false)
                .order(-1)
                .filename("<unknown>")
                .astParentType(NAMESPACE_BLOCK)
                .astParentFullName("<global>").apply { nodeCache.add(this) }
    }

    private fun getGlobalType(tdFullName: String): NewNodeBuilder {
        val shortName = if (tdFullName.contains('.')) tdFullName.substringAfterLast('.')
        else tdFullName
        return nodeCache
            .filterIsInstance<NewTypeBuilder>()
            .find { it.build().properties().get(FULL_NAME).get() == tdFullName }
            ?: driver.getVerticesByProperty(FULL_NAME, tdFullName, TYPE).firstOrNull()?.apply { nodeCache.add(this) }
            ?: NewTypeBuilder()
                .name(shortName)
                .fullName(tdFullName)
                .typeDeclFullName(tdFullName)
                .apply { nodeCache.add(this) }
    }
}