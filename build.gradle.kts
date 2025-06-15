// Top-level build file where you can add configuration options common to all sub-projects/modules.
buildscript {
    repositories {
        google() // Essential for Android plugins
        mavenCentral()
    }
    dependencies {
        // Replace '8.X.Y' with the correct Android Gradle Plugin version
        classpath("com.android.tools.build:gradle:8.10.0") // Example version
    }
}

plugins {
    // Standard Android Application plugin
    id("com.android.application")

    // Standard Kotlin Android plugin
    id("org.jetbrains.kotlin.android")

    // KSP plugin for Kotlin Symbol Processing
    // Ensure the version is correct and available
    id("com.google.devtools.ksp") version "2.0.0-1.0.13"

    // If you are using Kotlinx Serialization, include its plugin
    id("org.jetbrains.kotlin.plugin.serialization") // Correct plugin ID for serialization
}

// ... rest of your build.gradle.kts content (android block, dependencies block, etc.)


tasks.register("clean", Delete::class) {
    delete(rootProject.buildDir)
}