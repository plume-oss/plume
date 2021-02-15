package io.github.plume.oss.util

import io.github.plume.oss.TestDomainResources.Companion.INT_1
import io.github.plume.oss.TestDomainResources.Companion.STRING_1
import io.github.plume.oss.TestDomainResources.Companion.STRING_2
import io.github.plume.oss.drivers.DriverFactory
import io.github.plume.oss.drivers.GraphDatabase
import io.github.plume.oss.drivers.TinkerGraphDriver
import io.shiftleft.codepropertygraph.generated.NodeKeyNames.NAME
import io.shiftleft.codepropertygraph.generated.nodes.File
import io.shiftleft.codepropertygraph.generated.nodes.NewFileBuilder
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import scala.Option

class DiffGraphUtilTest {

    val driver = (DriverFactory(GraphDatabase.TINKER_GRAPH) as TinkerGraphDriver).apply { connect() }

    @Test
    fun addNodeTest() {
        val builder = io.shiftleft.passes.DiffGraph.newBuilder()
        builder.addNode(NewFileBuilder().name(STRING_1).order(INT_1).hash(Option.apply(STRING_2)).build())

        DiffGraphUtil.processDiffGraph(driver, builder.build())
        driver.getWholeGraph().use { g ->
            assertTrue(g.nodes().asSequence().any { it.label() == File.Label() && it.property(NAME) == STRING_1 })
        }
    }

    @Test
    fun removeNodeTest() {
        val f= NewFileBuilder().name(STRING_1).order(INT_1).hash(Option.apply(STRING_2))
        driver.addVertex(f)
        driver.getWholeGraph().use { g ->
            assertTrue(g.nodes().asSequence().any { it.label() == File.Label() && it.property(NAME) == STRING_1 })
        }
        val builder = io.shiftleft.passes.DiffGraph.newBuilder()
        builder.removeNode(f.id())

        DiffGraphUtil.processDiffGraph(driver, builder.build())
        driver.getWholeGraph().use { g ->
            assertTrue(g.nodes().asSequence().none { it.label() == File.Label() && it.property(NAME) == STRING_1 })
        }
    }

}