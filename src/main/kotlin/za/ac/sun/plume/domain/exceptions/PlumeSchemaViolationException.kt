package za.ac.sun.plume.domain.exceptions

import za.ac.sun.plume.domain.enums.EdgeLabel
import za.ac.sun.plume.domain.models.PlumeVertex

/**
 * Thrown when an invalid edge connection is attempted to be created between two [PlumeVertex]s.
 */
class PlumeSchemaViolationException(fromV: PlumeVertex, toV: PlumeVertex, edgeLabel: EdgeLabel) :
        RuntimeException(message = "CPG schema violation adding a $edgeLabel edge from ${fromV.javaClass} to ${toV.javaClass}")