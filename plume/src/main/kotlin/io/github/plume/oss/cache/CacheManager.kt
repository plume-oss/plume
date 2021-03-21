package io.github.plume.oss.cache

import io.github.plume.oss.drivers.IDriver
import io.github.plume.oss.util.ExtractorConst.GLOBAL
import io.github.plume.oss.util.SootToPlumeUtil
import io.shiftleft.codepropertygraph.generated.NodeKeyNames.FULL_NAME
import io.shiftleft.codepropertygraph.generated.NodeKeyNames.NAME
import io.shiftleft.codepropertygraph.generated.NodeTypes.*
import io.shiftleft.codepropertygraph.generated.nodes.NewFileBuilder
import io.shiftleft.codepropertygraph.generated.nodes.NewNamespaceBlockBuilder
import io.shiftleft.codepropertygraph.generated.nodes.NewTypeBuilder
import io.shiftleft.codepropertygraph.generated.nodes.NewTypeDeclBuilder
import scala.Option
import soot.SootClass
import soot.Type
import java.io.File

class CacheManager(private val driver: IDriver) {

    fun tryGetFile(name: String): NewFileBuilder? =
        GlobalCache.getFile(name) ?: (driver.getVerticesByProperty(
            NAME, name,
            FILE
        ).firstOrNull() as NewFileBuilder?)?.apply { GlobalCache.addFile(this) }

    /**
     * This will first see if there is a FILE in the cache, if not then will look in the graph,
     * if not then will build a new vertex.
     */
    fun getOrMakeFile(c: SootClass): NewFileBuilder {
        val fileName = SootToPlumeUtil.sootClassToFileName(c)
        val fileHash = GlobalCache.getFileHash(c)
        return tryGetFile(fileName)
            ?: (NewFileBuilder()
                .name(fileName)
                .order(1)
                .hash(Option.apply(fileHash)))
                .apply { GlobalCache.addFile(this) }
    }

    fun tryGetNamespaceBlock(fullName: String): NewNamespaceBlockBuilder? =
        GlobalCache.getNamespaceBlock(fullName) ?: (driver.getVerticesByProperty(
            FULL_NAME, fullName, NAMESPACE_BLOCK
        ).firstOrNull() as NewNamespaceBlockBuilder?)?.apply {
            GlobalCache.addNamespaceBlock(this)
        }

    /**
     * This will first see if there is a NAMESPACE_BLOCK in the cache, if not then will look in the graph,
     * if not then will build a new vertex.
     */
    fun getOrMakeNamespaceBlock(c: SootClass): NewNamespaceBlockBuilder {
        val fileName = SootToPlumeUtil.sootClassToFileName(c)
        val fullName = "$fileName:${c.packageName}"
        return tryGetNamespaceBlock(fullName) ?: (NewNamespaceBlockBuilder()
            .filename(fileName)
            .order(1)
            .name(c.packageName)
            .fullName("$fileName:${c.packageName}")).apply {
                GlobalCache.addNamespaceBlock(this)
            }
    }

    fun tryGetType(fullName: String): NewTypeBuilder? = GlobalCache.getType(fullName)
        ?: (driver.getVerticesByProperty(
            FULL_NAME,
            fullName,
            TYPE
        ).firstOrNull() as NewTypeBuilder?)?.apply { GlobalCache.addType(this) }

    fun getOrMakeType(t: Type): NewTypeBuilder {
        val fullName = t.toQuotedString()
        val shortName = if (fullName.contains('.')) fullName.substringAfterLast('.') else fullName
        return tryGetType(fullName) ?: NewTypeBuilder().name(shortName)
            .fullName(fullName)
            .typeDeclFullName(fullName).apply { GlobalCache.addType(this) }
    }

    fun tryGetTypeDecl(fullName: String): NewTypeDeclBuilder? =
        GlobalCache.getTypeDecl(fullName) ?: (driver.getVerticesByProperty(
            FULL_NAME, fullName, TYPE_DECL
        ).firstOrNull() as NewTypeDeclBuilder?)?.apply { GlobalCache.addTypeDecl(this) }

    fun getOrMakeTypeDecl(t: Type): NewTypeDeclBuilder {
        val fullName = t.toQuotedString()
        val filename = if (fullName.contains('.')) "${File.separator}${
            fullName.replace(".", File.separator).removeSuffix("[]")
        }.class"
        else fullName
        val parentType = if (fullName.contains('.')) fullName.substringBeforeLast(".")
        else fullName
        val shortName = if (fullName.contains('.')) fullName.substringAfterLast('.')
        else fullName
        return tryGetTypeDecl(fullName) ?: NewTypeDeclBuilder()
            .name(shortName)
            .fullName(fullName)
            .filename(filename)
            .astParentFullName(parentType)
            .astParentType(NAMESPACE_BLOCK)
            .order(1)
            .isExternal(false)
    }

    fun tryGetGlobalTypeDecl(fullName: String): NewTypeDeclBuilder? =
        GlobalCache.getTypeDecl(fullName)
            ?: (driver.getVerticesByProperty(
                FULL_NAME, fullName, TYPE_DECL
            ).firstOrNull() as NewTypeDeclBuilder?)?.apply { GlobalCache.addTypeDecl(this) }

    fun getOrMakeGlobalTypeDecl(t: Type): NewTypeDeclBuilder {
        return tryGetGlobalTypeDecl(t.toQuotedString())
            ?: (NewTypeDeclBuilder()
                .name(t.toQuotedString())
                .fullName(t.toQuotedString())
                .isExternal(false)
                .order(-1)
                .filename(UNKNOWN)
                .astParentType(NAMESPACE_BLOCK)
                .astParentFullName(GLOBAL)).apply { GlobalCache.addTypeDecl(this) }
    }

    fun tryGetGlobalType(fullName: String): NewTypeBuilder? = GlobalCache.getType(fullName)
        ?: (driver.getVerticesByProperty(
            FULL_NAME, fullName, TYPE
        ).firstOrNull() as NewTypeBuilder?)?.apply { GlobalCache.addType(this) }

    fun getOrMakeGlobalType(t: Type): NewTypeBuilder {
        val tdFullName = t.toQuotedString()
        val shortName = if (tdFullName.contains('.')) tdFullName.substringAfterLast('.')
        else tdFullName
        return tryGetGlobalType(tdFullName)
            ?: (NewTypeBuilder()
                .name(shortName)
                .fullName(tdFullName)
                .typeDeclFullName(tdFullName)
                    ).apply { GlobalCache.addType(this) }
    }

}