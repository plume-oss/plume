package io.github.plume.oss.drivers

import io.github.plume.oss.testfixtures.PlumeDriverFixture

class NeptuneIntTests extends PlumeDriverFixture(new NeptuneDriver(hostname = ""))
