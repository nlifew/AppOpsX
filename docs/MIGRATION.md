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

- `registerReceiver` 带上 `RECEIVER_NOT_EXPORTED` 或 `RECEIVER_EXPORTED`（`AppGlobal.installReceiver`）
- 声明 `QUERY_ALL_PACKAGES`（包可见性）
- `AlertInstalledPremActivity` 目前对外导出，需要收紧
- edge-to-edge 处理 insets
- 删除 API 33 以下的版本分支
- ~~`OpsxConfigProvider` 没有权限保护~~、~~不再使用 `/sdcard/Android/data/...` 下的 jar 和脚本~~：阶段 3 已随旧服务端一起删除
- ~~升级或替换过时的 AndroidX 依赖~~：阶段 1 已完成

#### 完成情况（2026-11-08）

`assembleDebug` 和 `assembleRelease` 均已通过（尚未在设备上验证）。

- **广播注册**：`installReceiver` 改为带 `RECEIVER_NOT_EXPORTED` 注册。（更正：之前判断这里会在 Android 14+ 上崩溃，其实不会。targetSdk 34 起强制要求导出标志，但只监听系统广播的 receiver 不受这条限制，而 `PACKAGE_ADDED` 正是系统广播。现在显式加上，是为了让意图更明确）
- 删除只给 Android 8 以下用的静态 `AppInstalledReceiver` 声明和运行时启用它的分支
- **包可见性**：声明 `QUERY_ALL_PACKAGES`。如果以后发布到 Google Play，需要在 Play 后台说明用途（权限管理工具属于允许的类别）
- **组件导出**：`AlertInstalledPremActivity` 改为 `exported="false"`，它只由应用内部启动。现在只有启动页、`appops://` 深链接对应的 `AppPermissionActivity`，以及 Shizuku 的 provider 是导出的
- **edge-to-edge**：统一在 `BaseActivity` 处理。所有页面都由 `layout_appbar` 加内容区组成，所以只需要两步：
  - 调用 `EdgeToEdge.enable()`，让 Android 13、14 也和 15+ 的表现一致；
  - 在 `onContentChanged()` 里设置 insets：appBar 的顶部让出状态栏高度，背景色延伸到状态栏后面；内容区让出导航栏和刘海。`layout_appbar` 的高度因此改为 `wrap_content`。
  - 状态栏图标颜色交给 `SystemBarStyle.auto` 按日间/夜间模式切换，主题里的 `statusBarColor`、`windowLightStatusBar` 已删除
- **旧版本分支**：删除了 `AppGlobal`、`Helper`（快捷方式）、`LangHelper`、`LocalImageLoader`、`ExpandableItemIndicator`、`FLog` 和 `remote/Helper` 里的 SDK 版本判断，以及 `IPackageManager` 桩里的旧版 `getPackageUid(String, int)`。`FixCompat` 里的 3 处留给阶段 4 重写
- **CI**：同时构建 debug 和 release

**未解决：新装应用弹窗失效**（重新设计见“5. 新装应用弹窗”）

`AppInstalledReceiver` 收到新装应用的广播后直接 `startActivity` 弹出 `AlertInstalledPremActivity`，但 Android 10 起禁止应用在后台启动 Activity，所以只有 AppOpsX 正好在前台时才会弹出。另外这个 receiver 是动态注册的，AppOpsX 进程不在时也收不到广播。

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

#### 进度（2026-11-08）

在 Xiaomi 13（MIUI 14 / Android 13，Shizuku root 模式）上发现两个问题，已修复，等待真机验证：

1. **“应用权限”页面报 `DeadObjectException`**：`getPackagesForOps` 的返回结果约 1.34 MB，超过 binder 单次传输上限（约 1 MB）。服务进程其实没有崩溃，报错里“remote process probably died”是系统的误判。修复：`OpsResult` 内部的列表改用 `ParcelableListSlice` 分批传输，所有返回 `OpsResult` 的接口都一并受益。
2. **每个应用只显示两个联网权限**：日志里 iptables 命令是 `--uid-owner -1`，说明应用的 uid 没取到。原因就是阶段 3 记录的 `int`/`long` flags 问题。修复：
   - `IPackageManager` 桩只保留实际用到的 4 个方法，flags 全部改成 `long`；
   - `ReflectUtils.opsConvert` 改为调用 `PackageOps` 和 `OpEntry` 的 `@SystemApi` getter（`getLastAccessTime(OP_FLAGS_ALL)` 等），不再读私有字段。旧代码在找不到 `getTime()` 时会把 `null` 拆箱成 `long` 并抛 NPE；字段缓存只按字段名做 key，也不记录找不到的字段，每个应用的每条记录都要重新抛一次异常；
   - 去掉 LineageOS 的 `mAllowedCount` 分支：Lineage 在 Android 13 上已经没有这个字段。

**已确认、暂不处理**

- `FixCompat` 在 API 28 及以上使用从 Android 9 抄来的写死的表。op 编号只会往后追加，所以这张表覆盖到的部分仍然正确，只是缺少 Android 9 之后新增的 op。改成运行时读取仍按原计划做。
- “应用权限”页面加载要 8 秒以上。除了上面的反射异常，主要是每个应用都要执行两次 iptables 命令来查询联网状态。可以改成一次 `iptables-save` 后统一解析。

### 可选：现代化

Kotlin、Material 3、用协程替换 RxJava 等，等功能稳定后再考虑。

## 4. 进度

| 阶段 | 状态 |
|---|---|
| 1 修复构建 | 已完成（构建通过，待设备验证） |
| 2 SDK 适配 | 已完成（构建通过，待设备验证；新装应用弹窗见第 5 节） |
| 3 接入 Shizuku | 已完成（Xiaomi 13 / Android 13 上已能通过 Shizuku root 模式拉起服务；核心功能依赖阶段 4） |
| 4 AppOps 适配与测试 | 进行中 |
| 新装应用弹窗 | 方案已定，阶段 4 之后实现 |

## 5. 新装应用弹窗（方案已定，阶段 4 之后实现）

**目标**：恢复原版行为。装好新应用后自动弹出权限确认框，让用户按模板关闭它的权限。

**原版为什么在新系统上失效**：见阶段 2 的“未解决”。原版的 receiver 是 AppOpsX 进程动态注册的，进程不在就收不到广播；收到了也不能从后台启动 Activity。

**方案**：交给 Shizuku 拉起的 `OpsxService` 来做。

1. **监听安装**：Shizuku API 13 支持 UserService 的构造函数接收 `Context` 参数。`OpsxService` 拿到 `Context` 后用 `registerReceiver` 监听 `PACKAGE_ADDED`，忽略 `EXTRA_REPLACING`（应用更新）。全部是公开 API，不会多出要按版本适配的隐藏 API。
2. **拉起弹窗**：由 `OpsxService` 调用 `startActivity` 启动 `AlertInstalledPremActivity`。
   - 后台启动 Activity 的限制针对的是普通应用。shell 身份持有 `START_ACTIVITIES_FROM_BACKGROUND`（`adb shell am start` 能随时拉起界面就是这个原因），root 身份不受限。Android 14 起这部分规则又收紧过，**需要在真机上验证**。
   - shell 身份启动不了别人应用里未导出的 Activity，所以 `AlertInstalledPremActivity` 要重新导出，同时加上 `android:permission="android.permission.INTERACT_ACROSS_USERS_FULL"`。普通第三方应用拿不到这个权限，shell 和 root 都有，这和 Shizuku 自己的 provider 用的是同一个做法。
3. **服务需要常驻**：依赖“允许后台运行”（daemon 模式）。daemon 模式的 UserService 一旦拉起，就独立于 AppOpsX 的进程存活。
4. **开机**：Shizuku 不会主动拉起任何 UserService。可以在 AppOpsX 里注册 `BOOT_COMPLETED`，收到后用 `addBinderReceivedListenerSticky` 等 Shizuku 就绪，再调用 `bindUserService`。前提是 Shizuku 自己开机时在运行。

**已知限制**

- 只在 Shizuku 运行、并且服务已被拉起时有效。
- Shizuku 的开机自启并不可靠：在维护者的 MIUI 14 设备上会稳定失败。如果 Shizuku 没有启动，服务也拉不起来，这期间装的应用不会弹窗，要等用户手动启动 Shizuku 并打开 AppOpsX 之后才恢复。
- 用 Magisk 或 KernelSU 模块保证开机可用可以解决这个问题，但对这个功能来说代价太大，不考虑。

**不做的方案**

- “下次打开 AppOpsX 时列出新装应用”：交互不自然，不是理想的方案，暂不实现。
- 改成发通知：仍然依赖 AppOpsX 进程存活，还要申请通知权限，不解决根本问题。
