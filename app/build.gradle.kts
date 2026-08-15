import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

// Signing config: keystore path + passwords read from system properties or env.
// CI sets these via GitHub Secrets. Local builds can pass -PkeystoreFile=... etc.
fun getSigningProp(name: String): String? {
    val sysProp = (project.findProperty(name) as? String)?.takeIf { it.isNotBlank() }
    val envName = when (name) {
        "keystoreFile" -> "KEYSTORE_FILE"
        "keystorePassword" -> "KEYSTORE_PASSWORD"
        "keyAlias" -> "KEY_ALIAS"
        "keyPassword" -> "KEY_PASSWORD"
        else -> name
    }
    return sysProp ?: System.getenv(envName)?.takeIf { it.isNotBlank() }
}

val keystoreFile = getSigningProp("keystoreFile")
val hasSigning = keystoreFile != null && getSigningProp("keystorePassword") != null

android {
    namespace = "ai.liquid.leapchat"
    compileSdk = 36

    defaultConfig {
        applicationId = "ai.liquid.leapchat"
        minSdk = 31
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        if (hasSigning) {
            create("release") {
                storeFile = file(keystoreFile!!)
                storePassword = getSigningProp("keystorePassword")
                keyAlias = getSigningProp("keyAlias")
                keyPassword = getSigningProp("keyPassword") ?: getSigningProp("keystorePassword")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            if (hasSigning) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlin {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.runtime.livedata)
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.leap.sdk)
    implementation(libs.leap.model.downloader)
    implementation(libs.compose.markdown)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}

