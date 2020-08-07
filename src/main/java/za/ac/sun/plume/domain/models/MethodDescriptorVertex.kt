package za.ac.sun.plume.domain.models

open class MethodDescriptorVertex (
        val name: String,
        val typeFullName: String,
        order: Int
): ASTVertex(order)