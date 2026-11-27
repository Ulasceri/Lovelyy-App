import java.util.Properties

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

// Release (upload) key for Google Play. Never committed: the passwords live in android/keystore.properties
// (gitignored) or in environment variables, the .jks file outside the repo. See docs/play-console.md step 1.
// Google Play re-signs the app with its own app-signing key; this key only proves the upload is ours.
val keystoreProps = Properties().apply {
    val f = rootProject.file("keystore.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}
fun signingValue(key: String, env: String): String? =
    keystoreProps.getProperty(key)?.takeIf { it.isNotBlank() } ?: System.getenv(env)?.takeIf { it.isNotBlank() }
val uploadStoreFile = signingValue("storeFile", "LOVELYY_UPLOAD_STORE_FILE")
val hasUploadKey = uploadStoreFile != null && file(uploadStoreFile).exists()

android {
    namespace = "com.lovelyy.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.lovelyy.app"
        minSdk = 26
        targetSdk = 36
        // Raise versionCode by one for every upload to Google Play (Play refuses a code it has seen).
        versionCode = 1
        versionName = "1.0.0"

        // Telemetry (analytics, crash reports, performance) default when the user hasn't chosen.
        // Release: on. Debug: off, so test runs don't pollute the data. See docs/analytics.md.
        buildConfigField("boolean", "TELEMETRY_DEFAULT_ENABLED", "true")
        buildConfigField("boolean", "PLUS_FAKE_BILLING", "false")
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
        if (hasUploadKey) {
            create("release") {
                storeFile = file(uploadStoreFile!!)
                storePassword = signingValue("storePassword", "LOVELYY_UPLOAD_STORE_PASSWORD")
                keyAlias = signingValue("keyAlias", "LOVELYY_UPLOAD_KEY_ALIAS") ?: "upload"
                keyPassword = signingValue("keyPassword", "LOVELYY_UPLOAD_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        debug {
            signingConfig = signingConfigs.getByName("debug")
            // Turn telemetry on in a debug build with: ./gradlew assembleDebug -Ptelemetry.debug=true
            // (or at runtime, see docs/analytics.md).
            val debugTelemetry = (project.findProperty("telemetry.debug") as String?)?.toBoolean() ?: false
            buildConfigField("boolean", "TELEMETRY_DEFAULT_ENABLED", debugTelemetry.toString())
            // Lovelyy Plus without Google Play: ./gradlew assembleDebug -Pplus.fakeBilling=true always fakes the
            // purchase; without the flag a debug build fakes it only when the Play products can't be loaded
            // (emulator, no Play listing yet). Never in release. See docs/architecture.md "Lovelyy Plus".
            val fakeBilling = (project.findProperty("plus.fakeBilling") as String?)?.toBoolean() ?: false
            buildConfigField("boolean", "PLUS_FAKE_BILLING", fakeBilling.toString())
        }
        release {
            // Without the upload key the release build still compiles, just unsigned (Play won't accept it).
            if (hasUploadKey) {
                signingConfig = signingConfigs.getByName("release")
            } else {
                logger.warn("⚠️  No upload key (android/keystore.properties): the release build is unsigned. See docs/play-console.md.")
            }
            // R8: shrinks and obfuscates code and drops unused resources. Libraries bring their own keep rules;
            // the app's few extra ones are in proguard-rules.pro. Crashlytics uploads the mapping file on build.
            isMinifyEnabled = true
            isShrinkResources = true
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
    // App Check: Firestore only answers the real app (Play Integrity); debug builds use a debug token.
    // See docs/security.md.
    implementation(libs.firebase.appcheck.playintegrity)
    debugImplementation(libs.firebase.appcheck.debug)
    implementation(libs.timber)
    implementation(libs.kotlinx.coroutines.play.services)

    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services)
    implementation(libs.googleid)

    // The cat: video clips (assets/cat) and the home-screen widget's background refresh.
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.work.runtime)

    // Lovelyy Plus purchases.
    implementation(libs.android.billing.ktx)

    testImplementation(libs.junit)
}
