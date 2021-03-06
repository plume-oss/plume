package io.github.plume.oss.passes.structure

import io.github.plume.oss.drivers.IDriver
import io.shiftleft.codepropertygraph.generated.nodes.NewTypeDeclBuilder

/**
 * Builds type declaration vertices for external (library or the code is unavailable) types.
 */
class ExternalTypePass(driver: IDriver) : TypePass(driver) {

    // TODO: Check for duplicates

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