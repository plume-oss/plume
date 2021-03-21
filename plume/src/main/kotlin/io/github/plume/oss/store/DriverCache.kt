package io.github.plume.oss.store

import io.github.plume.oss.drivers.IDriver
import io.github.plume.oss.util.ExtractorConst
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

/**
 * A driver-backed cache manger that first checks if the requested object exists locally before checking on the driver.
 * Also provides methods for creating vertices from Soot information if the equivalent graph structure does not exist.
 * The creation of these nodes are not added to the driver but only made locally.
 */
class DriverCache(private val driver: IDriver) {

    /**
     * Will attempt to look for the FILE in the local cache and, if not found, will then look on the database.
     * If found on the database then it is added to the local cache.
     */
    fun tryGetFile(name: String): NewFileBuilder? =
        LocalCache.getFile(name) ?: (driver.getVerticesByProperty(
            NAME, name,
            FILE
        ).firstOrNull() as NewFileBuilder?)?.apply { LocalCache.addFile(this) }

    /**
     * This will try to get an existing associated FILE vertex else create one. The created vertex is not written to the
     * database.
     */
    fun getOrMakeFile(c: SootClass): NewFileBuilder {
        val fileName = SootToPlumeUtil.sootClassToFileName(c)
        val fileHash = PlumeStorage.getFileHash(c)
        return tryGetFile(fileName)
            ?: (NewFileBuilder()
                .name(fileName)
                .order(1)
                .hash(Option.apply(fileHash)))
                .apply { LocalCache.addFile(this) }
    }

    /**
     * Will attempt to look for the NAMESPACE_BLOCK in the local cache and, if not found, will then look on the database.
     * If found on the database then it is added to the local cache.
     */
    fun tryGetNamespaceBlock(fullName: String): NewNamespaceBlockBuilder? =
        LocalCache.getNamespaceBlock(fullName) ?: (driver.getVerticesByProperty(
            FULL_NAME, fullName, NAMESPACE_BLOCK
        ).firstOrNull() as NewNamespaceBlockBuilder?)?.apply {
            LocalCache.addNamespaceBlock(this)
        }

    /**
     * This will try to get an existing associated NAMESPACE_BLOCK vertex else create one. The created vertex is not
     * written to the database.
     */
    fun getOrMakeNamespaceBlock(c: SootClass): NewNamespaceBlockBuilder {
        val fileName = SootToPlumeUtil.sootClassToFileName(c)
        val fullName = "$fileName:${c.packageName}"
        return tryGetNamespaceBlock(fullName) ?: (NewNamespaceBlockBuilder()
            .filename(fileName)
            .order(1)
            .name(c.packageName)
            .fullName("$fileName:${c.packageName}")).apply {
                LocalCache.addNamespaceBlock(this)
            }
    }

    /**
     * Will attempt to look for the TYPE in the local cache and, if not found, will then look on the database.
     * If found on the database then it is added to the local cache.
     */
    fun tryGetType(fullName: String): NewTypeBuilder? = LocalCache.getType(fullName)
        ?: (driver.getVerticesByProperty(
            FULL_NAME,
            fullName,
            TYPE
        ).firstOrNull() as NewTypeBuilder?)?.apply { LocalCache.addType(this) }

    /**
     * This will try to get an existing associated TYPE vertex else create one. The created vertex is not written to the
     * database.
     */
    fun getOrMakeType(t: Type): NewTypeBuilder {
        val fullName = t.toQuotedString()
        val shortName = if (fullName.contains('.')) fullName.substringAfterLast('.') else fullName
        return tryGetType(fullName) ?: NewTypeBuilder().name(shortName)
            .fullName(fullName)
            .typeDeclFullName(fullName).apply { LocalCache.addType(this) }
    }

    /**
     * Will attempt to look for the TYPE_DECL in the local cache and, if not found, will then look on the database.
     * If found on the database then it is added to the local cache.
     */
    fun tryGetTypeDecl(fullName: String): NewTypeDeclBuilder? =
        LocalCache.getTypeDecl(fullName) ?: (driver.getVerticesByProperty(
            FULL_NAME, fullName, TYPE_DECL
        ).firstOrNull() as NewTypeDeclBuilder?)?.apply { LocalCache.addTypeDecl(this) }

    /**
     * This will try to get an existing associated TYPE_DECL vertex else create one. The created vertex is not written
     * to the database.
     */
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

    /**
     * Will attempt to look for the global TYPE_DECL in the local cache and, if not found, will then look on the
     * database. If found on the database then it is added to the local cache.
     */
    fun tryGetGlobalTypeDecl(fullName: String): NewTypeDeclBuilder? =
        LocalCache.getTypeDecl(fullName)
            ?: (driver.getVerticesByProperty(
                FULL_NAME, fullName, TYPE_DECL
            ).firstOrNull() as NewTypeDeclBuilder?)?.apply { LocalCache.addTypeDecl(this) }

    /**
     * This will try to get an existing associated global TYPE_DECL vertex else create one. The created vertex is not
     * written to the database.
     */
    fun getOrMakeGlobalTypeDecl(t: Type): NewTypeDeclBuilder {
        return tryGetGlobalTypeDecl(t.toQuotedString())
            ?: (NewTypeDeclBuilder()
                .name(t.toQuotedString())
                .fullName(t.toQuotedString())
                .isExternal(false)
                .order(-1)
                .filename(ExtractorConst.UNKNOWN)
                .astParentType(NAMESPACE_BLOCK)
                .astParentFullName(GLOBAL)).apply { LocalCache.addTypeDecl(this) }
    }

    /**
     * Will attempt to look for the global TYPE in the local cache and, if not found, will then look on the database.
     * If found on the database then it is added to the local cache.
     */
    fun tryGetGlobalType(fullName: String): NewTypeBuilder? = LocalCache.getType(fullName)
        ?: (driver.getVerticesByProperty(
            FULL_NAME, fullName, TYPE
        ).firstOrNull() as NewTypeBuilder?)?.apply { LocalCache.addType(this) }

    /**
     * This will try to get an existing associated global TYPE vertex else create one. The created vertex is not written
     * to the database.
     */
    fun getOrMakeGlobalType(t: Type): NewTypeBuilder {
        val tdFullName = t.toQuotedString()
        val shortName = if (tdFullName.contains('.')) tdFullName.substringAfterLast('.')
        else tdFullName
        return tryGetGlobalType(tdFullName)
            ?: (NewTypeBuilder()
                .name(shortName)
                .fullName(tdFullName)
                .typeDeclFullName(tdFullName)
                    ).apply { LocalCache.addType(this) }
    }

}