import java.util.Properties
import java.net.URI

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}

val apiMode = localProperties.getProperty("API_MODE", "remote").trim().lowercase()
require(apiMode in setOf("mock", "remote")) { "API_MODE must be mock or remote" }
val configuredBaseUrl = localProperties.getProperty("BASE_URL", "http://10.0.2.2:8080/api/v1").trim().trimEnd('/')
val releaseUrl = localProperties.getProperty("RELEASE_BASE_URL", "https://localhost/api/v1").trim().trimEnd('/')
val releaseUri = URI(releaseUrl)
require(releaseUri.scheme == "https" && releaseUri.host != null && releaseUri.userInfo == null &&
    releaseUri.query == null && releaseUri.fragment == null) { "RELEASE_BASE_URL must be an HTTPS URL" }
val apiUri = runCatching { URI(configuredBaseUrl) }.getOrElse { error("BASE_URL must be a valid URI") }
require(apiUri.scheme?.lowercase() in setOf("http", "https") && apiUri.host != null &&
    apiUri.userInfo == null && apiUri.query == null && apiUri.fragment == null) { "BASE_URL must be an HTTP(S) API base URL" }
// Existing local configurations used just the server origin; keep them usable with the contract.
val apiBaseUrl = if (apiUri.path.isNullOrEmpty()) "$configuredBaseUrl/api/v1" else configuredBaseUrl

android {
    namespace = "com.ghostteam.findetect"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.ghostteam.findetect"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Runtime tokens belong to SessionManager; local.properties only selects the transport/address.
        buildConfigField("String", "BASE_URL", "\"$apiBaseUrl\"")
        buildConfigField("boolean", "USE_MOCK_API", "false")
    }

    buildTypes {
        debug {
            buildConfigField("boolean", "USE_MOCK_API", (apiMode == "mock").toString())
        }
        release {
            buildConfigField("String", "BASE_URL", "\"$releaseUrl\"")
            optimization {
                enable = false
            }
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // Provide the release key through Android Studio’s signed bundle wizard.
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    lint {
        abortOnError = false
        warningsAsErrors = false
        checkReleaseBuilds = true
        htmlReport = true
        htmlOutput = layout.buildDirectory.file("reports/lint-report.html").get().asFile
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    // Compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.lottie.compose)

    // AndroidX
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.security.crypto)

    // DI (Hilt)
    implementation(libs.hilt.android)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.androidx.hilt.work)
    ksp(libs.hilt.compiler)
    ksp(libs.androidx.hilt.compiler)

    // База данных и фоновые задачи
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)
    implementation(libs.androidx.work.runtime.ktx)

    // Сеть
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.okhttp)
    debugImplementation(libs.ktor.client.mock)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.ktor.client.logging)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.datetime)

    // Тесты
    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
