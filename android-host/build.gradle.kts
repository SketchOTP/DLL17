// The only module permitted to depend on the Android framework. It is a shell
// in R000: lifecycle gateway, sensor routing, persistence adapters, presentation
// and platform protection are all later phases.
plugins {
    // AGP 9 provides built-in Kotlin support; the standalone
    // `org.jetbrains.kotlin.android` plugin is rejected by AGP 9.0 and later.
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.animusmachinae.dll17"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.animusmachinae.dll17"
        minSdk = 29
        targetSdk = 37
        versionCode = 1
        versionName = "0.1.0-dev"
    }

    buildTypes {
        // Production signing is deferred. R000 ships development/debug signing
        // only; the release variant exists but is not configured for release
        // key custody.
        debug {
            isMinifyEnabled = false
        }
        release {
            isMinifyEnabled = false
        }
    }

    // No product flavors in R000.

    buildFeatures {
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(project(":core-math"))
    implementation(project(":core-crypto"))
    implementation(project(":core-state"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)

    testImplementation(libs.junit)
}
