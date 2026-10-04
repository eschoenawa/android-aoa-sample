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

rootProject.name = "android-aoa-sample"

include(":host-app")
include(":accessory-app")
include(":aoa:common")
include(":aoa:host")
include(":aoa:accessory")
