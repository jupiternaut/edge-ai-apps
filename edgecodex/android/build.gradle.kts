/**
 * EdgeCodex - Android build configuration
 *
 * Integration approach: Add to google-ai-edge/gallery as a custom task module.
 * EdgeCodex uses Gemma 4 E2B in conversational mode (no function calling).
 *
 * Quick integration steps:
 * 1. Copy the `edgecodex` package into gallery's customtasks directory
 * 2. Copy frontend assets to app/src/main/assets/edgecodex/
 * 3. Add "llm_edgecodex" to model allowlist taskTypes for Gemma 4 E2B/E4B
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
    namespace = "com.edgeai.edgecodex"
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
    // LiteRT-LM: on-device LLM inference (conversational mode, no function calling)
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

    // Kotlin serialization (for config JSON)
    implementation(libs.kotlinx.serialization.json)

    // Lifecycle
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.lifecycle.runtime.compose)
}
