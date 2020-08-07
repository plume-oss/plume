package za.ac.sun.plume.hooks

interface IHookBuilder {
    @Throws(Exception::class)
    fun build(): IHook?
}