// Root build used with -Peasynfp.jvmOnly=true: only the pure Kotlin modules
// (:rules, :backup), no Android Gradle Plugin required.
plugins {
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.detekt) apply false
    alias(libs.plugins.ktlint) apply false
}

apply(from = "gradle/quality.gradle")
