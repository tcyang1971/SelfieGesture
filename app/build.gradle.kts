plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "tw.edu.pu.csim.tcyang.selfiegesture"
    compileSdk = 35

    defaultConfig {
        applicationId = "tw.edu.pu.csim.tcyang.selfiegesture"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
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
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    implementation("com.google.accompanist:accompanist-permissions:0.37.3")

    val cameraxVersion = "1.4.2" // 請檢查最新的穩定版本

    //CameraX 的核心
    implementation("androidx.camera:camera-core:$cameraxVersion")
    //提供CameraX與底層Android camera2 API相機硬體的實現
    implementation("androidx.camera:camera-camera2:$cameraxVersion")

    // 自動管理CameraX用例的生命週期
    implementation("androidx.camera:camera-lifecycle:$cameraxVersion")
    // 包含PreviewView，用於顯示相機預覽
    implementation("androidx.camera:camera-view:$cameraxVersion")

    // MediaPipe Tasks Vision
    implementation("com.google.mediapipe:tasks-vision:0.10.21")// 請檢查最新版本
}