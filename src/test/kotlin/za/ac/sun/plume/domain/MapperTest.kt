package za.ac.sun.plume.domain

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail
import za.ac.sun.plume.TestDomainResources.Companion.vertices
import za.ac.sun.plume.domain.mappers.VertexMapper
import kotlin.reflect.full.memberProperties


class MapperTest {

    @Test
    fun propertiesToMapTest() {
        vertices.forEach { v ->
            val map = VertexMapper.propertiesToMap(v)
            map.remove("label")
            map.forEach { entry ->
                val property = v::class.memberProperties.find { it.name == entry.key }
                        ?: fail("Could not resolve property ${entry.key} from map")
                if (property.getter.call(v) is Enum<*>)
                    assertEquals(entry.value, (property.getter.call(v) as Enum<*>).name)
                else
                    assertEquals(entry.value, property.getter.call(v))
            }
        }
    }

    @Test
    fun mapToVertexTest() {
        vertices.forEach { v ->
            val map = VertexMapper.propertiesToMap(v)
            assertEquals(v, VertexMapper.mapToVertex(map))
        }
    }

}