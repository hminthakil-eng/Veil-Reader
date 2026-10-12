pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

// Plugin resolution has its own settings classpath, before the root buildscript
// constraints apply. Load the patched family here as well; keep it build-only.
buildscript {
    repositories { mavenCentral() }
    dependencies {
        classpath("org.bouncycastle:bcprov-jdk18on:1.85")
        classpath("org.bouncycastle:bcutil-jdk18on:1.85")
        classpath("org.bouncycastle:bcpkix-jdk18on:1.85")
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // Readium's PDFium adapter depends on PdfiumAndroid/AndroidPdfViewer artifacts
        // published through JitPack rather than Google Maven or Maven Central.
        maven("https://jitpack.io")
    }
}

rootProject.name = "VeilReader"
include(":app")
include(":benchmark")
