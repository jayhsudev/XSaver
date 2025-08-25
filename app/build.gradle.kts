plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.devtools.ksp)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "org.jayhsu.xsaver"
    compileSdk = 36

    defaultConfig {
        applicationId = "org.jayhsu.xsaver"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
    // Use AndroidX test runner (fixes 'No tests found' when running JUnit4 @Test with default legacy runner)
    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        // Load API base URL from gradle.properties with a sane default and expose via BuildConfig
        val apiBaseUrl = providers.gradleProperty("API_BASE_URL").orElse("https://api.yourdomain.com").get()
        buildConfigField("String", "API_BASE_URL", "\"$apiBaseUrl\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    kotlin {
        jvmToolchain(21)
        target {
            compilerOptions {
                optIn.add("kotlin.RequiresOptIn")
            }
        }
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    // Material Components (for XML themes/styles)
    implementation(libs.material)
    // AppCompat for ApplicationLocales
    implementation(libs.androidx.appcompat)
    // Navigation
    implementation(libs.androidx.navigation.compose)
    // Hilt Dependency Injection
    implementation(libs.dagger.hilt.android)
    ksp(libs.dagger.hilt.android.compiler)
    implementation(libs.androidx.hilt.navigation.compose)
    // Image Loading
    implementation(libs.coil.kt.coil.compose)
    // Networking
    implementation(libs.squareup.retrofit)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.retrofit2.kotlinx.serialization.converter)
    implementation(libs.squareup.okhttp)
    implementation(libs.squareup.logging.interceptor)
    // Room Database
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    // Permissions
    implementation(libs.accompanist.permissions)
    // DataStore Preferences
    implementation(libs.androidx.datastore.preferences)
    // Media playback
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.ui)
    // HTML parsing
    implementation(libs.jsoup)
    implementation(libs.kotlinx.coroutines.android)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    androidTestImplementation(libs.androidx.test.core)
    // Explicit test runner & rules to ensure instrumentation picks up JUnit4 tests
    androidTestImplementation("androidx.test:runner:1.6.1")
    androidTestImplementation("androidx.test:rules:1.6.1")
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

}