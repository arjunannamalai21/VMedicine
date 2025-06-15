// Top-level build file where you can add configuration options common to all sub-projects/modules.
// This file primarily handles:
// 1. Declaring the versions of plugins used across the project (via version catalog or directly).
// 2. Defining global repositories for resolving dependencies.
// 3. Setting up the clean task.

// Modern way to declare plugins and their versions for the entire project.
// Note: 'com.android.application' and 'org.jetbrains.kotlin.android' are NOT declared here.
// They are declared in the module-level build.gradle.kts.
plugins {
    // KSP plugin for Kotlin Symbol Processing
    // This plugin often needs to be applied at the project level,
    // and its version should be declared here.
    id("com.google.devtools.ksp") version "1.9.22-1.0.17" // Use a compatible KSP version with AGP 8.3.0 and Kotlin 1.9.22
    // As of June 2025, 2.0.0-1.0.13 is for Kotlin 2.0.0

    // The Kotlin Gradle Plugin (org.jetbrains.kotlin.android) is what *provides* Kotlin for Android.
    // Its version is typically tied to the Kotlin version used.
    // If you're using AGP 8.3.0, you're likely on Kotlin 1.9.22.
    // It's good practice to declare the Kotlin plugin here if you want to apply it to multiple modules
    // or manage its version centrally, although typically it's also added in the module.
    id("org.jetbrains.kotlin.android") version "1.9.22" // Explicitly declare Kotlin plugin version

    // If you are using Kotlinx Serialization, its plugin version should be declared here.
    id("org.jetbrains.kotlin.plugin.serialization") version "1.9.22" // Use the same version as Kotlin
}

// The buildscript block is still used for the Android Gradle Plugin itself,
// as it's a plugin that Gradle needs to load *before* evaluating your project.
buildscript {
    repositories {
        google()        // Essential for Android Gradle Plugin
        mavenCentral()  // Essential for other common dependencies
    }
    dependencies {
        // This is where you define the version of the Android Gradle Plugin (AGP).
        // Ensure this version is compatible with your Android Studio version and Kotlin version.
        // For Android Studio Giraffe/Hedgehog, 8.3.0 is a good choice.
        classpath("com.android.tools.build:gradle:8.3.0")
    }
}

// allprojects block defines repositories for all sub-projects (modules)
// to find their dependencies (libraries, etc.)
allprojects {
    repositories {
        google()        // Essential for Android Jetpack libraries and Google Play services
        mavenCentral()  // Essential for general Maven Central libraries
        // If you have other custom repositories (e.g., for internal libraries), add them here:
        // maven { url "https://your.private.repo" }
    }
}

// Task to clean the build directory of the root project.
tasks.register("clean", Delete::class) {
    delete(rootProject.buildDir)
}