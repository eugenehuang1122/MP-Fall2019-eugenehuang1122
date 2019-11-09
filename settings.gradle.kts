include(":app")

pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
    resolutionStrategy {
        eachPlugin {
            if (requested.id.namespace == "edu.illinois.cs.cs125" && requested.version != null) {
                useModule("com.github.cs125-illinois:${requested.id.name}:${requested.version}")
            }
        }
    }
}
