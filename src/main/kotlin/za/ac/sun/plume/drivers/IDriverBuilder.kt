package za.ac.sun.plume.drivers

interface IDriverBuilder {
    @Throws(Exception::class)
    fun build(): IDriver?
}