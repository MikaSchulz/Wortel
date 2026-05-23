package me.eyetealer.wortel

import androidx.compose.ui.window.ComposeUIViewController
import platform.UIKit.UIViewController

/**
 * iOS entry point. Imported by `iosApp/iosApp/iOSApp.swift` (Xcode project)
 * via the generated ComposeApp framework:
 *
 *     import ComposeApp
 *     ...
 *     ComposeAppKt.MainViewController()
 *
 * The Xcode project is intentionally not committed yet — bootstrap it later
 * via Android Studio's "New Compose Multiplatform Project" wizard or
 * `kotlin xcode-sync` once you have a Mac.
 */
@Suppress("FunctionName", "unused")
fun MainViewController(): UIViewController = ComposeUIViewController { App() }
