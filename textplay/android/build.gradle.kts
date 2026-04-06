/**
 * TextPlay - Android build configuration
 *
 * Integration approach: This module is designed to be added to the
 * google-ai-edge/gallery project as a custom task module.
 *
 * Quick integration steps:
 * 1. Copy the `textplay` package into the gallery's customtasks directory
 * 2. Copy frontend assets to app/src/main/assets/textplay/
 * 3. Register TextPlayTaskModule in the app's Hilt configuration
 * 4. Add "llm_textplay" to the model allowlist taskTypes
 */

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt)
    alias(libs.plugins.kotlin.serialization)
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.edgeai.textplay"
    compileSdk = 35

    defaultConfig {
        minSdk = 31  // Android 12+ (matches Gallery requirement)
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    // LiteRT-LM: on-device LLM inference with function calling
    implementation(libs.litertlm)

    // Compose UI
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.material3)
    implementation(libs.compose.ui)

    // WebView support
    implementation(libs.webkit)

    // Hilt DI
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)

    // Kotlin serialization (for WebView command JSON)
    implementation(libs.kotlinx.serialization.json)

    // Lifecycle
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.lifecycle.runtime.compose)
}
