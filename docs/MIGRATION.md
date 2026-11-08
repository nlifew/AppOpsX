# AppOpsX 迁移计划

原仓库最后一次更新停在 1.2.5（2019 年前后），之后长期无人维护。本文档记录继续维护时的现状评估、已做的决定和分阶段计划，随进度更新。

## 1. 现状（迁移前）

| 项 | 现状 |
|---|---|
| 构建工具 | AGP 3.3.0、Gradle 4.10.3、Java 8、`buildToolsVersion 28.0.3` |
| SDK | compileSdk / targetSdk 28，minSdk 21 |
| 仓库 | 依赖 jcenter（已只读、将关停）、jitpack |
| CI | Travis CI（`oraclejdk8`） |
| 代码量 | 约 1.5 万行 Java |

### 模块

| 模块 | 作用 |
|---|---|
| `app` | UI 层 |
| `opsxpro` | 客户端：负责拉起服务端、与服务端通信（`LocalServerManager`、`OpsxManager`） |
| `opsxlib` | 服务端：以 shell/root 身份经 `app_process` 运行，调用系统隐藏 API |
| `AdbLib` | ADB 协议实现（旧式 TCP 5555 + RSA 认证） |
| `apicompat` | 隐藏 API 的编译期桩（`IAppOpsService`、`AppOpsManager` 等），仅 `compileOnly` |

### 服务端启动链路

1. `opsxlib` 构建完成后，Gradle 脚本从 `intermediates/javac/debug/...` 取 class 文件打 jar，再用 `build-tools/28.0.3/dx` 转成 dex；
2. 产物拷贝到 `opsxpro/libs`（common jar）和 `opsxpro/src/main/assets`（dex jar）；
3. 运行时把 jar 和脚本释放到 `/sdcard/Android/data/com.zzzmode.appopsx/`；
4. 通过 adb（AdbLib 连本机 5555 端口）或 `su` 执行 `app_process` 拉起服务端；
5. 客户端和服务端之间通过 socket、token 握手和自定义序列化进行通信。

## 2. 已确定的决定

| 决定 | 理由 |
|---|---|
| **minSdk 33（Android 13）** | 不再兼容旧版本 SDK 的实现差异。`OpEntry` 公开 API、`AttributedOpEntry`、无线调试在这个基线上都已具备，`sOpNames` 等数组在 13 中重构为 `sAppOpInfos`，以 13 为起点只需适配一套结构。Android 12 及以下的用户请使用原版 1.2.5。 |
| **targetSdk 35** | 发布到应用商店时，按当时的要求再调整 |
| **服务端改为接入 Shizuku**（root 用户通过 Shizuku 的 root 模式或 Sui 使用，不再单独直连 root） | 现有启动链路和 AdbLib 都是 Shizuku 已解决的问题，自研还要实现无线调试配对（SPAKE2 + TLS）。接入后可以删除 `AdbLib`、`opsxlib` 和大部分 `opsxpro`。 |

### 待定

- **applicationId 与签名**：拿不到原作者的签名密钥，无法覆盖安装原版。如果要发布，倾向于换新的 applicationId，需要同步修改写死了 `com.zzzmode.appopsx` 的 provider authority 和路径。

## 3. 分阶段计划

### 阶段 1：修复构建

- 升级到当前的 Gradle、AGP 和 JDK；每个模块声明 `namespace`，删除 `buildToolsVersion`
- 仓库改为 `google()` + `mavenCentral()`，去掉 jcenter；核实 `tinypinyin`、`advrecyclerview` 等依赖的可用性，不可用就替换
- 删除 `app` 的 `providedApiCompat` 任务（解压 aar 生成 `deps/hidden.jar`），改为 `compileOnly project(":apicompat")`
- `opsxlib` 的 jar/dx 打包脚本依赖已删除的 `dx` 和旧的中间产物路径：本阶段先改成能用的形式，阶段 3 接入 Shizuku 后整体删除
- `private/` 中的签名和上传逻辑：去掉 `pushApkFile`（会上传到原作者的服务器），签名改为读取环境变量
- CI 从 Travis 迁移到 GitHub Actions

**完成标准**：在当前环境下 `./gradlew assembleDebug` 能成功。

#### 完成情况（2026-11-08）

`assembleDebug` 和 `assembleRelease` 均已通过（尚未在设备上验证运行）。

- Gradle 9.4.1（腾讯镜像）、AGP 9.2.1、daemon JVM 21（`gradle/gradle-daemon-jvm.properties`），compileSdk 36
- 所有构建脚本改用 Kotlin DSL，依赖版本统一放进 `gradle/libs.versions.toml`
- 仓库：阿里云镜像（public、central、gradle-plugin、google）在前，官方源作为兜底。`settings.gradle.kts` 把 `com.android.*` 插件直接解析到 `com.android.tools.build:gradle`，省去单独下载插件 marker
- 每个模块声明 `namespace`，并去掉 manifest 里的 `package`
- minSdk 33、targetSdk 35 已经写进构建配置；带 intent-filter 的组件按原有行为补上了 `android:exported`。运行时行为的适配仍放在阶段 2
- `opsxlib`：`serverClassesJar` 从 release 编译产物中取出 `server/` 和 `common/` 两个包打成 jar，再由 `serverDex` 调用 SDK build-tools 里的 `lib/d8.jar` 转成 `appopsx.jar`。产物通过 `serverDexElements` 配置交给 `opsxpro`，由 `opsxpro` 的 `copyServerDex` 作为生成的 assets 目录注入，不再拷进源码目录（阶段 3 接入 Shizuku 后已删除）
- `opsxpro` 改为 `api(project(":opsxlib"))`，不再依赖拷贝进来的 `appopsx-common.jar`；`app` 改为 `compileOnly(project(":apicompat"))`，删除了 `providedApiCompat` 任务
- 依赖：去掉 `legacy-preference-v14`，`preference` 升到 1.2.1，显式加入 `swiperefreshlayout`（以前靠 legacy 库间接引入），rxjava 2.2.21、rxandroid 2.1.1；tinypinyin 和 advrecyclerview 可以从阿里云镜像拿到
- AGP 8 起 app 模块的 R 字段不再是 final：把 4 个文件里 `switch (R.id.xxx)` 的写法改成了 `if/else`
- `gradle.properties` 保留了 `android.nonTransitiveRClass=false`：app 代码仍通过自身的 R 类引用依赖库的资源，以后要逐步改掉
- 删除了 `private/`（原作者的加密签名和上传 APK 的脚本）和 `.travis.yml`。release 签名改为读取环境变量 `APPOPSX_KEYSTORE`、`APPOPSX_KEYSTORE_PASSWORD`、`APPOPSX_KEY_ALIAS`、`APPOPSX_KEY_PASSWORD`，没有配置时使用 debug 签名
- 新增 GitHub Actions：`.github/workflows/build.yml`，执行 `assembleDebug` 并上传 APK

#### 本地构建

```sh
# 不需要配置 JAVA_HOME 以外的东西：wrapper 可以用 Android Studio 自带的 JBR 启动，daemon 会自动使用 JDK 21
export JAVA_HOME=".../Android Studio/jbr"
./gradlew assembleDebug
```

### 阶段 2：minSdk 33、targetSdk 35 适配

- `registerReceiver` 带上 `RECEIVER_NOT_EXPORTED` 或 `RECEIVER_EXPORTED`（`AppGlobal.installReceiver`）。**targetSdk 34 起不带会直接抛 `SecurityException`，在 Android 14+ 上很可能一启动就崩溃，优先修**
- 声明 `QUERY_ALL_PACKAGES`（包可见性）
- `AlertInstalledPremActivity` 目前对外导出，需要收紧
- edge-to-edge 处理 insets
- 删除 API 33 以下的版本分支
- ~~`OpsxConfigProvider` 没有权限保护~~、~~不再使用 `/sdcard/Android/data/...` 下的 jar 和脚本~~：阶段 3 已随旧服务端一起删除
- ~~升级或替换过时的 AndroidX 依赖~~：阶段 1 已完成

### 阶段 3：服务端改为接入 Shizuku

- 删除 `AdbLib`、`opsxlib` 的自建服务端以及 `opsxpro` 的启动和通信逻辑
- root 用户同样通过 Shizuku（以 root 启动）或 Sui 使用，不再保留单独的 root 直连

#### 完成情况（2026-11-08）

`assembleDebug` 和 `assembleRelease` 均已通过（尚未在设备上验证运行）。

**方案**：用 Shizuku 的 UserService，而不是计划里写的 `ShizukuBinderWrapper`。UserService 由 Shizuku 在独立进程中以 shell/root 身份实例化我们 APK 里的 `OpsxService`，app 通过 AIDL 调用它。这样做的原因是：

- 原有的服务端逻辑（`AppOpsHandler`、`ReflectUtils`、`FixCompat`、iptables）本来就是设计在特权进程里跑的，可以原样搬进去，阶段 4 再修内部实现；
- 服务进程不受隐藏 API 限制；如果改用 `ShizukuBinderWrapper` 在 app 进程里调用，`OpEntry` 私有字段反射等会被隐藏 API 策略拦截；
- iptables 联网控制需要 uid 0，只有 Shizuku 以 root 运行时 `OpsxService` 才是 root，行为和以前一致。

**结构**

| 位置 | 作用 |
|---|---|
| `opsxpro/src/main/aidl/.../IOpsxService.aidl` | 接口：get/set/reset ops、`getInstalledPackages`、`getUsers`；`destroy()` 使用 Shizuku 约定的事务号 16777114 |
| `opsxpro/.../remote/OpsxService.java` | 服务实现，运行在 Shizuku 拉起的进程中 |
| `opsxpro/.../remote/AppOpsHandler.java` | 原有 AppOps 逻辑，改成每个操作一个方法，出错时把异常放进 `OpsResult` 返回 |
| `opsxpro/.../OpsxManager.java` | 客户端：等待 Shizuku binder → 检查版本和授权 → `bindUserService`，对 app 暴露的方法名保持不变 |
| `opsxpro/.../ServiceUnavailableException.java` | 连接失败的原因：Shizuku 未运行、版本过低、未授权、启动超时 |
| `opsxlib/.../common/ParcelableListSlice.java` | 分批传输大列表，避免超过 binder 事务大小上限（思路同 AOSP 的 `ParceledListSlice`，对 app 不可用） |
| `app/.../ui/core/ShizukuHelper.java` | UI 侧的状态文字、授权请求、错误提示 |

**行为变化**

- 主界面收到 Shizuku binder 后自动申请授权；授权通过后重新加载用户列表
- 设置页去掉 ADB 模式、ADB 端口、root over adb、shell 启动命令四项，换成一个 Shizuku 状态项：未运行时点击打开 Shizuku 主页，未授权时点击申请授权
- “允许后台运行”对应 UserService 的 daemon 模式；“关闭后台服务”会结束服务进程
- UserService 的 version 取 APK 的安装时间，每次安装新包后 Shizuku 都会重启服务，不会继续跑旧代码
- “显示日志”改为显示诊断信息（系统版本、机型、commit、Shizuku 状态）。旧服务端写的日志文件已不存在
- 删除了 `OpsxConfigProvider`（无权限保护的导出 provider）和 `ServerStatusChangeReceiver`（任何应用都能发假广播）；现在唯一导出的 provider 是 Shizuku 的，受 `INTERACT_ACROSS_USERS_FULL` 保护
- 字符串：新增 Shizuku 相关文案（英文、简体、繁体），删除 15 个只给旧 ADB/root 界面用的字符串；cs/es/ru 的“后台运行”说明删掉了旧译文，暂时显示英文

**隐藏 API 桩**：`IPackageManager.getInstalledPackages` 改为 Android 13 的 `(long, int)`，`IUserManager.getUsers` 改为 `(boolean, boolean, boolean)`，`UserInfo` 补上 `Parcelable`。

**遗留问题（阶段 4）**

`AppOpsHandler` 和 `Helper` 内部调用的 `IPackageManager.getPackageUid`、`getApplicationInfo`、`getPackageInfo`，在 Android 13 上 flags 参数都已改成 `long`，桩里还是 `int`，运行时会报 `NoSuchMethodError`。其中 `getPackageUid` 和 `getApplicationInfo` 用来取应用的 uid，**这两个失败后 uid 为 -1，读取和修改权限都会失败**，所以阶段 3 做完后核心功能仍然用不了，要等阶段 4。

**文档**：`docs/help.md` 和 app 内的 `res/raw/help.html` 还在介绍 ADB/root 启动方式，需要改写成 Shizuku 的使用说明；README 也要同步更新。

### 阶段 4：AppOps 版本适配与测试

- `ReflectUtils.opsConvert`：不再反射读取 `OpEntry` 的私有字段，改用公开 getter 和 `AttributedOpEntry`
- `FixCompat`：不再反射读取 `sOpNames`、`sOpToSwitch`、`sOpPerms`，改用 `opToName()`、`opToSwitch()`、`opToPermission()` 和 `_NUM_OP`
- 去掉写死的 op 名称表，改为运行时从系统读取（每个大版本都会新增 op）
- 对 `IAppOpsService` 的调用集中到一个适配层，按 Android 13 到 16 逐个核对方法签名
- 修正 `IPackageManager` 桩中 flags 仍为 `int` 的方法（`getPackageUid`、`getApplicationInfo`、`getPackageInfo`），见阶段 3 的遗留问题
- 在 Android 13、14、15、16 上分别测试

### 可选：现代化

Kotlin、Material 3、用协程替换 RxJava 等，等功能稳定后再考虑。

## 4. 进度

| 阶段 | 状态 |
|---|---|
| 1 修复构建 | 已完成（构建通过，待设备验证） |
| 2 SDK 适配 | 未开始（部分条目已随阶段 1、3 完成） |
| 3 接入 Shizuku | 已完成（构建通过，待设备验证；核心功能依赖阶段 4） |
| 4 AppOps 适配与测试 | 未开始 |
