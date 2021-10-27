package io.github.plume.oss.passes

import io.github.plume.oss.drivers.IDriver

trait PlumeCpgPassBase {

  def createAndApply(driver: IDriver): Unit

}
