package io.github.plume.oss.drivers

import io.github.plume.oss.testfixtures.PlumeDriverFixture

class NeptuneIntTests extends PlumeDriverFixture(new NeptuneDriver(hostname = "neptunedbinstance-w1vsuktascp5.crkb9rixd1vx.eu-west-2.neptune.amazonaws.com"))