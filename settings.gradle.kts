pluginManagement {
    repositories {
        google()  // Remove content filtering for plugins
        mavenCentral()
        gradlePluginPortal()
        // Optional: Keep other repos but without restrictive filters
        maven(url = "https://oss.sonatype.org/content/repositories/snapshots/")
        maven(url = "https://jitpack.io")
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        maven(url = "https://jitpack.io") {
            content {
                includeGroupByRegex("com\\.github.*")
            }
        }
        maven(url = "https://oss.sonatype.org/content/repositories/snapshots/") {
            content {
                includeGroupByRegex("com\\.google.*")
            }
        }
    }
}

rootProject.name = "VMedicine"
include(":app")