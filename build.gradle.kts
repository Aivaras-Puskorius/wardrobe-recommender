buildscript {
    repositories {
        mavenCentral()
    }
    dependencies {
        // AGP 9 bundles Kotlin Gradle Plugin 2.2.10. This raises it to the catalog's
        // Kotlin version so it matches the Compose compiler plugin.
        // https://developer.android.com/build/releases/agp-9-0-0-release-notes
        classpath(libs.kotlin.gradle.plugin)
    }
}

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.roborazzi) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.room) apply false
}
