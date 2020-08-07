package za.ac.sun.plume.hooks

class JanusGraphHookIntTest : AbstractGremlinHookTest() {

    override fun provideBuilder(): GremlinHookBuilder {
        return JanusGraphHook.Builder("src/test/resources/conf/remote.properties")
    }

    override fun provideHook(): GremlinHook {
        return try {
            provideBuilder().build() as GremlinHook
        } catch (e: Exception) {
            throw Exception("Unable to build JanusGraphHook!")
        }
    }

}