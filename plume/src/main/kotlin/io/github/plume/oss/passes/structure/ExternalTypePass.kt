package io.github.plume.oss.passes.structure

import io.github.plume.oss.drivers.IDriver
import io.shiftleft.codepropertygraph.generated.nodes.NewTypeDeclBuilder
import soot.Type

/**
 * Builds type declaration vertices for external (library or the code is unavailable) types.
 */
class ExternalTypePass(driver: IDriver) : TypePass(driver) {

    // Overridden to avoid building a duplicate type
    override fun buildTypeDeclaration(type: Type): NewTypeDeclBuilder? {
        cache.tryGetTypeDecl(type.toQuotedString())?.let { return null }
        return super.buildTypeDeclaration(type)
    }

    /**
     * Creates an external TYPE_DECL.
     */
    override fun getTypeDeclNode(type: Type): NewTypeDeclBuilder {
        val t = super.getTypeDeclNode(type)
        t.isExternal(true)
        return t
    }
}