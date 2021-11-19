package io.github.plume.oss.drivers

import io.github.plume.oss.DockerManager
import io.github.plume.oss.testfixtures.PlumeDriverFixture
import io.shiftleft.codepropertygraph.generated.nodes.{Local, NewFile}
import sttp.model.Uri

class TigerGraphIntTests extends PlumeDriverFixture(new TigerGraphDriver()) {

  override def beforeAll(): Unit = {
    DockerManager.startDockerFile("TigerGraph", List("plume-tigergraph"))
    driver.asInstanceOf[IDriver with ISchemaSafeDriver].buildSchema()
    super.beforeAll()
  }

  override def afterAll(): Unit = {
    super.afterAll()
    DockerManager.closeAnyDockerContainers("TigerGraph")
  }

}
