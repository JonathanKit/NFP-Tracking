// Lint-only project for -Peasynfp.jvmOnly=true: ktlint and detekt over the Android modules' sources.
// Only the ktlintCheck / ktlintFormat / detekt tasks are meant to be used; nothing here compiles.
plugins {
    alias(libs.plugins.kotlin.jvm)
}

val androidModules = listOf("data", "widget", "app")

kotlin.sourceSets.named("main") {
    kotlin.setSrcDirs(androidModules.map { rootDir.resolve("$it/src/main/kotlin") })
}
kotlin.sourceSets.named("test") {
    kotlin.setSrcDirs(androidModules.map { rootDir.resolve("$it/src/test/kotlin") })
}

tasks.matching { it.name.startsWith("compile") }.configureEach { enabled = false }

afterEvaluate {
    extensions.configure<io.gitlab.arturbosch.detekt.extensions.DetektExtension> {
        source.setFrom(
            androidModules.flatMap { listOf("$it/src/main/kotlin", "$it/src/test/kotlin") }.map(rootDir::resolve),
        )
    }
}
