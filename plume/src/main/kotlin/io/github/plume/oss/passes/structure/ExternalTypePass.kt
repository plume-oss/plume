package io.github.plume.oss.passes.structure

import io.github.plume.oss.drivers.IDriver
import io.github.plume.oss.passes.IProgramStructurePass
import soot.SootClass

class ExternalTypePass(private val driver: IDriver) : IProgramStructurePass {
    override fun runPass(cs: List<SootClass>): List<SootClass> {
        return cs
    }
}