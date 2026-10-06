// AGP 9.4.0 currently brings Bouncy Castle 1.80.2 into the Gradle plugin classpath.
// Keep the build toolchain on the minimum patched Bouncy Castle family until upstream
// Android tooling adopts >= 1.85. This affects build-time plugin resolution only; it
// does not add Bouncy Castle to the app runtime classpath.
buildscript {
    dependencies {
        constraints {
            classpath("org.bouncycastle:bcprov-jdk18on:1.85")
            classpath("org.bouncycastle:bcutil-jdk18on:1.85")
            classpath("org.bouncycastle:bcpkix-jdk18on:1.85")
        }
    }
}

plugins {
    id("com.android.application") version "9.4.0" apply false
    id("com.android.test") version "9.4.0" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.3.21" apply false
    id("com.google.devtools.ksp") version "2.3.12" apply false
    id("androidx.room") version "2.8.5" apply false
    id("androidx.baselineprofile") version "1.5.0" apply false
}
