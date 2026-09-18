plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
    id("androidx.room")
    id("androidx.baselineprofile")
}

android {
    namespace = "com.veilreader.app"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.veilreader.app"
        minSdk = 26
        targetSdk = 37
        versionCode = 10
        versionName = "0.10.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
        }
        create("benchmark") {
            initWith(getByName("release"))
            signingConfig = signingConfigs.getByName("debug")
            matchingFallbacks += listOf("release")
        }
    }

    // Share the deterministic reader fixture between the explicit benchmark build and the
    // Baseline Profile plugin's generated nonMinifiedRelease target. Normal debug/release builds
    // do not include this source set or its exported benchmark-only activity.
    sourceSets {
        getByName("benchmark") {
            java.srcDir("src/performance/java")
            manifest.srcFile("src/performance/AndroidManifest.xml")
        }
        maybeCreate("nonMinifiedRelease").apply {
            java.srcDir("src/performance/java")
            manifest.srcFile("src/performance/AndroidManifest.xml")
        }
    }

    buildFeatures {
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        isCoreLibraryDesugaringEnabled = true
    }

    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
}

room {
    schemaDirectory("$projectDir/schemas")
}

baselineProfile {
    saveInSrc = true
    automaticGenerationDuringBuild = false
}

dependencies {
    // Keep AndroidX aligned with the versions used by Readium Kotlin Toolkit 3.3.0.
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.fragment:fragment-ktx:1.8.9")
    implementation("androidx.compose.ui:ui:1.10.5")
    implementation("androidx.compose.ui:ui-tooling-preview:1.10.5")
    implementation("androidx.compose.foundation:foundation:1.10.5")
    implementation("androidx.compose.material3:material3:1.4.0")
    // Stable adaptive window/posture APIs for phone, tablet, desktop-window and foldable layouts.
    implementation("androidx.compose.material3.adaptive:adaptive:1.3.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.10.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.10.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.10.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.10.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")

    // Production local persistence.
    implementation("androidx.room:room-runtime:2.8.5")
    implementation("androidx.room:room-ktx:2.8.5")
    ksp("androidx.room:room-compiler:2.8.5")
    implementation("androidx.datastore:datastore-preferences:1.2.1")
    implementation("androidx.profileinstaller:profileinstaller:1.4.1")

    // Readium powers real EPUB/PDF parsing and navigation.
    implementation("org.readium.kotlin-toolkit:readium-shared:3.3.0")
    implementation("org.readium.kotlin-toolkit:readium-streamer:3.3.0")
    implementation("org.readium.kotlin-toolkit:readium-navigator:3.3.0")
    implementation("org.readium.kotlin-toolkit:readium-adapter-pdfium:3.3.0")

    baselineProfile(project(":benchmark"))

    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.5")

    debugImplementation("androidx.compose.ui:ui-tooling:1.10.5")
    testImplementation("junit:junit:4.13.2")

    // Real Room verification runs on an Android emulator for data-layer changes.
    androidTestImplementation("androidx.test:core:1.7.0")
    androidTestImplementation("androidx.test.ext:junit:1.3.0")
    androidTestImplementation("androidx.room:room-testing:2.8.5")
}