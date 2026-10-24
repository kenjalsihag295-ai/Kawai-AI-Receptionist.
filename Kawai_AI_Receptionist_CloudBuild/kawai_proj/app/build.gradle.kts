plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.kawai.aireceptionist"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.kawai.aireceptionist"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "0.1"
    }
}
