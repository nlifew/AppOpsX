// 隐藏 API 的编译期桩，其他模块只以 compileOnly 方式依赖
plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "com.zzzmode.apicompat"
    compileSdk = 36

    defaultConfig {
        minSdk = 33
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        aidl = true
    }
}
