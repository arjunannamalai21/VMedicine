// app/build.gradle.kts (or your module's build.gradle.kts)

plugins {
    // THESE ARE THE MODULE-LEVEL PLUGINS
    // Apply the Android Application plugin to this module.
    // The version is implicitly handled by the top-level buildscript's classpath.
    id("com.android.application")

    // Apply the Kotlin Android plugin to enable Kotlin features for Android.
    // The version is implicitly handled by the top-level plugins block.
    id("org.jetbrains.kotlin.android")

    // Apply KSP to this module if you use annotation processing here.
    // The version is implicitly handled by the top-level plugins block.
    id("com.google.devtools.ksp")

    // Apply Kotlinx Serialization plugin to this module if you use serialization here.
    // The version is implicitly handled by the top-level plugins block.
    id("org.jetbrains.kotlin.plugin.serialization")
}

android {
    // ... your android block configuration (compileSdk, defaultConfig, buildFeatures, etc.)
    namespace = "com.example.vmedicine" // Make sure this is set
    compileSdk = 34 // Or latest stable API level

    defaultConfig {
        applicationId = "com.example.vmedicine"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        compose = true // If using Jetpack Compose
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.8" // Update this for your Compose version
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    // ... your module-specific dependencies here
    // Example:
    implementation(platform("androidx.compose:compose-bom:2023.08.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.core:core-ktx:1.10.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.1")
    implementation("androidx.activity:activity-compose:1.7.2")

    // KSP dependencies (e.g., Room Compiler, Dagger Hilt Compiler)
    // ksp("androidx.room:room-compiler:2.6.0")
    // ksp("com.google.dagger:hilt-compiler:2.48")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation(platform("androidx.compose:compose-bom:2023.08.00"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}