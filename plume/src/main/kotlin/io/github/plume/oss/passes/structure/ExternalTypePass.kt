package io.github.plume.oss.passes.structure

import io.github.plume.oss.drivers.IDriver
import io.shiftleft.codepropertygraph.generated.NodeKeyNames.FULL_NAME
import io.shiftleft.codepropertygraph.generated.NodeTypes.TYPE_DECL
import io.shiftleft.codepropertygraph.generated.nodes.NewTypeDeclBuilder
import soot.SootClass
import soot.Type

/**
 * Builds type declaration vertices for external (library or the code is unavailable) types.
 */
class ExternalTypePass(private val driver: IDriver) : TypePass(driver) {

    // Overridden to avoid building a duplicate type
    override fun buildTypeDeclaration(type: Type): NewTypeDeclBuilder? {
        driver.getVerticesByProperty(FULL_NAME, type.toQuotedString(), TYPE_DECL)
            .filterIsInstance<NewTypeDeclBuilder>()
            .find { t -> t.build().fullName() == type.toQuotedString() }
            ?.let { return null }
        println("Creating new external type")
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