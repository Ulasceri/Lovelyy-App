plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

// Firebase config: the google-services plugin needs app/google-services.json.
// Until you create a Firebase project (see README.md), the app still builds and
// shows a "Firebase setup needed" screen instead of crashing.
if (file("google-services.json").exists()) {
    apply(plugin = libs.plugins.google.services.get().pluginId)
    // Crash reports (Crashlytics) and app-start / network traces (Performance Monitoring).
    apply(plugin = libs.plugins.firebase.crashlytics.get().pluginId)
    apply(plugin = libs.plugins.firebase.perf.get().pluginId)
} else {
    logger.warn("⚠️  app/google-services.json not found: building without Firebase. See README.md.")
}

android {
    namespace = "com.lovelyy.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.lovelyy.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"

        // Telemetry (analytics, crash reports, performance) default when the user hasn't chosen.
        // Release: on. Debug: off, so test runs don't pollute the data. See docs/analytics.md.
        buildConfigField("boolean", "TELEMETRY_DEFAULT_ENABLED", "true")
    }

    // App-specific debug key, committed to the repo, so every machine has the same
    // SHA-1 registered in Firebase (needed for Google sign-in). Debug only.
    signingConfigs {
        getByName("debug") {
            storeFile = file("debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }

    buildTypes {
        debug {
            signingConfig = signingConfigs.getByName("debug")
            // Turn telemetry on in a debug build with: ./gradlew assembleDebug -Ptelemetry.debug=true
            // (or at runtime, see docs/analytics.md).
            val debugTelemetry = (project.findProperty("telemetry.debug") as String?)?.toBoolean() ?: false
            buildConfigField("boolean", "TELEMETRY_DEFAULT_ENABLED", debugTelemetry.toString())
        }
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
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
        buildConfig = true
    }
    androidResources {
        // The cat videos are already compressed; storing them uncompressed lets them stream from the APK.
        noCompress += "mp4"
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    debugImplementation(libs.androidx.compose.ui.tooling)

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    // Telemetry: see docs/analytics.md.
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.crashlytics)
    implementation(libs.firebase.perf)
    implementation(libs.timber)
    implementation(libs.kotlinx.coroutines.play.services)

    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services)
    implementation(libs.googleid)

    // The cat: video clips (assets/cat) and the home-screen widget's background refresh.
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.work.runtime)

    testImplementation(libs.junit)
}
