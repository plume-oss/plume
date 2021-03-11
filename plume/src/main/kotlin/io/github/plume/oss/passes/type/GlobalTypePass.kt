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
            .map(::getGlobalTypeDecl)
            .forEach { t ->
                logger.debug("Upserting and linking for global type ${t.build().properties()[FULL_NAME]}")
                driver.addEdge(n, t, AST)
                driver.addEdge(t, f, SOURCE_FILE)
                driver.addEdge(f, t, CONTAINS)
            }
        return ts
    }
    /*
     * TYPE -(REF)-> TYPE_DECL
     */
    private fun getGlobalTypeDecl(t: Type): NewNodeBuilder {
        val shortName = if (t.toQuotedString().contains('.')) t.toQuotedString().substringAfterLast('.')
        else t.toQuotedString()
        return nodeCache
            .find { it.build().properties().get(FULL_NAME).get() == t.toQuotedString() }
            ?: NewTypeDeclBuilder()
                .name(t.toQuotedString())
                .fullName(t.toQuotedString())
                .isExternal(false)
                .order(-1)
                .filename("<unknown>")
                .astParentType(NAMESPACE_BLOCK)
                .astParentFullName("<global>").apply {
                    val type = NewTypeBuilder().name(shortName)
                        .fullName(t.toQuotedString()).typeDeclFullName(t.toQuotedString())
                    driver.addEdge(type, this, REF)
                    nodeCache.add(this)
                }
    }




}