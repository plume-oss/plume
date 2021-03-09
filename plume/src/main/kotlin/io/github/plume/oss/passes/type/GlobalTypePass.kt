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
     */
    override fun runPass(ts: List<Type>): List<Type> {
        val n = driver.getVerticesByProperty(NAME, GLOBAL, NAMESPACE_BLOCK).first()
        val f = driver.getVerticesByProperty(NAME, UNKNOWN, FILE).first()
        // Fill up cache
        nodeCache.addAll(driver.getVerticesByProperty(AST_PARENT_FULL_NAME, GLOBAL, TYPE_DECL))
        ts.filterNot { it is RefType }
            .map(::getGlobalTypeDecl)
            .forEach { t ->
                logger.debug("Building global type $t")
                driver.addEdge(n, t, AST)
                driver.addEdge(t, f, SOURCE_FILE)
                driver.addEdge(f, t, CONTAINS)
            }
        return ts
    }

    private fun getGlobalTypeDecl(t: Type): NewNodeBuilder {
        return nodeCache
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

}