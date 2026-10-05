import org.cyclonedx.gradle.CyclonedxPlugin

initscript {
    repositories {
        gradlePluginPortal()
    }
    dependencies {
        classpath("org.cyclonedx.bom:org.cyclonedx.bom.gradle.plugin:3.4.1")
    }
    configurations.configureEach {
        resolutionStrategy {
            // CycloneDX 3.4.1 currently resolves Bouncy Castle 1.80.2.
            // CVE-2026-8763 and GHSA-qp49-qgx5-5m26 are fixed in 1.85.
            // Keep the BC family aligned in this CI-only classpath.
            force(
                "org.bouncycastle:bcprov-jdk18on:1.85",
                "org.bouncycastle:bcpkix-jdk18on:1.85",
                "org.bouncycastle:bcutil-jdk18on:1.85"
            )
        }
    }
}

rootProject {
    apply<CyclonedxPlugin>()
}
