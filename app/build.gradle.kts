plugins {
    alias(libs.plugins.android.application)
}

val gitCommitId = providers.exec {
    commandLine("git", "rev-parse", "--short", "HEAD")
    isIgnoreExitValue = true
}.standardOutput.asText.map { it.trim().ifEmpty { "unknown" } }

// 发布签名从环境变量读取，未配置时 release 退回 debug 签名
val releaseKeystore = providers.environmentVariable("APPOPSX_KEYSTORE")

android {
    namespace = "com.zzzmode.appopsx"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.zzzmode.appopsx"
        minSdk = 33
        targetSdk = 35
        versionCode = 125
        versionName = "1.2.5"
        vectorDrawables.useSupportLibrary = true
        manifestPlaceholders["gitCommitId"] = gitCommitId.get()
    }

    signingConfigs {
        if (releaseKeystore.isPresent) {
            create("release") {
                storeFile = file(releaseKeystore.get())
                storePassword = providers.environmentVariable("APPOPSX_KEYSTORE_PASSWORD").orNull
                keyAlias = providers.environmentVariable("APPOPSX_KEY_ALIAS").orNull
                keyPassword = providers.environmentVariable("APPOPSX_KEY_PASSWORD").orNull
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.findByName("release") ?: signingConfigs.getByName("debug")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        buildConfig = true
    }
    lint {
        abortOnError = false
    }
    packaging {
        resources.excludes += listOf("META-INF/LICENSE.txt", "META-INF/NOTICE.txt")
    }
}

dependencies {
    implementation(project(":opsxpro"))
    compileOnly(project(":apicompat"))
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.recyclerview)
    implementation(libs.androidx.preference)
    implementation(libs.androidx.swiperefreshlayout)
    implementation(libs.material)
    implementation(libs.rxjava)
    implementation(libs.rxandroid)
    implementation(libs.tinypinyin)
    implementation(libs.advrecyclerview)
}
