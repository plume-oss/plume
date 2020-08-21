package za.ac.sun.plume.drivers

class JanusGraphDriverIntTest : AbstractGremlinDriverTest() {

    override fun provideBuilder(): GremlinDriverBuilder {
        return JanusGraphDriver.Builder("src/test/resources/conf/remote.properties")
    }

    override fun provideHook(): GremlinDriver {
        return try {
            provideBuilder().build() as GremlinDriver
        } catch (e: Exception) {
            throw Exception("Unable to build JanusGraphHook!")
        }
    }

}