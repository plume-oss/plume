package io.github.plume.oss.passes.structure

import io.github.plume.oss.GlobalCache
import io.github.plume.oss.drivers.IDriver
import io.github.plume.oss.passes.IProgramStructurePass
import io.github.plume.oss.util.SootParserUtil
import io.github.plume.oss.util.SootToPlumeUtil
import io.shiftleft.codepropertygraph.generated.EdgeTypes.*
import io.shiftleft.codepropertygraph.generated.NodeKeyNames.*
import io.shiftleft.codepropertygraph.generated.NodeTypes.*
import io.shiftleft.codepropertygraph.generated.nodes.*
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import soot.SootClass
import soot.SootField

/**
 * Builds type declaration vertices for internal (application) types.
 */
open class TypePass(private val driver: IDriver) : IProgramStructurePass {

    private val logger: Logger = LogManager.getLogger(TypePass::javaClass)
    private val nodeCache = mutableListOf<NewNodeBuilder>()

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
        cs.forEach { c ->
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
        GlobalCache.getFile(fileName)?.let { f ->
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
            projectMember(field, i + 1).let { memberVertex ->
                driver.addEdge(t, memberVertex, AST)
                // TODO: Check tthis
//                GlobalCache.addToMethodCache(field, memberVertex)
            }
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

    protected open fun buildTypeDecNode(
        shortName: String,
        fullName: String,
        filename: String,
        parentType: String
    ): NewTypeDeclBuilder =
        NewTypeDeclBuilder()
            .name(shortName)
            .fullName(fullName)
            .filename(filename)
            .astParentFullName(parentType)
            .astParentType(NAMESPACE_BLOCK)
            .order(1)
            .isExternal(false)

    /*
     * TYPE -(REF)-> TYPE_DECL
     */
    protected open fun buildTypeDeclaration(type: soot.Type): NewTypeDeclBuilder? {
        val filename = if (type.toQuotedString().contains('.')) "/${
            type.toQuotedString().replace(".", "/").removeSuffix("[]")
        }.class"
        else type.toQuotedString()
        val parentType = if (type.toQuotedString().contains('.')) type.toQuotedString().substringBeforeLast(".")
        else type.toQuotedString()
        val shortName = if (type.toQuotedString().contains('.')) type.toQuotedString().substringAfterLast('.')
        else type.toQuotedString()

        val t = NewTypeBuilder().name(shortName)
            .fullName(type.toQuotedString())
            .typeDeclFullName(type.toQuotedString())
        val td = buildTypeDecNode(shortName, type.toQuotedString(), filename, parentType)
        driver.addEdge(t, td, REF)
        GlobalCache.addType(t)
        GlobalCache.addTypeDecl(td)
        return td
    }

    protected open fun projectMember(field: SootField, childIdx: Int): NewMemberBuilder =
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
        val n = getNamespaceBlock(fullName)
        driver.addEdge(n, t, AST)
    }

    private fun getNamespaceBlock(fullName: String): NewNodeBuilder {
        return nodeCache.filter { it.build().properties().keySet().contains(FULL_NAME) }
            .find { it.build().properties().get(FULL_NAME).get() == fullName }
            ?: driver.getVerticesByProperty(FULL_NAME, fullName, NAMESPACE_BLOCK).first().apply { nodeCache.add(this) }
    }
}