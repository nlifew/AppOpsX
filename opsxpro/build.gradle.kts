plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "com.zzzmode.android.opsxpro"
    compileSdk = 36

    defaultConfig {
        minSdk = 33
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        buildConfig = true
    }
}

val serverDex by configurations.creating {
    isCanBeConsumed = false
    isCanBeResolved = true
}

dependencies {
    api(project(":AdbLib"))
    api(project(":opsxlib"))
    compileOnly(project(":apicompat"))
    serverDex(project(":opsxlib", "serverDexElements"))
}

// 把 opsxlib 生成的服务端 dex jar 放进 assets
abstract class CopyServerDex : DefaultTask() {
    @get:InputFiles
    abstract val dexFiles: ConfigurableFileCollection

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @TaskAction
    fun copy() {
        val dir = outputDir.get().asFile
        dir.deleteRecursively()
        dir.mkdirs()
        dexFiles.forEach { it.copyTo(File(dir, it.name)) }
    }
}

val copyServerDex = tasks.register<CopyServerDex>("copyServerDex") {
    dexFiles.from(serverDex)
}

androidComponents {
    onVariants { variant ->
        variant.sources.assets?.addGeneratedSourceDirectory(copyServerDex, CopyServerDex::outputDir)
    }
}
