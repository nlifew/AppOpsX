## AppOpsX
[![Build](https://github.com/nlifew/AppOpsX/actions/workflows/build.yml/badge.svg)][ci]
[![Software License](https://img.shields.io/github/license/nlifew/AppOpsX.svg)][license]

AppOpsX is a front-end application for the Android AppOpsService. It allows you to restrict app permissions.

This is a maintained fork of [8enet/AppOpsX][upstream], which has not been updated since 2019. It has been updated for current Android versions and now uses [Shizuku][shizuku] instead of its own ADB/root server.

> Requires **Android 13 or later** and **[Shizuku][shizuku]** (started via wireless debugging or root, or Sui).
> For Android 12 and earlier, use the original AppOpsX 1.2.5.

[简体中文](README.zh.md)

## Features
* Search applications
* Group apps by permissions
* Import/Export backup file
* Permission template for newly installed apps
* Multi-user support
* Block Wi-Fi / mobile data per app (requires Shizuku running as root)

## Getting started
1. Install [Shizuku][shizuku] and start it, either through wireless debugging (no root needed) or with root.
2. Open AppOpsX and allow it in the Shizuku permission dialog.
   If the dialog does not appear, go to AppOpsX Settings → Shizuku, or grant it in the Shizuku app.
3. Shizuku has to be started again after every reboot unless your device can start it automatically.

## Building
Requires JDK 21 (Android Studio's bundled JBR works) and an Android SDK with platform 36.

    ./gradlew assembleDebug

Release builds are signed with the keystore given in the `APPOPSX_KEYSTORE`, `APPOPSX_KEYSTORE_PASSWORD`,
`APPOPSX_KEY_ALIAS` and `APPOPSX_KEY_PASSWORD` environment variables, or with the debug key if they are not set.

See [docs/MIGRATION.md](docs/MIGRATION.md) (Chinese) for what changed in this fork and what is still pending.

## Reporting Bugs
Bug reports and feature requests can be made via the [issue tracker][issues].
Please include your Android version, device, and whether Shizuku runs via ADB or root
(AppOpsX Settings → Show Logs shows this information).

## Contributing
Please fork this repository and contribute back using [pull requests][pr].
Translations are welcome as pull requests to `app/src/main/res/values-*/strings.xml`.

## License
AppOpsX is released under the [MIT License][license]. Original work by [8enet][upstream].

[upstream]: https://github.com/8enet/AppOpsX
[shizuku]: https://shizuku.rikka.app/
[pr]: https://github.com/nlifew/AppOpsX/pulls
[issues]: https://github.com/nlifew/AppOpsX/issues
[license]: https://github.com/nlifew/AppOpsX/blob/master/LICENSE
[ci]: https://github.com/nlifew/AppOpsX/actions/workflows/build.yml
