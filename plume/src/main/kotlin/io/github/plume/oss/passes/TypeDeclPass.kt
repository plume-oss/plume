package io.github.plume.oss.passes

import io.github.plume.oss.drivers.IDriver
import soot.SootClass

class TypeDeclPass(private val driver: IDriver) : IProgramStructurePass {
    override fun runPass(cs: List<SootClass>): List<SootClass> {
        TODO("Not yet implemented")
    }
}