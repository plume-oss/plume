package io.github.plume.oss.store

import io.github.plume.oss.metrics.CacheMetrics
import io.github.plume.oss.options.CacheOptions
import io.shiftleft.codepropertygraph.generated.nodes.NewFileBuilder
import io.shiftleft.codepropertygraph.generated.nodes.NewNamespaceBlockBuilder
import io.shiftleft.codepropertygraph.generated.nodes.NewTypeBuilder
import io.shiftleft.codepropertygraph.generated.nodes.NewTypeDeclBuilder
import org.cache2k.Cache
import org.cache2k.Cache2kBuilder


/**
 * A local cache to reduce read calls to the database. The caching policy can be configured under [CacheOptions].
 */
object LocalCache {

    private val typeCache: Cache<String, NewTypeBuilder> =
        object : Cache2kBuilder<String, NewTypeBuilder>() {}
            .name("typeCache")
            .expireAfterWrite(CacheOptions.cacheExpiry.first, CacheOptions.cacheExpiry.second)
            .entryCapacity(CacheOptions.cacheSize)
            .build()
    private val typeDeclCache: Cache<String, NewTypeDeclBuilder> =
        object : Cache2kBuilder<String, NewTypeDeclBuilder>() {}
            .name("typeDeclCache")
            .expireAfterWrite(CacheOptions.cacheExpiry.first, CacheOptions.cacheExpiry.second)
            .entryCapacity(CacheOptions.cacheSize)
            .build()
    private val fileCache: Cache<String, NewFileBuilder> =
        object : Cache2kBuilder<String, NewFileBuilder>() {}
            .name("fileCache")
            .expireAfterWrite(CacheOptions.cacheExpiry.first, CacheOptions.cacheExpiry.second)
            .entryCapacity(CacheOptions.cacheSize)
            .build()
    private val namespaceBlockCache: Cache<String, NewNamespaceBlockBuilder> =
        object : Cache2kBuilder<String, NewNamespaceBlockBuilder>() {}
            .name("namespaceBlockCache")
            .expireAfterWrite(CacheOptions.cacheExpiry.first, CacheOptions.cacheExpiry.second)
            .entryCapacity(CacheOptions.cacheSize)
            .build()

    fun removeType(fullName: String) = typeCache.remove(fullName)

    fun addType(t: NewTypeBuilder) = typeCache.put(t.build().fullName(), t)

    fun getType(fullName: String): NewTypeBuilder? = typeCache[fullName]
        .apply { if (this != null) CacheMetrics.cacheHit() else CacheMetrics.cacheMiss() }

    fun removeTypeDecl(fullName: String) = typeDeclCache.remove(fullName)

    fun addTypeDecl(td: NewTypeDeclBuilder) = typeDeclCache.put(td.build().fullName(), td)

    fun getTypeDecl(fullName: String): NewTypeDeclBuilder? = typeDeclCache[fullName]
        .apply { if (this != null) CacheMetrics.cacheHit() else CacheMetrics.cacheMiss() }

    fun removeFile(name: String) = fileCache.remove(name)

    fun addFile(f: NewFileBuilder) = fileCache.put(f.build().name(), f)

    fun getFile(name: String): NewFileBuilder? = fileCache[name]
        .apply { if (this != null) CacheMetrics.cacheHit() else CacheMetrics.cacheMiss() }

    fun addNamespaceBlock(n: NewNamespaceBlockBuilder) = namespaceBlockCache.put(n.build().fullName(), n)

    fun getNamespaceBlock(name: String): NewNamespaceBlockBuilder? = namespaceBlockCache[name]
        .apply { if (this != null) CacheMetrics.cacheHit() else CacheMetrics.cacheMiss() }

    fun clear() {
        typeCache.clear()
        typeDeclCache.clear()
        fileCache.clear()
        namespaceBlockCache.clear()
    }
}
