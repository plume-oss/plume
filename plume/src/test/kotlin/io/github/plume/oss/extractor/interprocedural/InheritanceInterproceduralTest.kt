package io.github.plume.oss.extractor.interprocedural

import io.github.plume.oss.Extractor
import io.github.plume.oss.drivers.DriverFactory
import io.github.plume.oss.drivers.GraphDatabase
import io.github.plume.oss.drivers.TinkerGraphDriver
import io.github.plume.oss.options.ExtractorOptions
import io.github.plume.oss.store.LocalCache
import io.shiftleft.codepropertygraph.generated.EdgeTypes.CALL
import io.shiftleft.codepropertygraph.generated.nodes.Call
import io.shiftleft.codepropertygraph.generated.nodes.Method
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import overflowdb.Graph
import java.io.File

class InheritanceInterproceduralTest {

    companion object {
        private val driver = DriverFactory(GraphDatabase.TINKER_GRAPH) as TinkerGraphDriver
        private lateinit var g: Graph
        private var PATH: File
        private var CLS_PATH: File
        private val TEST_PATH = "interprocedural${File.separator}inheritance"

        init {
            val testFileUrl = InheritanceInterproceduralTest::class.java.classLoader.getResource(TEST_PATH.replace(File.separator, "/"))
                ?: throw NullPointerException("Unable to obtain test resource")
            PATH = File(testFileUrl.file)
            CLS_PATH = File(
                PATH.absolutePath.replace(System.getProperty("user.dir") + File.separator, "").replace(TEST_PATH, "")
            )
        }
    }

    @AfterEach
    fun tearDown() {
        LocalCache.clear()
        driver.close()
        g.close()
    }

    @Test
    fun testNoneInheritance() {
        val extractor = Extractor(driver)
        ExtractorOptions.callGraphAlg = ExtractorOptions.CallGraphAlg.NONE
        // Load test resource and project + export graph
        val f = File(CLS_PATH.absolutePath + File.separator + TEST_PATH)
        extractor.load(f).project()
        g = driver.getWholeGraph()
        // Check calls
        val ns = g.nodes().asSequence().toList()
        val mtd = ns.filterIsInstance<Method>()
        val calls = ns.filterIsInstance<Call>()
        mtd.first { it.fullName() == "interprocedural.inheritance.Base.<init>:void()" }
            .apply { assertNotNull(this) }
        mtd.first { it.fullName() == "interprocedural.inheritance.Derived.<init>:void()" }
            .apply { assertNotNull(this) }
        mtd.first { it.fullName() == "interprocedural.inheritance.Base.show:void()" }
            .apply { assertNotNull(this) }
        mtd.first { it.fullName() == "interprocedural.inheritance.Derived.show:void()" }
            .apply { assertNotNull(this) }
        calls.filter { it.methodFullName() == "interprocedural.inheritance.Base.<init>:void()" }
            .apply { assertEquals(2, this.toList().size) }
            .forEach { assertFalse(g.V(it.id()).next().outE(CALL).hasNext()) }
        calls.filter { it.methodFullName() == "interprocedural.inheritance.Derived.<init>:void()" }
            .apply { assertEquals(2, this.toList().size) }
            .forEach { assertFalse(g.V(it.id()).next().outE(CALL).hasNext()) }
        calls.filter { it.methodFullName() == "interprocedural.inheritance.Base.show:void()" }
            .apply { assertEquals(2, this.toList().size) }
            .forEach { assertFalse(g.V(it.id()).next().outE(CALL).hasNext()) }
        calls.filter { it.methodFullName() == "interprocedural.inheritance.Derived.show:void()" }
            .apply { assertEquals(1, this.toList().size) }
            .forEach { assertFalse(g.V(it.id()).next().outE(CALL).hasNext()) }
    }

    @Test
    fun testCHAInheritance() {
        val extractor = Extractor(driver)
        ExtractorOptions.callGraphAlg = ExtractorOptions.CallGraphAlg.CHA
        // Load test resource and project + export graph
        val f = File(CLS_PATH.absolutePath + File.separator + TEST_PATH)
        extractor.load(f)
        extractor.project()
        g = driver.getWholeGraph()
        // Check calls
        val ns = g.nodes().asSequence().toList()
        val mtd = ns.filterIsInstance<Method>()
        val calls = ns.filterIsInstance<Call>()
        val baseInit =
            mtd.first { it.fullName() == "interprocedural.inheritance.Base.<init>:void()" }
                .apply { assertNotNull(this) }
        val derivedInit =
            mtd.first { it.fullName() == "interprocedural.inheritance.Derived.<init>:void()" }
                .apply { assertNotNull(this) }
        val baseShow =
            mtd.first { it.fullName() == "interprocedural.inheritance.Base.show:void()" }
                .apply { assertNotNull(this) }
        val derivedShow =
            mtd.first { it.fullName() == "interprocedural.inheritance.Derived.show:void()" }
                .apply { assertNotNull(this) }
        calls.filter { it.methodFullName() == "interprocedural.inheritance.Base.<init>:void()" }
            .apply { assertEquals(2, this.toList().size) }
            .forEach { assertTrue(g.V(it.id()).next().out(CALL).asSequence().any { c -> c.id() == baseInit.id() }) }
        calls.filter { it.methodFullName() == "interprocedural.inheritance.Derived.<init>:void()" }
            .apply { assertEquals(2, this.toList().size) }
            .forEach { assertTrue(g.V(it.id()).next().out(CALL).asSequence().any { c -> c.id() == derivedInit.id() }) }
        calls.filter { it.methodFullName() == "interprocedural.inheritance.Base.show:void()" }
            .apply { assertEquals(2, this.toList().size) }
            .forEach { call ->
                val cs = g.V(call.id()).next().out(CALL).asSequence().toList()
                if (cs.size > 1) {
                    assertTrue(cs.any { it.id() == derivedShow.id() })
                    assertTrue(cs.any { it.id() == baseShow.id() })
                } else {
                    assertTrue(cs.any { it.id() == derivedShow.id() })
                }
            }
        calls.filter { it.methodFullName() == "interprocedural.inheritance.Derived.show:void()" }
            .apply { assertEquals(1, this.toList().size) }
            .forEach { c -> assertTrue(g.V(c.id()).next().out(CALL).asSequence().any { it.id() == derivedShow.id() }) }
    }

    @Test
    fun testSPARKInheritance() {
        val extractor = Extractor(driver)
        ExtractorOptions.callGraphAlg = ExtractorOptions.CallGraphAlg.SPARK
        // Load test resource and project + export graph
        val f = File(CLS_PATH.absolutePath + File.separator + TEST_PATH)
        extractor.load(f)
        extractor.project()
        g = driver.getWholeGraph()
        // Check calls
        val ns = g.nodes().asSequence().toList()
        val mtd = ns.filterIsInstance<Method>()
        val calls = ns.filterIsInstance<Call>()
        val baseInit =
            mtd.first { it.fullName() == "interprocedural.inheritance.Base.<init>:void()" }
                .apply { assertNotNull(this) }
        val derivedInit =
            mtd.first { it.fullName() == "interprocedural.inheritance.Derived.<init>:void()" }
                .apply { assertNotNull(this) }
        val baseShow =
            mtd.first { it.fullName() == "interprocedural.inheritance.Base.show:void()" }
                .apply { assertNotNull(this) }
        val derivedShow =
            mtd.first { it.fullName() == "interprocedural.inheritance.Derived.show:void()" }
                .apply { assertNotNull(this) }
        calls.filter { it.methodFullName() == "interprocedural.inheritance.Base.<init>:void()" }
            .apply { assertEquals(2, this.toList().size) }
            .forEach { c -> assertTrue(g.V(c.id()).next().out(CALL).asSequence().any { it.id() == baseInit.id() }) }
        calls.filter { it.methodFullName() == "interprocedural.inheritance.Derived.<init>:void()" }
            .apply { assertEquals(2, this.toList().size) }
            .forEach { c -> assertTrue(g.V(c.id()).next().out(CALL).asSequence().any { it.id() == derivedInit.id() }) }
        calls.filter { it.methodFullName() == "interprocedural.inheritance.Base.show:void()" }
            .apply { assertEquals(2, this.toList().size) }
            .let { cs ->
                assertTrue(cs.any { c ->
                    g.V(c.id()).next().out(CALL).asSequence().any { it.id() == derivedShow.id() }
                })
                assertTrue(cs.any { c -> g.V(c.id()).next().out(CALL).asSequence().any { it.id() == baseShow.id() } })
            }
        calls.filter { it.methodFullName() == "interprocedural.inheritance.Derived.show:void()" }
            .apply { assertEquals(1, this.toList().size) }
            .let { cs ->
                assertTrue(cs.any { c ->
                    g.V(c.id()).next().out(CALL).asSequence().any { it.id() == derivedShow.id() }
                })
            }
    }
}