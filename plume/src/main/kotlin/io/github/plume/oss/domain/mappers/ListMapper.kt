package io.github.plume.oss.domain.mappers

import scala.jdk.CollectionConverters

/**
 * Contains functions to serialize and deserialize Scala lists.
 */
object ListMapper {

    /**
     * Converts a given Scala list to a string for serialization.
     *
     * @param list The list to convert.
     * @return a comma-delimited string.
     */
    fun scalaListToString(list: scala.collection.immutable.List<*>): String =
        CollectionConverters.IterableHasAsJava(list).asJava().joinToString(separator = ",")

    /**
     * Converts a serialized list to a Scala list.
     *
     * @param string The string to convert.
     * @return a Scala list of the given type.
     */
    fun stringToScalaList(string: String): scala.collection.immutable.List<String> {
        val list = if (string.isBlank()) emptyList() else string.split(',').toList()
        return CollectionConverters.ListHasAsScala(list).asScala().toList()
    }

}