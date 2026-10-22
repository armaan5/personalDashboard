import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// Pure Kotlin/JVM: no Android plugin, so `android.*` cannot be imported here (PRD A2, D10).
plugins {
    alias(libs.plugins.kotlin.jvm)
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_17
    }
}

dependencies {
    testImplementation(kotlin("test-junit"))
    testImplementation(libs.junit)
}
