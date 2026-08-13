// The only module permitted to depend on the Android framework.
//
// The shell itself is unchanged from R000 and still displays no organism.
// R001 adds one thing here: an instrumented test that executes the canonical
// core on real ART, which is how the Android half of the determinism matrix is
// qualified. Lifecycle gateway, sensor routing, persistence adapters,
// presentation and platform protection remain later phases.
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
        // R001 determinism qualification runs as an instrumented test: the claim
        // is about ART executing the canonical core, not about the shell UI.
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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

    sourceSets {
        getByName("androidTest") {
            kotlin.srcDir("src/androidTest/kotlin")
        }
    }
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(project(":core-math"))
    implementation(project(":core-crypto"))
    implementation(project(":core-state"))
    implementation(project(":core-continuity"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)

    testImplementation(libs.junit)

    androidTestImplementation(libs.junit)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.runner)
}
