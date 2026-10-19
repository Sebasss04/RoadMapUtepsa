// Align built-in AGP Kotlin with the Compose compiler. Do not apply kotlin-android.
buildscript {
    dependencies { classpath(libs.kotlin.gradle.plugin) }
}

// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false

    id("com.google.gms.google-services") version "4.5.0" apply false
}