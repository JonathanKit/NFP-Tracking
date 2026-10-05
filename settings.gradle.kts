pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google\\.android.*")
                includeGroupByRegex("com\\.google\\.testing.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google\\.android.*")
                includeGroupByRegex("com\\.google\\.testing.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
    }
}

rootProject.name = "easyNFP"

// Pure JVM modules: build and test without the Android SDK.
include(":rules")
include(":backup")

// `-Peasynfp.jvmOnly=true` builds only the JVM modules. Useful where Google Maven
// or the Android SDK is not reachable (e.g. restricted agent sandboxes).
val jvmOnly = providers.gradleProperty("easynfp.jvmOnly").orNull == "true"
if (jvmOnly) {
    rootProject.buildFileName = "build-jvm.gradle.kts"
    // Runs ktlint and detekt on the Android sources without the Android toolchain.
    include(":android-sources-lint")
    project(":android-sources-lint").projectDir = file("gradle/android-sources-lint")
} else {
    include(":data")
    include(":widget")
    include(":app")
}
