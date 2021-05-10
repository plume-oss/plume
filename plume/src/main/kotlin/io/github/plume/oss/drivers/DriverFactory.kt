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
package io.github.plume.oss.drivers

/**
 * The factory responsible for obtaining the desired [IDriver].
 */
object DriverFactory {
    /**
     * Creates an [IDriver] based on the given [GraphDatabase].
     *
     * @param driverType The [GraphDatabase] the driver is meant to interface with.
     * @return the driver designed to interface with the given [GraphDatabase].
     */
    @JvmStatic
    operator fun invoke(driverType: GraphDatabase): IDriver {
        return when (driverType) {
            GraphDatabase.TINKER_GRAPH -> TinkerGraphDriver()
            GraphDatabase.JANUS_GRAPH -> JanusGraphDriver()
            GraphDatabase.TIGER_GRAPH -> TigerGraphDriver()
            GraphDatabase.NEPTUNE -> NeptuneDriver()
            GraphDatabase.NEO4J -> Neo4jDriver()
            GraphDatabase.NEO4J_EMBEDDED -> EmbeddedNeo4jDriver()
            GraphDatabase.OVERFLOWDB -> OverflowDbDriver()
        }
    }
}

/**
 * The graph databases supported by Plume's [IDriver]s.
 */
enum class GraphDatabase {
    /**
     * An in-memory graph database queried using Gremlin.
     */
    TINKER_GRAPH,

    /**
     * An open-source graph database with various search engine and storage backend options. This is queried using
     * Gremlin.
     */
    JANUS_GRAPH,

    /**
     * An enterprise graph database communicated over via REST and queried using GSQL.
     */
    TIGER_GRAPH,

    /**
     * Amazon Neptune is a fast, reliable graph database service that makes it easy to build and run applications that
     * work with highly connected datasets. This is queried using Gremlin.
     */
    NEPTUNE,

    /**
     * Neo4j is the graph database platform powering mission-critical enterprise applications like artificial
     * intelligence, fraud detection and recommendations.
     */
    NEO4J,

    /**
     * The embedded version of Neo4j useful when speed is of utmost importance.
     */
    NEO4J_EMBEDDED,

    /**
     * ShiftLeft's OverflowDB is an in-memory graph database, which implements a swapping mechanism to deal
     * with large graphs.
     * */
    OVERFLOWDB
}