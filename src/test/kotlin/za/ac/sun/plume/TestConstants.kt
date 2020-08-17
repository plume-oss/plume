package za.ac.sun.plume

import java.io.File

object TestConstants {
    val testDir = "${System.getProperty("java.io.tmpdir")}${File.separator}plume"
    val testGraph = "$testDir${File.separator}plume-extractor-test.xml"
}