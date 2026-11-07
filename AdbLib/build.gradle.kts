plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "com.cgutman.adblib"
    compileSdk = 36

    defaultConfig {
        minSdk = 33
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
