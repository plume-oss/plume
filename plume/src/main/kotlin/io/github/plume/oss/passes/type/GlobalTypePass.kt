package io.github.plume.oss.passes.type

import io.github.plume.oss.drivers.IDriver
import io.github.plume.oss.passes.ITypePass
import io.github.plume.oss.util.ExtractorConst.GLOBAL
import io.shiftleft.codepropertygraph.generated.EdgeTypes.AST
import io.shiftleft.codepropertygraph.generated.NodeKeyNames.NAME
import io.shiftleft.codepropertygraph.generated.NodeTypes.NAMESPACE_BLOCK
import io.shiftleft.codepropertygraph.generated.nodes.NewTypeDeclBuilder
import soot.*
import soot.jimple.toolkits.typing.fast.BottomType

/**
 * Builds all types which will be considered as global types e.g. int, array types e.g. java.lang.String[].
 */
class GlobalTypePass(private val driver: IDriver) : ITypePass {

    /**
     * Creates a global TYPE_DECL and connects it to the global namespace block. i.e
     *
     *     NAMESPACE_BLOCK(<global>) -(AST)-> TYPE_DECL
     */
    override fun runPass(ts: List<Type>): List<Type> {
        val n = driver.getVerticesByProperty(NAME, GLOBAL, NAMESPACE_BLOCK).first()
        ts.filter { it is PrimType || it is ArrayType || it is VoidType || it is NullType || it is BottomType }
            .map(::buildGlobalTypeDecl)
            .forEach { t -> driver.addEdge(n, t, AST) }
        return ts
    }

    private fun buildGlobalTypeDecl(t: Type) = NewTypeDeclBuilder()
        .name(t.toQuotedString())
        .fullName(t.toQuotedString())
        .isExternal(false)
        .order(-1)
        .filename("")
        .astParentType(NAMESPACE_BLOCK)
        .astParentFullName("<global>")

}