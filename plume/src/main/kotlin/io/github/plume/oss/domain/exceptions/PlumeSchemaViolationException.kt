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
package io.github.plume.oss.domain.exceptions

import io.shiftleft.codepropertygraph.generated.nodes.NewNode
import io.shiftleft.codepropertygraph.generated.nodes.NewNodeBuilder

/**
 * Thrown when an invalid edge connection is attempted to be created between two [NewNodeBuilder]s.
 */
class PlumeSchemaViolationException(fromV: NewNodeBuilder<out NewNode>, toV: NewNodeBuilder<out NewNode>, edgeLabel: String) :
    RuntimeException("CPG schema violation adding a $edgeLabel edge from ${fromV.javaClass.simpleName} to ${toV.javaClass.simpleName}")