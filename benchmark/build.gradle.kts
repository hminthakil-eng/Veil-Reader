plugins {
    id("com.android.test")
    id("androidx.baselineprofile")
    id("com.google.firebase.testlab")
}

val useFirebaseTestLab = providers.gradleProperty("veilUseFirebaseTestLab")
    .map(String::toBoolean)
    .orElse(false)

val firebaseTestLabCredentials = providers.gradleProperty("firebaseTestLabCredentials")
    .orElse(providers.environmentVariable("FIREBASE_TESTLAB_CREDENTIALS"))

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

    buildTypes {
        create("benchmark") {
            isDebuggable = true
            matchingFallbacks += listOf("benchmark", "release")
        }
    }
}

firebaseTestLab {
    if (firebaseTestLabCredentials.isPresent) {
        serviceAccountCredentials.set(file(firebaseTestLabCredentials.get()))
    }

    managedDevices {
        create("ftlDeviceShiba34") {
            device = "shiba"
            apiLevel = 34
        }
    }

    testOptions {
        results {
            directoriesToPull.addAll(
                "/storage/emulated/0/Android/media/${android.namespace}"
            )
        }
    }
}

baselineProfile {
    if (useFirebaseTestLab.get()) {
        managedDevices += "ftlDeviceShiba34"
        useConnectedDevices = false
    } else {
        useConnectedDevices = true
    }
}

dependencies {
    implementation("androidx.benchmark:benchmark-macro-junit4:1.5.0")
    implementation("androidx.test:core:1.7.0")
    implementation("androidx.test.ext:junit:1.3.0")
    implementation("androidx.test:runner:1.7.0")
}
