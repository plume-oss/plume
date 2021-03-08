package io.github.plume.oss.passes.structure

import io.github.plume.oss.drivers.IDriver
import io.shiftleft.codepropertygraph.generated.NodeTypes.TYPE_DECL
import io.shiftleft.codepropertygraph.generated.nodes.NewTypeDeclBuilder
import io.shiftleft.codepropertygraph.generated.nodes.TypeDecl
import overflowdb.Graph
import soot.SootClass
import soot.Type

/**
 * Builds type declaration vertices for external (library or the code is unavailable) types.
 */
class ExternalTypePass(private val driver: IDriver) : TypePass(driver) {

    private lateinit var programStructure: Graph

    override fun runPass(cs: List<SootClass>): List<SootClass> {
        programStructure = driver.getProgramStructure()
        super.runPass(cs)
        programStructure.close()
        return cs
    }

    // Overridden to avoid building a duplicate type
    override fun buildTypeDeclaration(type: Type): NewTypeDeclBuilder? {
        programStructure.nodes(TYPE_DECL).asSequence()
            .filterIsInstance<TypeDecl>()
            .find { t -> t.fullName() == type.toQuotedString() }
            ?.let { return null }
        return super.buildTypeDeclaration(type)
    }

    /**
     * Creates an external TYPE_DECL.
     */
    override fun buildTypeDecNode(
        shortName: String,
        fullName: String,
        filename: String,
        parentType: String
    ): NewTypeDeclBuilder {
        val t = super.buildTypeDecNode(shortName, fullName, filename, parentType)
        t.isExternal(true)
        return t
    }
}