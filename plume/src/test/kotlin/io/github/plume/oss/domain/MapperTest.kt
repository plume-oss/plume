package io.github.plume.oss.domain

import io.github.plume.oss.TestDomainResources.Companion.vertices
import io.github.plume.oss.domain.mappers.VertexMapper
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import scala.jdk.CollectionConverters


class MapperTest {

    @Test
    fun mapToVertexTest() {
        vertices.forEach { v ->
            // Only check properties currently used by Plume
            val node = v.build()
            val map = VertexMapper.prepareListsInMap(CollectionConverters.MapHasAsJava(node.properties()).asJava())
            map["label"] = node.label()
            map["id"] = v.id()
            val expectedProperties = node.properties()
            val actualProperties = VertexMapper.mapToVertex(map).build().properties()
            val excludedKeys = expectedProperties.keySet().diff(actualProperties.keySet())
            excludedKeys.foreach { expectedProperties.`$minus`(it) }
            assertEquals(expectedProperties, actualProperties)
        }
    }

}