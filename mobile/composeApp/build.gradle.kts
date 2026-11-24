import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// Shared module: domain, data, view models and the whole Compose UI for every
// platform. It is a Kotlin Multiplatform *library* — the Android application
// lives in :androidApp (AGP 9 no longer lets com.android.application share a
// module with the KMP plugin), iOS consumes the ComposeApp framework, and the
// web app is the wasmJs executable built from this module.
plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
}

kotlin {
    android {
        namespace = "me.eyetealer.wortel.shared"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }

    // Pure JVM target — runs commonMain tests on machines without an
    // Android SDK or emulator (CI, SSH dev box). Not shipped.
    jvm("desktop") {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }

    // Apple Silicon only: Compose Multiplatform 1.12 dropped the x86_64
    // simulator target (iosX64) that Kotlin has deprecated.
    listOf(
        iosArm64(),
        iosSimulatorArm64(),
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
        }
    }

    // Web target — renders the same commonMain UI in the browser via
    // WebAssembly + Skia. Produces static files for GitHub Pages.
    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        outputModuleName = "composeApp"
        browser {
            commonWebpackConfig {
                outputFileName = "composeApp.js"
            }
        }
        binaries.executable()
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.ui)
            implementation(libs.compose.components.resources)
            implementation(libs.androidx.lifecycle.viewmodel.compose)
            implementation(libs.androidx.lifecycle.runtime.compose)

            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)

            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.client.serialization.json)

            implementation(project.dependencies.platform(libs.supabase.bom))
            implementation(libs.supabase.auth)

            implementation(libs.multiplatform.settings.noarg)
        }

        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
        }

        androidMain.dependencies {
            implementation(libs.kotlinx.coroutines.android)
            implementation(libs.ktor.client.okhttp)
        }

        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
        }

        // The JS Ktor engine doubles as the wasmJs engine.
        wasmJsMain.dependencies {
            implementation(libs.ktor.client.js)
        }
    }
}
