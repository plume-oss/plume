package io.github.plume.oss.domain.exceptions

import io.github.plume.oss.domain.enums.EdgeLabel
import io.github.plume.oss.domain.models.PlumeVertex

/**
 * Thrown when an invalid edge connection is attempted to be created between two [PlumeVertex]s.
 */
class PlumeSchemaViolationException(fromV: PlumeVertex, toV: PlumeVertex, edgeLabel: EdgeLabel) :
        RuntimeException("CPG schema violation adding a $edgeLabel edge from ${fromV.javaClass.simpleName} to ${toV.javaClass.simpleName}")