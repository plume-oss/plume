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
            val map = CollectionConverters.MapHasAsJava(node.properties()).asJava().toMutableMap()
            map["label"] = node.label()
            map["id"] = v.id()
            val expectedProperties = node.properties()
            val actualProperties = VertexMapper.mapToVertex(map).build().properties()
            val excludedKeys = expectedProperties.keySet().diff(actualProperties.keySet())
            excludedKeys.foreach { expectedProperties.`$minus`(it) }
            assertEquals(expectedProperties, actualProperties)
        }
    }

    @Test
    fun vertexConstraintsTest() {
//        vertices.forEach { src ->
//            when (src) {
//                is ArrayInitializerVertex -> {
//                    val clazz = ArrayInitializerVertex
//                    EdgeLabel.values().forEach { e ->
//                        VertexLabel.values().forEach { tgt ->
//                            if (clazz.VALID_OUT_EDGES.containsKey(e) && clazz.VALID_OUT_EDGES[e]!!.contains(tgt)) {
//                                assertTrue(VertexMapper.checkSchemaConstraints(clazz.LABEL, e, tgt))
//                            } else {
//                                assertFalse(VertexMapper.checkSchemaConstraints(clazz.LABEL, e, tgt))
//                            }
//                        }
//                    }
//                }
//                is BindingVertex -> {
//                    val clazz = BindingVertex
//                    EdgeLabel.values().forEach { e ->
//                        VertexLabel.values().forEach { tgt ->
//                            if (clazz.VALID_OUT_EDGES.containsKey(e) && clazz.VALID_OUT_EDGES[e]!!.contains(tgt)) {
//                                assertTrue(VertexMapper.checkSchemaConstraints(clazz.LABEL, e, tgt))
//                            } else {
//                                assertFalse(VertexMapper.checkSchemaConstraints(clazz.LABEL, e, tgt))
//                            }
//                        }
//                    }
//                }
//                is BlockVertex -> {
//                    val clazz = BlockVertex
//                    EdgeLabel.values().forEach { e ->
//                        VertexLabel.values().forEach { tgt ->
//                            if (clazz.VALID_OUT_EDGES.containsKey(e) && clazz.VALID_OUT_EDGES[e]!!.contains(tgt)) {
//                                assertTrue(VertexMapper.checkSchemaConstraints(clazz.LABEL, e, tgt))
//                            } else {
//                                assertFalse(VertexMapper.checkSchemaConstraints(clazz.LABEL, e, tgt))
//                            }
//                        }
//                    }
//                }
//                is CallVertex -> {
//                    val clazz = CallVertex
//                    EdgeLabel.values().forEach { e ->
//                        VertexLabel.values().forEach { tgt ->
//                            if (clazz.VALID_OUT_EDGES.containsKey(e) && clazz.VALID_OUT_EDGES[e]!!.contains(tgt)) {
//                                assertTrue(VertexMapper.checkSchemaConstraints(clazz.LABEL, e, tgt))
//                            } else {
//                                assertFalse(VertexMapper.checkSchemaConstraints(clazz.LABEL, e, tgt))
//                            }
//                        }
//                    }
//                }
//                is ControlStructureVertex -> {
//                    val clazz = ControlStructureVertex
//                    EdgeLabel.values().forEach { e ->
//                        VertexLabel.values().forEach { tgt ->
//                            if (clazz.VALID_OUT_EDGES.containsKey(e) && clazz.VALID_OUT_EDGES[e]!!.contains(tgt)) {
//                                assertTrue(VertexMapper.checkSchemaConstraints(clazz.LABEL, e, tgt))
//                            } else {
//                                assertFalse(VertexMapper.checkSchemaConstraints(clazz.LABEL, e, tgt))
//                            }
//                        }
//                    }
//                }
//                is FieldIdentifierVertex -> {
//                    val clazz = FieldIdentifierVertex
//                    EdgeLabel.values().forEach { e ->
//                        VertexLabel.values().forEach { tgt ->
//                            if (clazz.VALID_OUT_EDGES.containsKey(e) && clazz.VALID_OUT_EDGES[e]!!.contains(tgt)) {
//                                assertTrue(VertexMapper.checkSchemaConstraints(clazz.LABEL, e, tgt))
//                            } else {
//                                assertFalse(VertexMapper.checkSchemaConstraints(clazz.LABEL, e, tgt))
//                            }
//                        }
//                    }
//                }
//                is FileVertex -> {
//                    val clazz = FileVertex
//                    EdgeLabel.values().forEach { e ->
//                        VertexLabel.values().forEach { tgt ->
//                            if (clazz.VALID_OUT_EDGES.containsKey(e) && clazz.VALID_OUT_EDGES[e]!!.contains(tgt)) {
//                                assertTrue(VertexMapper.checkSchemaConstraints(clazz.LABEL, e, tgt))
//                            } else {
//                                assertFalse(VertexMapper.checkSchemaConstraints(clazz.LABEL, e, tgt))
//                            }
//                        }
//                    }
//                }
//                is IdentifierVertex -> {
//                    val clazz = IdentifierVertex
//                    EdgeLabel.values().forEach { e ->
//                        VertexLabel.values().forEach { tgt ->
//                            if (clazz.VALID_OUT_EDGES.containsKey(e) && clazz.VALID_OUT_EDGES[e]!!.contains(tgt)) {
//                                assertTrue(VertexMapper.checkSchemaConstraints(clazz.LABEL, e, tgt))
//                            } else {
//                                assertFalse(VertexMapper.checkSchemaConstraints(clazz.LABEL, e, tgt))
//                            }
//                        }
//                    }
//                }
//                is JumpTargetVertex -> {
//                    val clazz = JumpTargetVertex
//                    EdgeLabel.values().forEach { e ->
//                        VertexLabel.values().forEach { tgt ->
//                            if (clazz.VALID_OUT_EDGES.containsKey(e) && clazz.VALID_OUT_EDGES[e]!!.contains(tgt)) {
//                                assertTrue(VertexMapper.checkSchemaConstraints(clazz.LABEL, e, tgt))
//                            } else {
//                                assertFalse(VertexMapper.checkSchemaConstraints(clazz.LABEL, e, tgt))
//                            }
//                        }
//                    }
//                }
//                is LiteralVertex -> {
//                    val clazz = LiteralVertex
//                    EdgeLabel.values().forEach { e ->
//                        VertexLabel.values().forEach { tgt ->
//                            if (clazz.VALID_OUT_EDGES.containsKey(e) && clazz.VALID_OUT_EDGES[e]!!.contains(tgt)) {
//                                assertTrue(VertexMapper.checkSchemaConstraints(clazz.LABEL, e, tgt))
//                            } else {
//                                assertFalse(VertexMapper.checkSchemaConstraints(clazz.LABEL, e, tgt))
//                            }
//                        }
//                    }
//                }
//                is LocalVertex -> {
//                    val clazz = LocalVertex
//                    EdgeLabel.values().forEach { e ->
//                        VertexLabel.values().forEach { tgt ->
//                            if (clazz.VALID_OUT_EDGES.containsKey(e) && clazz.VALID_OUT_EDGES[e]!!.contains(tgt)) {
//                                assertTrue(VertexMapper.checkSchemaConstraints(clazz.LABEL, e, tgt))
//                            } else {
//                                assertFalse(VertexMapper.checkSchemaConstraints(clazz.LABEL, e, tgt))
//                            }
//                        }
//                    }
//                }
//                is MemberVertex -> {
//                    val clazz = MemberVertex
//                    EdgeLabel.values().forEach { e ->
//                        VertexLabel.values().forEach { tgt ->
//                            if (clazz.VALID_OUT_EDGES.containsKey(e) && clazz.VALID_OUT_EDGES[e]!!.contains(tgt)) {
//                                assertTrue(VertexMapper.checkSchemaConstraints(clazz.LABEL, e, tgt))
//                            } else {
//                                assertFalse(VertexMapper.checkSchemaConstraints(clazz.LABEL, e, tgt))
//                            }
//                        }
//                    }
//                }
//                is MetaDataVertex -> {
//                    val clazz = MetaDataVertex
//                    EdgeLabel.values().forEach { e ->
//                        VertexLabel.values().forEach { tgt ->
//                            if (clazz.VALID_OUT_EDGES.containsKey(e) && clazz.VALID_OUT_EDGES[e]!!.contains(tgt)) {
//                                assertTrue(VertexMapper.checkSchemaConstraints(clazz.LABEL, e, tgt))
//                            } else {
//                                assertFalse(VertexMapper.checkSchemaConstraints(clazz.LABEL, e, tgt))
//                            }
//                        }
//                    }
//                }
//                is MethodParameterInVertex -> {
//                    val clazz = MethodParameterInVertex
//                    EdgeLabel.values().forEach { e ->
//                        VertexLabel.values().forEach { tgt ->
//                            if (clazz.VALID_OUT_EDGES.containsKey(e) && clazz.VALID_OUT_EDGES[e]!!.contains(tgt)) {
//                                assertTrue(VertexMapper.checkSchemaConstraints(clazz.LABEL, e, tgt))
//                            } else {
//                                assertFalse(VertexMapper.checkSchemaConstraints(clazz.LABEL, e, tgt))
//                            }
//                        }
//                    }
//                }
//                is MethodRefVertex -> {
//                    val clazz = MethodRefVertex
//                    EdgeLabel.values().forEach { e ->
//                        VertexLabel.values().forEach { tgt ->
//                            if (clazz.VALID_OUT_EDGES.containsKey(e) && clazz.VALID_OUT_EDGES[e]!!.contains(tgt)) {
//                                assertTrue(VertexMapper.checkSchemaConstraints(clazz.LABEL, e, tgt))
//                            } else {
//                                assertFalse(VertexMapper.checkSchemaConstraints(clazz.LABEL, e, tgt))
//                            }
//                        }
//                    }
//                }
//                is MethodReturnVertex -> {
//                    val clazz = MethodReturnVertex
//                    EdgeLabel.values().forEach { e ->
//                        VertexLabel.values().forEach { tgt ->
//                            if (clazz.VALID_OUT_EDGES.containsKey(e) && clazz.VALID_OUT_EDGES[e]!!.contains(tgt)) {
//                                assertTrue(VertexMapper.checkSchemaConstraints(clazz.LABEL, e, tgt))
//                            } else {
//                                assertFalse(VertexMapper.checkSchemaConstraints(clazz.LABEL, e, tgt))
//                            }
//                        }
//                    }
//                }
//                is MethodVertex -> {
//                    val clazz = MethodVertex
//                    EdgeLabel.values().forEach { e ->
//                        VertexLabel.values().forEach { tgt ->
//                            if (clazz.VALID_OUT_EDGES.containsKey(e) && clazz.VALID_OUT_EDGES[e]!!.contains(tgt)) {
//                                assertTrue(VertexMapper.checkSchemaConstraints(clazz.LABEL, e, tgt))
//                            } else {
//                                assertFalse(VertexMapper.checkSchemaConstraints(clazz.LABEL, e, tgt))
//                            }
//                        }
//                    }
//                }
//                is ModifierVertex -> {
//                    val clazz = ModifierVertex
//                    EdgeLabel.values().forEach { e ->
//                        VertexLabel.values().forEach { tgt ->
//                            if (clazz.VALID_OUT_EDGES.containsKey(e) && clazz.VALID_OUT_EDGES[e]!!.contains(tgt)) {
//                                assertTrue(VertexMapper.checkSchemaConstraints(clazz.LABEL, e, tgt))
//                            } else {
//                                assertFalse(VertexMapper.checkSchemaConstraints(clazz.LABEL, e, tgt))
//                            }
//                        }
//                    }
//                }
//                is NamespaceBlockVertex -> {
//                    val clazz = NamespaceBlockVertex
//                    EdgeLabel.values().forEach { e ->
//                        VertexLabel.values().forEach { tgt ->
//                            if (clazz.VALID_OUT_EDGES.containsKey(e) && clazz.VALID_OUT_EDGES[e]!!.contains(tgt)) {
//                                assertTrue(VertexMapper.checkSchemaConstraints(clazz.LABEL, e, tgt))
//                            } else {
//                                assertFalse(VertexMapper.checkSchemaConstraints(clazz.LABEL, e, tgt))
//                            }
//                        }
//                    }
//                }
//                is ReturnVertex -> {
//                    val clazz = ReturnVertex
//                    EdgeLabel.values().forEach { e ->
//                        VertexLabel.values().forEach { tgt ->
//                            if (clazz.VALID_OUT_EDGES.containsKey(e) && clazz.VALID_OUT_EDGES[e]!!.contains(tgt)) {
//                                assertTrue(VertexMapper.checkSchemaConstraints(clazz.LABEL, e, tgt))
//                            } else {
//                                assertFalse(VertexMapper.checkSchemaConstraints(clazz.LABEL, e, tgt))
//                            }
//                        }
//                    }
//                }
//                is TypeArgumentVertex -> {
//                    val clazz = TypeArgumentVertex
//                    EdgeLabel.values().forEach { e ->
//                        VertexLabel.values().forEach { tgt ->
//                            if (clazz.VALID_OUT_EDGES.containsKey(e) && clazz.VALID_OUT_EDGES[e]!!.contains(tgt)) {
//                                assertTrue(VertexMapper.checkSchemaConstraints(clazz.LABEL, e, tgt))
//                            } else {
//                                assertFalse(VertexMapper.checkSchemaConstraints(clazz.LABEL, e, tgt))
//                            }
//                        }
//                    }
//                }
//                is TypeDeclVertex -> {
//                    val clazz = TypeDeclVertex
//                    EdgeLabel.values().forEach { e ->
//                        VertexLabel.values().forEach { tgt ->
//                            if (clazz.VALID_OUT_EDGES.containsKey(e) && clazz.VALID_OUT_EDGES[e]!!.contains(tgt)) {
//                                assertTrue(VertexMapper.checkSchemaConstraints(clazz.LABEL, e, tgt))
//                            } else {
//                                assertFalse(VertexMapper.checkSchemaConstraints(clazz.LABEL, e, tgt))
//                            }
//                        }
//                    }
//                }
//                is TypeParameterVertex -> {
//                    val clazz = TypeParameterVertex
//                    EdgeLabel.values().forEach { e ->
//                        VertexLabel.values().forEach { tgt ->
//                            if (clazz.VALID_OUT_EDGES.containsKey(e) && clazz.VALID_OUT_EDGES[e]!!.contains(tgt)) {
//                                assertTrue(VertexMapper.checkSchemaConstraints(clazz.LABEL, e, tgt))
//                            } else {
//                                assertFalse(VertexMapper.checkSchemaConstraints(clazz.LABEL, e, tgt))
//                            }
//                        }
//                    }
//                }
//                is TypeRefVertex -> {
//                    val clazz = TypeRefVertex
//                    EdgeLabel.values().forEach { e ->
//                        VertexLabel.values().forEach { tgt ->
//                            if (clazz.VALID_OUT_EDGES.containsKey(e) && clazz.VALID_OUT_EDGES[e]!!.contains(tgt)) {
//                                assertTrue(VertexMapper.checkSchemaConstraints(clazz.LABEL, e, tgt))
//                            } else {
//                                assertFalse(VertexMapper.checkSchemaConstraints(clazz.LABEL, e, tgt))
//                            }
//                        }
//                    }
//                }
//                is TypeVertex -> {
//                    val clazz = TypeVertex
//                    EdgeLabel.values().forEach { e ->
//                        VertexLabel.values().forEach { tgt ->
//                            if (clazz.VALID_OUT_EDGES.containsKey(e) && clazz.VALID_OUT_EDGES[e]!!.contains(tgt)) {
//                                assertTrue(VertexMapper.checkSchemaConstraints(clazz.LABEL, e, tgt))
//                            } else {
//                                assertFalse(VertexMapper.checkSchemaConstraints(clazz.LABEL, e, tgt))
//                            }
//                        }
//                    }
//                }
//                is UnknownVertex -> {
//                    val clazz = UnknownVertex
//                    EdgeLabel.values().forEach { e ->
//                        VertexLabel.values().forEach { tgt ->
//                            if (clazz.VALID_OUT_EDGES.containsKey(e) && clazz.VALID_OUT_EDGES[e]!!.contains(tgt)) {
//                                assertTrue(VertexMapper.checkSchemaConstraints(clazz.LABEL, e, tgt))
//                            } else {
//                                assertFalse(VertexMapper.checkSchemaConstraints(clazz.LABEL, e, tgt))
//                            }
//                        }
//                    }
//                }
//            }
//        }
    }

}