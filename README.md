# Farm Manager

Kotlin Multiplatform (KMP) farm management app for **Android** and **iOS**. Track agriculture expenses, harvest, sales, and profit/loss per crop—fully **offline** with a local Room database. UI is built with **Compose Multiplatform** and **Material Design 3**.

**Repository:** [github.com/velsans/farms](https://github.com/velsans/farms)

## Platforms

| Platform | Module | Status |
|----------|--------|--------|
| Android | `:composeApp` | Full feature set |
| iOS | `:composeApp` + `iosApp/` | UI + offline DB; file import/export stubs ready for native pickers |

## Features

- **Agri module** — dashboard, crops, expenses, harvest, sales, reports
- **Excel import/export** — merge import (no duplicate wipe); Apache POI on Android, Okio-based XLSX on iOS
- **Share** exports via system sheet (WhatsApp, Files, etc.)
- **Goat / Chicken** module placeholders for future expansion
- **Back navigation** — sub-screens return to Agri dashboard; exit confirmation on dashboard

## Architecture

```
composeApp/
├── commonMain/     Shared UI (Compose), ViewModel, Room, Koin, MVI
├── androidMain/    MainActivity, Android file pickers, POI Excel
└── iosMain/        MainViewController, iOS platform bridges, Okio Excel

iosApp/             Xcode shell linking ComposeApp.framework
```

| Layer | Technology |
|-------|------------|
| UI | Compose Multiplatform, Material 3 |
| State | MVI-style `FarmIntent` / `FarmViewModel` |
| DI | Koin (replaces Hilt for KMP) |
| Database | Room 2.7 + SQLite bundled driver |
| Excel | Apache POI (Android), Okio XLSX (iOS) |

## Requirements

- **Android Studio** Ladybug or newer (recommended)
- **JDK 17**
- **Xcode 15+** (for iOS)
- **compileSdk 35**, **minSdk 26**

## Build & run

### Android

```bash
./gradlew :composeApp:assembleDebug
```

Run **composeApp** from Android Studio, or install the debug APK.

If `java` is not on your PATH:

```bash
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
./gradlew :composeApp:assembleDebug
```

### iOS

1. Build the Kotlin framework:

```bash
./gradlew :composeApp:linkDebugFrameworkIosSimulatorArm64
```

2. Open `iosApp/iosApp.xcodeproj` in Xcode.
3. Set your **Team ID** in `iosApp/Configuration/Config.xcconfig` if needed.
4. Run on a simulator or device.

Gradle embeds the `ComposeApp` framework automatically via the Xcode build phase script.

## Project notes

- The legacy `app/` folder is kept for reference; **`:composeApp`** is the active application module.
- Shared strings live in `composeApp/src/commonMain/composeResources/values/strings.xml`.
- Platform file actions use `PlatformUiActions` (Android document pickers) and `PlatformFileHandler` (read/write/share bytes).

## License

No license file is bundled. Add one (e.g. MIT or Apache-2.0) if you plan to distribute the project.
