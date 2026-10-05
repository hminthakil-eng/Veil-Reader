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

    buildFeatures {
        compose = true
        buildConfig = true
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }

    sourceSets {
        getByName("androidTest").assets.srcDir("$projectDir/schemas")
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

tasks.withType<org.gradle.api.tasks.testing.Test>().configureEach {
    // Native Robolectric retains process-wide JNI bindings across SDK sandboxes.
    // Give each test class a fresh worker: SDK 35 Skia review must not inherit
    // the SDK 37 persistence suite's native state. All classes still execute.
    forkEvery = 1
    // Robolectric's SDK 37 ApplicationSharedMemory bridge uses FileDescriptor
    // access through this JDK 21 package. Keep the export limited to test JVMs.
    jvmArgs("--add-exports=java.base/jdk.internal.access=ALL-UNNAMED")
}

dependencies {
    // Keep AndroidX aligned with the versions used by Readium Kotlin Toolkit 3.4.0.
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.fragment:fragment-ktx:1.8.9")
    implementation("androidx.compose.ui:ui:1.10.6")
    implementation("androidx.compose.ui:ui-tooling-preview:1.10.6")
    implementation("androidx.compose.foundation:foundation:1.10.6")
    implementation("androidx.compose.material3:material3:1.4.0")
    // Stable adaptive window/posture APIs for phone, tablet, desktop-window and foldable layouts.
    implementation("androidx.compose.material3.adaptive:adaptive:1.3.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.10.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.10.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.10.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.10.0")
    implementation("androidx.metrics:metrics-performance:1.0.0")
    implementation("com.irurueta:irurueta-android-glutils:1.1.11")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")

    // Production local persistence.
    implementation("androidx.room:room-runtime:2.8.5")
    implementation("androidx.room:room-ktx:2.8.5")
    ksp("androidx.room:room-compiler:2.8.5")
    implementation("androidx.datastore:datastore-preferences:1.2.1")
    implementation("androidx.profileinstaller:profileinstaller:1.4.1")

    // Readium powers real EPUB/PDF parsing and navigation.
    implementation("org.readium.kotlin-toolkit:readium-shared:3.4.0")
    implementation("org.readium.kotlin-toolkit:readium-streamer:3.4.0")
    implementation("org.readium.kotlin-toolkit:readium-navigator:3.4.0")
    implementation("org.readium.kotlin-toolkit:readium-adapter-pdfium:3.4.0")
    // Explicit access to the PDFView already used by Readium's Pdfium adapter for manual zoom fallback controls.
    implementation("com.github.marain87:AndroidPdfViewer:3.2.8")

    // Manga image delivery: request-scoped network headers + large-image subsampling.
    implementation("io.coil-kt.coil3:coil-compose:3.6.3")
    implementation("io.coil-kt.coil3:coil-network-okhttp:3.6.3")
    implementation("io.github.panpf.zoomimage:zoomimage-compose-coil3-core:1.5.0")

    baselineProfile(project(":benchmark"))

    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.5")

    debugImplementation("androidx.compose.ui:ui-tooling:1.10.6")
    debugImplementation("androidx.compose.ui:ui-test-manifest:1.10.6")
    testImplementation("junit:junit:4.13.2")
    // Reuse the native Compose test stack for cloud rendering; no production dependency.
    testImplementation("androidx.compose.ui:ui-test-junit4:1.10.6")
    testImplementation("androidx.lifecycle:lifecycle-viewmodel-testing:2.10.0")
    testImplementation("org.robolectric:robolectric:4.17")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.2")

    // Real Room verification runs on an Android emulator for data-layer changes.
    androidTestImplementation("androidx.test:core:1.7.0")
    androidTestImplementation("androidx.test:runner:1.7.0")
    androidTestImplementation("androidx.test.ext:junit:1.3.0")
    androidTestImplementation("androidx.room:room-testing:2.8.5")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4:1.10.6")
}
