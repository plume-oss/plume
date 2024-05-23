package com.github.plume.oss.drivers

import com.github.plume.oss.DockerManager
import com.github.plume.oss.testfixtures.PlumeDriverFixture

class TigerGraphIntTests extends PlumeDriverFixture(new TigerGraphDriver()) {

  private implicit val loader: ClassLoader = getClass.getClassLoader

  override def beforeAll(): Unit = {
//    DockerManager.startDockerFile("TigerGraph", List("plume-tigergraph"))
    driver.asInstanceOf[IDriver & ISchemaSafeDriver].buildSchema()
  }

  override def afterAll(): Unit = {
//    DockerManager.closeAnyDockerContainers("TigerGraph")
  }

}
