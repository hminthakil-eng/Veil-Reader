pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
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

include(":rd-reader-labs")
