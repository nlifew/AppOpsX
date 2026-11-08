## AppOpsX
[![Build](https://github.com/nlifew/AppOpsX/actions/workflows/build.yml/badge.svg)][ci]
[![Software License](https://img.shields.io/github/license/nlifew/AppOpsX.svg)][license]

一个使用 appops 的 GUI 权限管理器，能方便地控制权限，避免权限滥用。

本仓库是 [8enet/AppOpsX][upstream] 的维护分支。原项目自 2019 年起不再更新，这里已适配到当前的 Android 版本，并改为通过 [Shizuku][shizuku] 获取权限，不再使用原来自带的 ADB/root 服务端。

> 需要 **Android 13 及以上**，以及 **[Shizuku][shizuku]**（通过无线调试或 root 启动，或使用 Sui）。
> Android 12 及以下请使用原版 AppOpsX 1.2.5。

### 特点
* 快速应用搜索和排序
* 按权限分组列表
* 导出/恢复配置数据
* 新装应用的自动忽略权限模板
* 多用户支持
* WiFi/移动数据联网控制（需要 Shizuku 以 root 方式运行）

### 使用方法
1. 安装并启动 [Shizuku][shizuku]：没有 root 的设备通过无线调试启动，有 root 的设备可以直接以 root 启动。
2. 打开 AppOpsX，在 Shizuku 的授权弹窗中允许。
   如果没有弹窗，可以在 AppOpsX 设置 → Shizuku 中申请，或者在 Shizuku 应用里为 AppOpsX 授权。
3. 手机重启后需要重新启动 Shizuku（除非你的设备能让 Shizuku 开机自启）。

### 编译
需要 JDK 21（Android Studio 自带的 JBR 即可）和装有 platform 36 的 Android SDK。

    ./gradlew assembleDebug

release 包使用环境变量 `APPOPSX_KEYSTORE`、`APPOPSX_KEYSTORE_PASSWORD`、`APPOPSX_KEY_ALIAS`、`APPOPSX_KEY_PASSWORD` 指定的签名；没有设置时使用 debug 签名。

这个分支做了哪些改动、还有哪些待办，见 [docs/MIGRATION.md](docs/MIGRATION.md)。

### BUG 反馈与建议
请提交 [issues][issues]，并附上 Android 版本、机型，以及 Shizuku 是通过 ADB 还是 root 启动的（在 AppOpsX 设置 → 显示日志 中可以看到这些信息）。

### 贡献
欢迎提供代码、设计、文案、功能等任何内容，请通过 [pull request][pr] 提交。
翻译可以直接修改 `app/src/main/res/values-*/strings.xml` 后提交 pull request。

### License
[MIT License][license]。原作者为 [8enet][upstream]。

[upstream]: https://github.com/8enet/AppOpsX
[shizuku]: https://shizuku.rikka.app/
[pr]: https://github.com/nlifew/AppOpsX/pulls
[issues]: https://github.com/nlifew/AppOpsX/issues
[license]: https://github.com/nlifew/AppOpsX/blob/master/LICENSE
[ci]: https://github.com/nlifew/AppOpsX/actions/workflows/build.yml
