/*
 * Copyright 2021 Plume Authors
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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