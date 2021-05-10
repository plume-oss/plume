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

import io.github.plume.oss.domain.mappers.VertexMapper
import io.shiftleft.codepropertygraph.generated.EdgeTypes.AST
import io.shiftleft.codepropertygraph.generated.EdgeTypes.SOURCE_FILE
import io.shiftleft.codepropertygraph.generated.NodeTypes.*
import io.shiftleft.codepropertygraph.generated.nodes.Factories
import io.shiftleft.codepropertygraph.generated.nodes.NewNodeBuilder
import overflowdb.Config
import overflowdb.Graph
import overflowdb.Node
import scala.jdk.CollectionConverters
import io.shiftleft.codepropertygraph.generated.edges.Factories as EdgeFactories
import io.shiftleft.codepropertygraph.generated.nodes.Factories as NodeFactories

abstract class CypherDriverQueries {

    fun addVertexCypher(v: NewNodeBuilder, idx: Int): String =
        """
            ${createVertexPayload(v, idx)}
            RETURN ID(n$idx) as id$idx
        """.trimIndent()

    fun checkVertexExistCypher(id: Long, label: String? = null): String =
        """
        MATCH (n${if (label != null) ":$label" else ""})
        WHERE id(n) = $id
        RETURN n
        """.trimIndent()

    fun checkEdgeExistCypher(src: NewNodeBuilder, tgt: NewNodeBuilder, edge: String): String {
        val srcN = src.build()
        val tgtN = tgt.build()
        return """
                MATCH (a:${srcN.label()}), (b:${tgtN.label()})
                WHERE id(a) = ${src.id()} AND id(b) = ${tgt.id()}
                RETURN EXISTS ((a)-[:$edge]->(b)) as edge_exists
                """.trimIndent()
    }

    fun addEdgeCypher(src: NewNodeBuilder, tgt: NewNodeBuilder, edge: String): String {
        val srcN = src.build()
        val tgtN = tgt.build()
        return """
                MATCH (a:${srcN.label()}), (b:${tgtN.label()})
                WHERE id(a) = ${src.id()} AND id(b) = ${tgt.id()}
                CREATE (a)-[r:$edge]->(b)
                RETURN r
                """.trimIndent()
    }

    fun clearGraphCypher(): String =
        """
        MATCH (n)
        DETACH DELETE n
        """.trimIndent()

    fun getAllVerticesCypher(): String =
        """
        MATCH (n)
        WHERE NOT (n)-[]-()
        RETURN n
        """.trimIndent()

    fun getAllEdgesCypher(): String =
        """
        MATCH (n)-[r]->(m)
        RETURN n AS src, m AS tgt, type(r) AS rel 
        """.trimIndent()

    fun getMethodQueryHead(fullName: String, includeBody: Boolean): String =
        if (!includeBody)
            """
            MATCH (root:$METHOD {FULL_NAME:'$fullName'})-[r1:$AST]->(child)
                    WITH DISTINCT r1 AS coll
            """.trimIndent()
        else
            """
            MATCH (root:$METHOD {FULL_NAME:'$fullName'})-[r1:$AST*0..]->(child)-[r2]->(n1) 
                WHERE NOT (child)-[:$SOURCE_FILE]-(n1)
            OPTIONAL MATCH (root)-[r3]->(n2) WHERE NOT (root)-[:$SOURCE_FILE]-(n2)  
            WITH DISTINCT (r1 + r2 + r3) AS coll
            """.trimIndent()

    fun getMethodCypher(queryHead: String): String =
        """
        $queryHead
        UNWIND coll AS e1
        WITH DISTINCT e1
        WITH [r in collect(e1) | {rel: type(r), src: startNode(r), tgt: endNode(r)} ] as e2
        UNWIND e2 as x
        RETURN x
        """.trimIndent()

    fun getProgramStructureCypher1(): String =
        """
        MATCH (n:$FILE)-[r1:$AST*0..]->(m)-[r2]->(o) 
        WITH DISTINCT (r1 + r2) AS coll
        UNWIND coll AS e1
        WITH DISTINCT e1
        WITH [r in collect(e1) | {rel: type(r), src: startNode(r), tgt: endNode(r)} ] as e2
        UNWIND e2 as x
        RETURN x
        """.trimIndent()

    fun getProgramStructureCypher2(): String =
        """
        MATCH (m:$TYPE_DECL)
        MATCH (n:$FILE)
        MATCH (o:$NAMESPACE_BLOCK)
        RETURN m, n, o
        """.trimIndent()

    fun getNeighboursCypher(v: NewNodeBuilder): String =
        """
        MATCH (n:${v.build().label()})-[r1]-(m)
        WHERE ID(n) = ${v.id()}
        WITH DISTINCT r1 AS coll
        UNWIND coll AS e1
        WITH DISTINCT e1
        WITH [r in collect(e1) | {rel: type(r), src: startNode(r), tgt: endNode(r)} ] as e2
        UNWIND e2 as x
        RETURN x
        """.trimIndent()

    fun deleteVertexCypher(id: Long, label: String?): String =
        """
        MATCH (n${if (label != null) ":$label" else ""})
        WHERE ID(n) = $id
        DETACH DELETE n
        """.trimIndent()

    fun deleteEdgeCypher(src: NewNodeBuilder, tgt: NewNodeBuilder, edge: String): String {
        val srcN = src.build()
        val tgtN = tgt.build()
        return """
                MATCH (s:${srcN.label()})-[r:$edge]->(t:${tgtN.label()})
                WHERE ID(s) = ${src.id()} AND ID(t) = ${tgt.id()}  
                DELETE r
                """.trimIndent()
    }

    fun deleteMethodCypher(fullName: String): String =
        """
        MATCH (a)-[r:${AST}*]->(t)
        WHERE a.FULL_NAME = "$fullName"
        FOREACH (x IN r | DELETE x)
        DETACH DELETE a, t
        """.trimIndent()

    fun updateVertexPropertyCypher(id: Long, label: String?, key: String, value: Any): String =
        """
        MATCH (n${if (label != null) ":$label" else ""})
        WHERE ID(n) = $id
        SET n.$key = ${if (value is String) "\"$value\"" else value}
        """.trimIndent()

    fun getMetaDataCypher(): String =
        """
        MATCH (n:$META_DATA)
        RETURN n
        LIMIT 1
        """.trimIndent()

    fun getVerticesByPropertyCypher(
        propertyKey: String,
        propertyValue: Any,
        label: String?
    ): String =
        """
        MATCH (n${if (label != null) ":$label" else ""})
        WHERE n.$propertyKey = ${if (propertyValue is String) "\"$propertyValue\"" else propertyValue}
        RETURN n
        """.trimIndent()

    fun getPropertyFromVerticesCypher(propertyKey: String, label: String?): String =
        """
        MATCH (n${if (label != null) ":$label" else ""})
        RETURN n.$propertyKey AS p
        """.trimIndent()

    fun getVerticesOfTypeCypher(label: String): String =
        """
        MATCH (n:$label)
        RETURN n
        """.trimIndent()

    fun newOverflowGraph(): Graph = Graph.open(
        Config.withDefaults(),
        NodeFactories.allAsJava(),
        EdgeFactories.allAsJava()
    )

    fun addNodeToGraph(graph: Graph, v: NewNodeBuilder): Node {
        val maybeExistingNode = graph.node(v.id())
        if (maybeExistingNode != null) return maybeExistingNode

        val bNode = v.build()
        val sNode = graph.addNode(v.id(), bNode.label())
        bNode.properties().foreachEntry { key, value -> sNode.setProperty(key, value) }
        return sNode
    }


    companion object {
        fun sanitizePayload(p: String): String =
            p.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\'", "\\\'")
                .replace("\n", "\\\n")
                .replace("\t", "\\\t")
                .replace("\b", "\\\b")
                .replace("\r", "\\\r")
                .replace("\\f", "\\\\f")

        fun createVertexPayload(v: NewNodeBuilder, idx: Int): String {
            val node = v.build()
            val propertyMap = VertexMapper.stripUnusedProperties(
                v.build().label(),
                CollectionConverters.MapHasAsJava(node.properties()).asJava().toMutableMap()
            )
            propertyMap["label"] = node.label()
            val payload = StringBuilder("{")
            val attributeList = extractAttributesFromMap(propertyMap).toList()
            attributeList.forEachIndexed { i: Int, e: Pair<String, Any> ->
                payload.append("${e.first}:")
                val p = e.second
                if (p is String) payload.append("\"${sanitizePayload(p)}\"")
                else payload.append(p)
                if (i < attributeList.size - 1) payload.append(",")
            }
            payload.append("}")
            return "CREATE (n$idx:${node.label()} $payload)"
        }

        fun extractAttributesFromMap(propertyMap: MutableMap<String, Any>): MutableMap<String, Any> {
            val attributes = VertexMapper.prepareListsInMap(propertyMap)
            propertyMap.forEach { e ->
                if (attributes[e.key] is Int) attributes[e.key] = (attributes[e.key] as Int).toLong()
            }
            return attributes
        }
    }

}