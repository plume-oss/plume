package io.github.plume.oss.drivers

import io.github.plume.oss.testfixtures.PlumeDriverFixture

class NeptuneIntTests extends PlumeDriverFixture(new NeptuneDriver(hostname = "instance_id.region_name.neptune.amazonaws.com"))