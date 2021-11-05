package io.github.plume.oss.drivers

import io.github.plume.oss.DockerManager
import io.github.plume.oss.testfixtures.PlumeDriverFixture

class Neo4jIntTests extends PlumeDriverFixture(new Neo4jDriver()) {

  override def beforeAll(): Unit = {
    DockerManager.startDockerFile("Neo4j", List("plume-neo4j"))
    super.beforeAll()
  }

  override def afterAll(): Unit = {
    super.afterAll()
    DockerManager.closeAnyDockerContainers("Neo4j")
  }

}
