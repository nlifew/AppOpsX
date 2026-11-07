pluginManagement {
    repositories {
        maven(url = "https://maven.aliyun.com/repository/public/" )
        maven(url = "https://maven.aliyun.com/repository/central" )
        maven(url = "https://maven.aliyun.com/repository/gradle-plugin" )
        maven(url = "https://maven.aliyun.com/repository/google" )

        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
    // application 和 library 插件都在同一个 AGP 构件里，直接解析它，省去单独下载插件 marker
    resolutionStrategy {
        eachPlugin {
            if (requested.id.namespace == "com.android") {
                useModule("com.android.tools.build:gradle:${requested.version}")
            }
        }
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        maven(url = "https://maven.aliyun.com/repository/public/" )
        maven(url = "https://maven.aliyun.com/repository/central" )
        maven(url = "https://maven.aliyun.com/repository/gradle-plugin" )
        maven(url = "https://maven.aliyun.com/repository/google" )
        google()
        mavenCentral()
    }
}

rootProject.name = "AppOpsX"

include(":app")
include(":opsxlib")
include(":opsxpro")
include(":AdbLib")
include(":apicompat")
