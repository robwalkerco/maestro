rootProject.name = "maestro"

pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

include("maestro-utils")
include("maestro-android")
include("maestro-cli")
include("maestro-client")
include("maestro-ios")
include("maestro-ios-driver")
include("maestro-network-proxy")
include("maestro-network-proxy:android")
include("maestro-network-proxy:android-unsafe")
include("maestro-orchestra")
include("maestro-orchestra-models")
include("maestro-orchestra-proto")
include("maestro-proto")
include("maestro-studio:server")
include("maestro-studio:web")
include("maestro-test")
include("maestro-sdk:android")
include("maestro-sdk:android-playground")
//include("examples:samples")
//findProject(":examples:samples")?.name = "samples"
