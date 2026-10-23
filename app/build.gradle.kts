plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

// Release signing (SPEC §8, D5). The four PD_* keys live in ~/.gradle/gradle.properties, never in
// the repo. When any is missing the signing config is not attached, and release packaging fails
// with checkReleaseSigning instead, so test / lint / assembleDebug still work without them.
val releaseSigningKeys = listOf("PD_STORE_FILE", "PD_STORE_PASSWORD", "PD_KEY_ALIAS", "PD_KEY_PASSWORD")
val releaseSigning = releaseSigningKeys.associateWith { providers.gradleProperty(it).orNull?.takeIf(String::isNotBlank) }
val missingReleaseSigningKeys = releaseSigning.filterValues { it == null }.keys.toList()

android {
    namespace = "dev.armaan.pd"
    compileSdk = 37
    buildToolsVersion = "37.0.0"

    defaultConfig {
        applicationId = "dev.armaan.pd"
        minSdk = 28
        targetSdk = 37
        versionCode = 1
        versionName = "0.1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        if (missingReleaseSigningKeys.isEmpty()) {
            create("release") {
                storeFile = file(releaseSigning.getValue("PD_STORE_FILE")!!)
                storePassword = releaseSigning.getValue("PD_STORE_PASSWORD")
                keyAlias = releaseSigning.getValue("PD_KEY_ALIAS")
                keyPassword = releaseSigning.getValue("PD_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            if (missingReleaseSigningKeys.isEmpty()) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }
}

val checkReleaseSigning = tasks.register("checkReleaseSigning") {
    group = "verification"
    description = "Fails when any PD_* release signing property is missing."
    val missing = missingReleaseSigningKeys
    doLast {
        if (missing.isNotEmpty()) {
            throw GradleException(
                "Release signing is not configured. Missing Gradle properties: " +
                    missing.joinToString(", ") +
                    ". Set them in ~/.gradle/gradle.properties (see CLAUDE.md, Release).",
            )
        }
    }
}

tasks.configureEach {
    if (name == "packageRelease" || name == "bundleRelease" || name == "assembleRelease") {
        dependsOn(checkReleaseSigning)
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    implementation(project(":core"))

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(libs.kotlinx.coroutines.core)

    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(kotlin("test-junit"))

    constraints {
        androidTestImplementation(libs.androidx.test.espresso.core) {
            because("ui-test pulls espresso 3.5, which calls InputManager.getInstance, removed on API 37")
        }
    }
}
