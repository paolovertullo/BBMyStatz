pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
                includeGroupByRegex("org.jetbrains.kotlin.*")
            }
        }
        plugins {
            id("org.jetbrains.kotlin.android") version "2.0.0"
            id("org.jetbrains.kotlin.plugin.compose") version "2.0.0"
        }
        mavenCentral()

        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}

rootProject.name = "BBMyStatz"
include(":app")
 