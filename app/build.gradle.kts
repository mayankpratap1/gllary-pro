plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.edgellm"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.edgellm"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
        
        ndk { 
            abiFilters += listOf("arm64-v8a", "x86_64") 
        }
        
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildFeatures { 
        compose = true 
    }
    
    packaging {
        resources {
            excludes += setOf(
                "META-INF/ASL-2.0.txt", "META-INF/LGPL-2.1.txt",
                "META-INF/DEPENDENCIES", "META-INF/LICENSE",
                "META-INF/LICENSE.txt", "META-INF/NOTICE",
                "META-INF/NOTICE.txt", "META-INF/INDEX.LIST",
                "META-INF/io.netty.versions.properties",
                "draftv4/schema", "draftv3/schema"
            )
        }
    }
    
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
            freeCompilerArgs.add("-Xskip-metadata-version-check")
        }
    }
}

dependencies {
    // ── Core Android ──
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.4")
    implementation("androidx.activity:activity-compose:1.9.1")

    // ── AI Engines ──
    implementation("com.google.ai.edge.litertlm:litertlm-android:latest.release")
    // Note: llamacpp-kotlin removed until stable build path verified to prevent build blocking

    // ── Ktor server ──
    implementation("io.ktor:ktor-server-cio:3.2.3")
    implementation("io.ktor:ktor-server-core:3.2.3")
    implementation("io.ktor:ktor-server-content-negotiation:3.2.3")
    implementation("io.ktor:ktor-serialization-kotlinx-json:3.2.3")

    // ── Camera (Ask Image) ──
    implementation("androidx.camera:camera-camera2:1.3.4")
    implementation("androidx.camera:camera-lifecycle:1.3.4")
    implementation("androidx.camera:camera-view:1.3.4")

    // ── Compose ──
    implementation(platform("androidx.compose:compose-bom:2024.09.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.navigation:navigation-compose:2.8.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.4")

    // ── Room ──
    implementation("androidx.room:room-runtime:2.7.0")
    implementation("androidx.room:room-ktx:2.7.0")
    ksp("androidx.room:room-compiler:2.7.0")

    // ── Utility ──
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.1")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    // ── Image loading ──
    implementation("io.coil-kt:coil-compose:2.7.0")

    // ── Markdown ──
    implementation("io.noties.markwon:core:4.6.2")
}
