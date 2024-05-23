package com.github.plume.oss.drivers

import com.github.plume.oss.DockerManager
import com.github.plume.oss.testfixtures.PlumeDriverFixture

class Neo4jIntTests extends PlumeDriverFixture(new Neo4jDriver()) {

  override def beforeAll(): Unit = {
    DockerManager.startDockerFile("Neo4j", List("plume-neo4j"))
    driver.asInstanceOf[IDriver & ISchemaSafeDriver].buildSchema()
  }

  override def afterAll(): Unit = {
    DockerManager.closeAnyDockerContainers("Neo4j")
  }

}
