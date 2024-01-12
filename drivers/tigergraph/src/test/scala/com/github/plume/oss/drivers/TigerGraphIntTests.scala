package com.github.plume.oss.drivers

import com.github.plume.oss.DockerManager
import com.github.plume.oss.testfixtures.PlumeDriverFixture

class TigerGraphIntTests extends PlumeDriverFixture(new TigerGraphDriver()) {

  override def beforeAll(): Unit = {
    DockerManager.startDockerFile("TigerGraph", List("plume-tigergraph"))
    driver.asInstanceOf[IDriver with ISchemaSafeDriver].buildSchema()
  }

  override def afterAll(): Unit = {
    DockerManager.closeAnyDockerContainers("TigerGraph")
  }

}
