// 通过 Shizuku UserService 以 shell/root 身份运行 OpsxService，并向 app 提供 OpsxManager
plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "com.zzzmode.android.opsxpro"
    compileSdk = 36

    defaultConfig {
        minSdk = 33
        consumerProguardFiles("consumer-rules.pro")
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        aidl = true
        buildConfig = true
    }
}

dependencies {
    api(project(":opsxlib"))
    api(libs.shizuku.api)
    implementation(libs.shizuku.provider)
    compileOnly(project(":apicompat"))
}
