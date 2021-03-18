package io.github.plume.oss.domain

import io.github.plume.oss.domain.mappers.ListMapper
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import scala.jdk.CollectionConverters

class ListMapperTest {

    @Test
    fun oneElementListToStringTest() {
        val l1 = CollectionConverters.ListHasAsScala(listOf("T1")).asScala().toList()
        val s1 = ListMapper.scalaListToString(l1)
        assertEquals("T1", s1)
    }

    @Test
    fun twoElementListToStringTest() {
        val l1 = CollectionConverters.ListHasAsScala(listOf("T1", "T2")).asScala().toList()
        val s1 = ListMapper.scalaListToString(l1)
        assertEquals("T1,T2", s1)
    }

    @Test
    fun emptyListToStringTest() {
        val l1 = CollectionConverters.ListHasAsScala(emptyList<String>()).asScala().toList()
        val s1 = ListMapper.scalaListToString(l1)
        assertEquals("", s1)
    }

    @Test
    fun stringToOneElementListTest() {
        val l1 = CollectionConverters.ListHasAsScala(listOf("T1")).asScala().toList()
        assertEquals(l1, ListMapper.stringToScalaList("T1"))
    }

    @Test
    fun stringToTwoElementListTest() {
        val l1 = CollectionConverters.ListHasAsScala(listOf("T1", "T2")).asScala().toList()
        assertEquals(l1, ListMapper.stringToScalaList("T1,T2"))
    }

    @Test
    fun stringToEmptyElementListTest() {
        val l1 = CollectionConverters.ListHasAsScala(emptyList<String>()).asScala().toList()
        assertEquals(l1, ListMapper.stringToScalaList(""))
    }

}