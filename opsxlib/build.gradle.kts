// 服务端：经 app_process 以 shell/root 身份运行。
// 除了作为普通库给 opsxpro 编译使用外，还会把 server/common 两个包单独打成 dex jar，
// 作为 opsxpro 的 assets（appopsx.jar）在运行时释放并加载。
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

val serverClassesJar = tasks.register<Jar>("serverClassesJar") {
    from(tasks.named<JavaCompile>("compileReleaseJavaWithJavac").flatMap { it.destinationDirectory })
    include("com/zzzmode/appopsx/server/**", "com/zzzmode/appopsx/common/**")
    archiveFileName.set("appopsx-classes.jar")
    destinationDirectory.set(layout.buildDirectory.dir("serverDex"))
}

val serverDex = tasks.register<JavaExec>("serverDex") {
    val input = serverClassesJar.flatMap { it.archiveFile }
    val output = layout.buildDirectory.file("serverDex/out/appopsx.jar")
    val d8Jar = androidComponents.sdkComponents.sdkDirectory.map {
        it.file("build-tools/${android.buildToolsVersion}/lib/d8.jar")
    }
    val bootClasspath = androidComponents.sdkComponents.bootClasspath

    inputs.file(input)
    outputs.file(output)
    classpath(d8Jar)
    mainClass.set("com.android.tools.r8.D8")
    argumentProviders.add(CommandLineArgumentProvider {
        bootClasspath.get().flatMap { listOf("--lib", it.asFile.path) } +
            listOf("--release", "--min-api", "33", "--output", output.get().asFile.path, input.get().asFile.path)
    })
    doFirst { output.get().asFile.parentFile.mkdirs() }
}

// 供 opsxpro 以 project(":opsxlib", "serverDexElements") 方式消费
val serverDexElements by configurations.creating {
    isCanBeConsumed = true
    isCanBeResolved = false
}
artifacts.add(serverDexElements.name, layout.buildDirectory.file("serverDex/out/appopsx.jar")) {
    builtBy(serverDex)
}
