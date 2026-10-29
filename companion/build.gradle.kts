// Dashwheel Companion: the small app on the driver's phone that shares its
// notifications (and later calls) with the launcher over the phone's hotspot.
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

// Same release key as the launcher: CI's env vars, else keystore.properties at the repo root.
apply(from = rootProject.file("gradle/release-signing.gradle.kts"))

// What that script resolved for this module: the key, or null to sign with the debug key.
val releaseKeystore = extra["releaseKeystoreFile"] as? File
val releaseStorePassword = extra["releaseStorePassword"] as? String
val releaseKeyAlias = extra["releaseKeyAlias"] as? String
val releaseKeyPassword = extra["releaseKeyPassword"] as? String

android {
    namespace = "com.openauto.dash.companion"
    compileSdk = 36

    defaultConfig {
        // Same fork identity as the launcher: installs beside the upstream companion.
        applicationId = "com.mydashwheel.dash.companion"
        minSdk = 29
        targetSdk = 36
        versionCode = (System.getenv("VERSION_CODE") ?: "1").toInt()
        versionName = System.getenv("VERSION_NAME") ?: "1.0"
    }

    // Same two editions as the launcher (app/build.gradle.kts). "github" updates
    // itself from the GitHub release; "play" is the Google Play edition: no
    // self-update and no install permission. Gate code on Edition.play.
    flavorDimensions += "edition"
    productFlavors {
        create("github") {
            dimension = "edition"
            buildConfigField("boolean", "PLAY", "false")
        }
        create("play") {
            dimension = "edition"
            // Its own package on Google Play, beside the launcher's com.mydashwheel.dash.play.
            applicationId = "com.mydashwheel.dash.companion.play"
            buildConfigField("boolean", "PLAY", "true")
        }
    }

    signingConfigs {
        if (releaseKeystore != null) {
            create("release") {
                storeFile = releaseKeystore
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = if (releaseKeystore != null) {
                signingConfigs.getByName("release")
            } else {
                signingConfigs.getByName("debug")
            }
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
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
        localeFilters += listOf("en", "fr", "de", "es", "it", "pt", "nl", "pl", "ru", "hu", "lt")
    }

    lint {
        abortOnError = true
        checkReleaseBuilds = false
    }
}

dependencies {
    implementation(project(":link"))
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.activity:activity-compose:1.9.2")
    implementation(platform("androidx.compose:compose-bom:2024.12.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
    // Scans the pairing QR code the launcher shows.
    implementation("com.journeyapps:zxing-android-embedded:4.3.0")
    // Reads the car's registration certificate off its photo, on the phone (RegistrationCard).
    implementation("com.google.android.gms:play-services-mlkit-text-recognition:19.0.1")
    // Road watch: the camera's picture and frames, and the small detector that finds the vehicles ahead.
    implementation("androidx.camera:camera-camera2:1.4.1")
    implementation("androidx.camera:camera-lifecycle:1.4.1")
    implementation("androidx.camera:camera-view:1.4.1")
    // The road recorder: the camera's video in segments, with the time, place and speed drawn into it.
    implementation("androidx.camera:camera-video:1.4.1")
    implementation("androidx.camera:camera-effects:1.4.1")
    implementation("com.google.ai.edge.litert:litert:1.4.0")
    // The detector on the phone's graphics chip where it can run there (RoadDetector).
    implementation("com.google.ai.edge.litert:litert-gpu:1.4.0")
    implementation("com.google.ai.edge.litert:litert-gpu-api:1.4.0")
    // JVM unit tests (companion/src/test): telling a call notification's buttons apart.
    testImplementation("junit:junit:4.13.2")
}
