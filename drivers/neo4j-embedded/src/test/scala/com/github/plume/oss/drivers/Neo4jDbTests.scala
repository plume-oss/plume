package com.github.plume.oss.drivers

import com.github.plume.oss.testfixtures.PlumeDriverFixture

class Neo4jDbTests extends PlumeDriverFixture(new Neo4jEmbeddedDriver()) {}
