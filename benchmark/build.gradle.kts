plugins {
    id("com.android.test")
    id("androidx.baselineprofile")
}

android {
    namespace = "com.veilreader.benchmark"
    compileSdk = 37

    defaultConfig {
        minSdk = 28
        targetSdk = 37
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    targetProjectPath = ":app"
    experimentalProperties["android.experimental.self-instrumenting"] = true

    testOptions.managedDevices.devices {
        create<com.android.build.api.dsl.ManagedVirtualDevice>("pixel6Api35") {
            device = "Pixel 6"
            apiLevel = 35
            systemImageSource = "aosp"
        }
    }

    buildTypes {
        create("benchmark") {
            isDebuggable = true
            matchingFallbacks += listOf("benchmark", "release")
        }
    }
}

baselineProfile {
    // Windows CI starts one owned emulator; hosted generation keeps its GMD.
    val connected = providers.gradleProperty("veil.profile.connected").orNull == "true"
    if (!connected) managedDevices += "pixel6Api35"
    useConnectedDevices = connected
}

dependencies {
    implementation("androidx.benchmark:benchmark-macro-junit4:1.5.0")
    implementation("androidx.test:core:1.7.0")
    implementation("androidx.test.ext:junit:1.3.0")
    implementation("androidx.test:runner:1.7.0")
}

