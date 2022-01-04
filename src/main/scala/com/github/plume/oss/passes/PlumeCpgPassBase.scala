package com.github.plume.oss.passes

import com.github.plume.oss.drivers.IDriver

trait PlumeCpgPassBase {

  def createAndApply(driver: IDriver): Unit

}
