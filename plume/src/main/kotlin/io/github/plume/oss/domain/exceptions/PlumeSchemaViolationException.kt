package io.github.plume.oss.domain.exceptions

import io.github.plume.oss.domain.enums.EdgeLabel
import io.shiftleft.codepropertygraph.generated.nodes.NewNodeBuilder

/**
 * Thrown when an invalid edge connection is attempted to be created between two [NewNodeBuilder]s.
 */
class PlumeSchemaViolationException(fromV: NewNodeBuilder, toV: NewNodeBuilder, edgeLabel: EdgeLabel) :
    RuntimeException("CPG schema violation adding a $edgeLabel edge from ${fromV.javaClass.simpleName} to ${toV.javaClass.simpleName}")