package io.github.plume.oss.passes.structure

import io.github.plume.oss.store.DriverCache
import io.github.plume.oss.store.LocalCache
import io.github.plume.oss.drivers.IDriver
import io.github.plume.oss.passes.IProgramStructurePass
import io.github.plume.oss.util.SootParserUtil
import io.github.plume.oss.util.SootToPlumeUtil
import io.shiftleft.codepropertygraph.generated.EdgeTypes.*
import io.shiftleft.codepropertygraph.generated.nodes.NewMemberBuilder
import io.shiftleft.codepropertygraph.generated.nodes.NewModifierBuilder
import io.shiftleft.codepropertygraph.generated.nodes.NewTypeBuilder
import io.shiftleft.codepropertygraph.generated.nodes.NewTypeDeclBuilder
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import soot.SootClass
import soot.SootField
import soot.Type

/**
 * Builds type declaration vertices for internal (application) types.
 */
open class TypePass(private val driver: IDriver) : IProgramStructurePass {

    private val logger: Logger = LogManager.getLogger(TypePass::javaClass)
    protected val cache = DriverCache(driver)

    /**
     * This pass will build type declarations, their modifiers and members and linking them to
     * their neighbour files. i.e.
     *
     *     TYPE_DECL -(SOURCE_FILE)-> FILE
     *     TYPE_DECL <-(CONTAINS)- FILE
     *     TYPE_DECL -(AST)-> *MEMBER
     *     TYPE_DECL -(AST)-> *MODIFIER
     *     TYPE_DECL -(REF)-> TYPE
     *     TYPE_DECL <-(AST)- NAMESPACE_BLOCK
     */
    override fun runPass(cs: List<SootClass>): List<SootClass> {
        cs.filter { c -> cache.tryGetTypeDecl(c.type.toQuotedString()) == null }
            .forEach { c ->
                logger.debug("Building type declaration, modifiers and fields for ${c.type}")
                buildTypeDeclaration(c.type)?.let { t ->
                    linkModifiers(c, t)
                    linkMembers(c, t)
                    linkSourceFile(c, t)
                    linkNamespaceBlock(c, t)
                }
            }
        return cs
    }

    /*
     * TYPE_DECL -(SOURCE_FILE)-> FILE
     * TYPE_DECL <-(CONTAINS)- FILE
     */
    private fun linkSourceFile(c: SootClass, t: NewTypeDeclBuilder) {
        val fileName = SootToPlumeUtil.sootClassToFileName(c)
        LocalCache.getFile(fileName)?.let { f ->
            logger.debug("Linking file $f to type ${c.type.toQuotedString()}")
            driver.addEdge(t, f, SOURCE_FILE)
            driver.addEdge(f, t, CONTAINS)
        }
    }

    /*
     * TYPE_DECL -(AST)-> MEMBER
     */
    private fun linkMembers(c: SootClass, t: NewTypeDeclBuilder) {
        c.fields.forEachIndexed { i, field ->
            projectMember(field, i + 1).let { memberVertex -> driver.addEdge(t, memberVertex, AST) }
        }
    }

    /*
     * TYPE_DECL -(AST)-> MODIFIER
     */
    private fun linkModifiers(c: SootClass, t: NewTypeDeclBuilder) {
        SootParserUtil.determineModifiers(c.modifiers)
            .mapIndexed { i, m -> NewModifierBuilder().modifierType(m).order(i + 1) }
            .forEach { m -> driver.addEdge(t, m, AST) }
    }

    /**
     * Returns the TYPE before it is processed in the database.
     */
    protected open fun getTypeNode(type: Type): NewTypeBuilder = cache.getOrMakeType(type)

    /**
     * Returns the TYPE_DECL before it is processed in the database.
     */
    protected open fun getTypeDeclNode(type: Type): NewTypeDeclBuilder = cache.getOrMakeTypeDecl(type)

    /*
     * TYPE -(REF)-> TYPE_DECL
     */
    protected open fun buildTypeDeclaration(type: Type): NewTypeDeclBuilder? {
        val t = getTypeNode(type)
        val td = getTypeDeclNode(type)
        driver.addEdge(t, td, REF)
        return td
    }

    private fun projectMember(field: SootField, childIdx: Int): NewMemberBuilder =
        NewMemberBuilder()
            .name(field.name)
            .code(field.declaration)
            .typeFullName(field.type.toQuotedString())
            .order(childIdx)

    /*
     * TYPE_DECL <-(AST)- NAMESPACE_BLOCK
     */
    private fun linkNamespaceBlock(c: SootClass, t: NewTypeDeclBuilder) {
        val fileName = SootToPlumeUtil.sootClassToFileName(c)
        val fullName = "$fileName:${c.packageName}"
        cache.tryGetNamespaceBlock(fullName)?.let { n -> driver.addEdge(n, t, AST) }
    }

}