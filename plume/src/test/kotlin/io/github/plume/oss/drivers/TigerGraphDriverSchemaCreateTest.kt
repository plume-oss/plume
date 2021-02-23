package io.github.plume.oss.drivers

import io.shiftleft.codepropertygraph.generated.EdgeTypes
import io.shiftleft.codepropertygraph.generated.NodeKeyNames
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import kotlin.properties.Delegates

class TigerGraphDriverSchemaCreateTest {

    companion object {
        lateinit var driver: TigerGraphDriver
        private var testStartTime by Delegates.notNull<Long>()

        @JvmStatic
        @BeforeAll
        fun setUpAll() {
            testStartTime = System.nanoTime()
            driver = (DriverFactory(GraphDatabase.TIGER_GRAPH) as TigerGraphDriver)
                .username("tigergraph")
                .password("tigergraph")
                .hostname("127.0.0.1")
                .restPpPort(9000)
                .gsqlPort(14240)
                .secure(false)
            Assertions.assertEquals("127.0.0.1", driver.hostname)
            Assertions.assertEquals(9000, driver.restPpPort)
            Assertions.assertEquals(false, driver.secure)
        }

        @JvmStatic
        @AfterAll
        fun tearDownAll() {
            println("${TigerGraphDriverIntTest::class.java.simpleName} completed in ${(System.nanoTime() - testStartTime) / 1e6} ms")
            driver.close()
        }
    }


    @Test
    fun testPayloadContents() {
        val payload = driver.buildSchemaPayload()
        NodeKeyNames.ALL.filterNot { it == "NODE_LABEL" }.map(payload::contains).forEach(Assertions::assertTrue)
        EdgeTypes.ALL.map(payload::contains).forEach(Assertions::assertTrue)
    }

    @Test
    fun testPayloadDelivery() {
        driver.buildSchema()
    }
}