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
package io.github.plume.oss.drivers

import io.github.plume.oss.util.PlumeKeyProvider
import io.shiftleft.codepropertygraph.generated.nodes.NewNodeBuilder
import org.apache.tinkerpop.gremlin.process.traversal.P

abstract class GremlinOverriddenIdDriver : GremlinDriver(), IOverridenIdDriver {

    override fun getVertexIds(lowerBound: Long, upperBound: Long): Set<Long> {
        return g.V().id().`is`(P.inside(lowerBound - 1, upperBound + 1)).toSet().map { it as Long }.toSet()
    }

    override fun prepareVertexProperties(v: NewNodeBuilder): Map<String, Any> {
        val propertyMap = super.prepareVertexProperties(v)
        // Get the implementing classes fields and values
        if (v.id() < 0L) v.id(PlumeKeyProvider.getNewId(this))
        return propertyMap
    }

}