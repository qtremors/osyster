pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
dependencyResolutionManagement {
    // Modules may not declare their own repositories.
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "Osyster"
include(":app")
include(":core:runtime")
include(":core:navigation:api")
include(":core:device:domain")
include(":core:device:data")
include(":core:usage:domain")
include(":core:usage:data")
include(":core:system:domain")
include(":core:system:data")
include(":core:preferences:domain")
include(":core:ui")
include(":feature:home")
include(":feature:device")
include(":feature:datausage")
include(":feature:screenusage")
include(":feature:appstopper")
include(":feature:stayawake")
include(":feature:flashlight")
include(":feature:activitylauncher")
include(":feature:linkactions")
include(":feature:quicktiles")
include(":feature:musicwidgets")
include(":feature:onboarding")
include(":feature:search")
include(":feature:tools")
include(":feature:settings")
