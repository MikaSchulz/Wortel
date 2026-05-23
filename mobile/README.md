# Wortel Mobile

Kotlin Multiplatform + Compose Multiplatform app. Android + iOS share **all** UI code via Compose.

## Stack

- **Kotlin Multiplatform** 2.1.20
- **Compose Multiplatform** 1.8.0 (Material 3)
- **Ktor 3** HTTP client
- **Supabase Kotlin SDK** 3.x — auth + functions
- **AGP** 8.7.3, **min SDK** 26 (Android 8), **target SDK** 35

## Directory layout

```
mobile/
├── composeApp/
│   └── src/
│       ├── commonMain/kotlin/me/eyetealer/wortel/
│       │   ├── App.kt                       # root composable + nav
│       │   ├── domain/                      # ported from backend (Game, evaluate, …)
│       │   ├── data/                        # SupabaseClient, GameRepository, DTOs
│       │   ├── viewmodel/GameViewModel.kt   # StateFlow<GameUiState>
│       │   └── ui/                          # theme, screens, components
│       ├── androidMain/                     # MainActivity, AndroidManifest.xml
│       ├── iosMain/                         # MainViewController (Compose UIViewController)
│       └── commonTest/                      # shared unit tests
├── build.gradle.kts
├── settings.gradle.kts
├── gradle.properties
└── gradle/libs.versions.toml                # version catalog
```

## Prerequisites

| Tool | Version | Notes |
| --- | --- | --- |
| JDK | 17+ | KMP/CMP toolchain |
| Android Studio | Ladybug (2024.2) or newer | with KMP plugin enabled |
| Android SDK | API 35 + emulator | via Android Studio SDK Manager |
| Xcode (iOS only) | 16+ | macOS only — defer if you don't have a Mac |

## Open in Android Studio

1. Android Studio → File → Open → select `mobile/` directory
2. Wait for Gradle sync (downloads dependencies on first run)
3. Run config "composeApp" → green play button
4. Pick emulator (or attached device) → app installs + launches

## Build from CLI

```bash
cd mobile

# Android debug APK
./gradlew :composeApp:assembleDebug

# Install on connected device/emulator
./gradlew :composeApp:installDebug

# Unit tests (commonMain)
./gradlew :composeApp:testDebugUnitTest

# Lint
./gradlew lint
```

## Live backend

Pre-configured to hit the production Supabase project:

- URL: `https://lmmjbpgfnuxbcdvjtdkm.supabase.co`
- See `composeApp/src/commonMain/kotlin/me/eyetealer/wortel/data/SupabaseConfig.kt`

To point at a staging/branch URL: edit `SupabaseConfig.kt` or extract to BuildConfig later.

## Auth

MVP uses **anonymous sign-in**. On first launch the app calls
`auth.signInAnonymously()` — Supabase issues a JWT bound to a generated user.
That user persists for the install (token stored in Supabase SDK's auth storage).

Games created while signed in are bound to the user (RLS-enforced).
Reinstalling the app generates a new anonymous user → previous games are no
longer visible. Future work: upgrade anonymous to email/Apple/Google.

## iOS

`iosMain/MainViewController.kt` provides the Compose entry point. To run on iOS:

1. Use a Mac with Xcode 16+
2. Generate the Xcode project skeleton:
   - In Android Studio: File → New → Module → "Compose Multiplatform iOS module"
   - Or manually create `iosApp/iosApp.xcodeproj` referencing the ComposeApp framework
3. Set up code signing
4. Run from Xcode → app launches in iOS Simulator or device

For now, the iOS source compiles as part of `:composeApp` so APIs stay in sync, but no Xcode project is committed.

## Testing strategy

- **Domain tests** in `commonTest/` — pure Kotlin, no Android runtime, run on every build
- **ViewModel tests** with `kotlinx.coroutines.test` — fake repositories
- **Integration tests** against live Supabase — manual / future CI job

```bash
./gradlew :composeApp:check    # runs all tests + lint
```

## Common issues

| Symptom | Fix |
| --- | --- |
| "SDK location not found" | Create `mobile/local.properties` with `sdk.dir=/path/to/Android/Sdk` (Android Studio does this automatically on first open) |
| Gradle sync fails on Compose Compiler | Make sure `compose-compiler` plugin version matches Kotlin version in `libs.versions.toml` |
| Network errors / 401 on Supabase | Check `SupabaseConfig.PUBLISHABLE_KEY` matches your project's publishable key |
| `kotlinx.coroutines` mismatch | Aligned via version catalog — don't pin individual modules |

## Next features (post-MVP)

- Email/Apple/Google sign-in (upgrade anonymous user)
- Daily Wordle (server-driven)
- Stats screen (games played, wins, streak)
- Animations: tile flip on reveal, shake on invalid word
- Haptics (Android `Vibrator`, iOS `UIImpactFeedbackGenerator`)
- Share result (Wordle-style emoji grid)
- Settings: dark mode toggle, language toggle (DE/EN)
