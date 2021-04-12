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
package io.github.plume.oss.store

import io.github.plume.oss.metrics.CacheMetrics
import io.github.plume.oss.options.CacheOptions
import io.shiftleft.codepropertygraph.generated.nodes.NewFileBuilder
import io.shiftleft.codepropertygraph.generated.nodes.NewNamespaceBlockBuilder
import io.shiftleft.codepropertygraph.generated.nodes.NewTypeBuilder
import io.shiftleft.codepropertygraph.generated.nodes.NewTypeDeclBuilder
import org.apache.logging.log4j.LogManager
import org.cache2k.Cache
import org.cache2k.Cache2kBuilder


/**
 * A local cache to reduce read calls to the database. The caching policy can be configured under [CacheOptions].
 */
object LocalCache {

    private val logger = LogManager.getLogger(LocalCache::javaClass)

    private val typeCache: Cache<String, NewTypeBuilder>
    private val typeDeclCache: Cache<String, NewTypeDeclBuilder>
    private val fileCache: Cache<String, NewFileBuilder>
    private val namespaceBlockCache: Cache<String, NewNamespaceBlockBuilder>

    init {
        val typeDeclSize = (CacheOptions.cacheSize * 0.26).toLong()
        val typeSize = (CacheOptions.cacheSize * 0.26).toLong()
        val fileSize = (CacheOptions.cacheSize * 0.24).toLong()
        val namespaceBlockSize = (CacheOptions.cacheSize * 0.24).toLong()
        typeCache = object : Cache2kBuilder<String, NewTypeBuilder>() {}
            .name("typeCache").entryCapacity(typeSize).build()
        typeDeclCache = object : Cache2kBuilder<String, NewTypeDeclBuilder>() {}
            .name("typeDeclCache").entryCapacity(typeDeclSize).build()
        fileCache = object : Cache2kBuilder<String, NewFileBuilder>() {}
            .name("fileCache").entryCapacity(fileSize).build()
        namespaceBlockCache = object : Cache2kBuilder<String, NewNamespaceBlockBuilder>() {}
            .name("namespaceBlockCache").entryCapacity(namespaceBlockSize).build()
        logger.info("Configured cache size is ${CacheOptions.cacheSize}.")
        logger.debug("Assigning cache as follows => " +
                "(TYPE, $typeSize), " +
                "(TYPE_DECL, $typeDeclSize), " +
                "(FILE, $fileSize), " +
                "(NAMESPACE_BLOCK, $namespaceBlockSize)")
    }

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

    fun removeNamespaceBlock(fullName: String) = namespaceBlockCache.remove(fullName)

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
