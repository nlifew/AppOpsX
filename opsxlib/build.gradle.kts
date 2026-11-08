// 客户端与 OpsxService 共用的数据结构和工具类
plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "com.zzzmode.xlib"
    compileSdk = 36

    defaultConfig {
        minSdk = 33
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    lint {
        abortOnError = false
    }
}

dependencies {
    compileOnly(project(":apicompat"))
}
