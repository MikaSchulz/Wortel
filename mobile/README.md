# Wortel Mobile

Kotlin Multiplatform + Compose Multiplatform. Android, iOS and Web share **all** app
logic and UI.

## Stack

| | Version |
| --- | --- |
| Kotlin | 2.4.21 |
| Compose Multiplatform | 1.12.2 (Material 3 1.9.0) |
| Android Gradle Plugin | 9.5.1 — compileSdk 37, targetSdk 36, minSdk 26 |
| Gradle | 9.9.1 (JDK 17+, CI uses 21) |
| Ktor | 3.6.1 |
| supabase-kt | 3.9.0 (Auth only) |
| multiplatform-settings | 1.3.0 (persistent key-value storage) |

All versions live in `gradle/libs.versions.toml`.

## Modules

```
mobile/
├── composeApp/                      # KMP library: everything shared
│   └── src/
│       ├── commonMain/kotlin/me/eyetealer/wortel/
│       │   ├── App.kt               # root composable + screen routing
│       │   ├── domain/              # evaluate() (mirrors the backend), enums
│       │   ├── data/                # GameApi, AuthService, LocalStorage, DTOs
│       │   ├── viewmodel/           # GameViewModel — StateFlow<GameUiState>
│       │   └── ui/                  # theme, screens, components
│       ├── commonTest/              # domain + ViewModel tests (fakes, no network)
│       ├── androidMain/             # Android actuals (Platform)
│       ├── iosMain/                 # MainViewController for Xcode
│       ├── wasmJsMain/              # main.kt (ComposeViewport) + index.html
│       └── desktopMain/             # JVM target, used to run tests headless
└── androidApp/                      # Android application: MainActivity + manifest
```

Why two modules: since AGP 9 the Android *application* plugin can no longer be
applied to a Kotlin Multiplatform module. `composeApp` therefore uses
`com.android.kotlin.multiplatform.library`, and `androidApp` is a thin shell around it.

`GameViewModel` depends on interfaces (`GameApi`, `AuthService`,
`GameSessionStorage`, `SettingsStorage`), so tests run against in-memory fakes.

## Targets

| Target | Build | Output |
| --- | --- | --- |
| Android | `./gradlew :androidApp:assembleDebug` | `androidApp/build/outputs/apk/debug/androidApp-debug.apk` |
| iOS | Xcode project (Mac required, not committed yet) | `ComposeApp.framework` (arm64 device + Apple-silicon simulator) |
| Web | `./gradlew :composeApp:wasmJsBrowserDistribution` | `composeApp/build/dist/wasmJs/productionExecutable/` |
| Desktop (tests only) | `./gradlew :composeApp:desktopTest` | — |

Intel-Mac iOS simulators (`iosX64`) are no longer supported by Compose Multiplatform.

## Build from the CLI

```bash
cd mobile

./gradlew :composeApp:desktopTest                       # unit tests, no Android SDK needed at runtime
./gradlew :androidApp:lintDebug :androidApp:assembleDebug
./gradlew :androidApp:installDebug                      # on a connected device/emulator
./gradlew :composeApp:wasmJsBrowserDevelopmentRun       # web dev server
./gradlew :composeApp:compileKotlinIosSimulatorArm64    # type-check iOS code (works on Linux)
```

The Android SDK must be available (Android Studio sets `local.properties`, or set
`ANDROID_HOME`).

## Open in Android Studio

File → Open → `mobile/`, wait for Gradle sync, run the `androidApp` configuration.
Use a current Android Studio with the Kotlin Multiplatform plugin (AGP 9.5 support
required).

## Backend + auth

Pre-configured for the production Supabase project in
`composeApp/src/commonMain/kotlin/me/eyetealer/wortel/data/SupabaseConfig.kt`
(URL + publishable key — safe to ship, RLS protects the data).

Sign-in: **Google OAuth** or **guest** (Supabase anonymous sign-in). Guests keep
their user id until they sign out or clear app data. Game traffic goes to the
`games` Edge Function through `GameRepository`'s own Ktor client so structured error
codes reach the UI (`ErrorMessages` translates them).

## Persistence

`LocalStorage` (multiplatform-settings) keeps the active game id and the colour-blind
setting: SharedPreferences on Android, NSUserDefaults on iOS, `localStorage` on the
web. Keys are unchanged from earlier versions, so saved data carries over.

## iOS

`iosMain/MainViewController.kt` is the Compose entry point. To run on iOS, create
`iosApp/` with the Kotlin Multiplatform wizard (or Xcode) on a Mac, link the
`ComposeApp` framework and call `MainViewControllerKt.MainViewController()`.

## Next features

- Daily challenge in the client (backend endpoints exist)
- Stats screen (games played, wins, streak)
- App icon, haptics, share result (emoji grid)
