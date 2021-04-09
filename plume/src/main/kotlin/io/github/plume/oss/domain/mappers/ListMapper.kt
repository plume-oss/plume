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