plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "com.ley.wordmemo"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.ley.wordmemo"
        minSdk = 26
        targetSdk = 36
        versionCode = 8
        versionName = "0.5.3"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    signingConfigs {
        create("app") {
            // 项目专属签名 (随仓库分发, 保证任意机器构建的 APK 签名一致, 可互相覆盖安装)
            storeFile = rootProject.file("keystore/wordmemo.keystore")
            storePassword = "wordmemo"
            keyAlias = "wordmemo"
            keyPassword = "wordmemo"
        }
    }

    buildTypes {
        debug {
            signingConfig = signingConfigs.getByName("app")
        }
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("app")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
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
        }
    }
    buildFeatures {
        compose = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    // Core
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.activity.compose)

    // Compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons)

    // Navigation
    implementation(libs.androidx.navigation.compose)

    // Room
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)

    // Network / AI API
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.retrofit)
    implementation(libs.retrofit.kotlinx.serialization)

    // Image / Camera
    implementation(libs.coil.compose)
    implementation(libs.camx.core)
    implementation(libs.camx.camera2)
    implementation(libs.camx.lifecycle)
    implementation(libs.camx.view)

    // DataStore
    implementation(libs.androidx.datastore.preferences)

    // Permissions
    implementation(libs.accompanist.permissions)

    // Miuix (HyperOS 风格 Compose 组件库, KernelSU 系管理器同款)
    implementation(libs.miuix)

    // Debug
    debugImplementation(libs.androidx.ui.tooling)
}