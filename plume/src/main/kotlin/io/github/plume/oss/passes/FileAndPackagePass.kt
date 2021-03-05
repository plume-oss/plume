package io.github.plume.oss.passes

import io.github.plume.oss.drivers.IDriver
import soot.SootClass

class FileAndPackagePass(private val driver: IDriver) : IProgramStructurePass {
    override fun runPass(cs: List<SootClass>): List<SootClass> {
        val packageNames = cs.map { it.javaPackageName }.distinct().toList()

        return cs
    }
}