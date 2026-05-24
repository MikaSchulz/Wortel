package me.eyetealer.wortel.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Holds the three Wordle tile colours. Provided via [LocalWortelPalette] so
 * Tile + Keyboard read it instead of hard-coding constants — letting the
 * colorblind toggle swap palettes at runtime.
 */
data class WortelPalette(
    val correct: Color,
    val present: Color,
    val absent: Color,
)

// Wordle defaults — soft green / mustard yellow / dark gray.
val DefaultPalette = WortelPalette(
    correct = Color(0xFF538D4E),
    present = Color(0xFFB59F3B),
    absent = Color(0xFF3A3A3C),
)

// Colorblind variant — orange / blue / dark gray. Matches the official
// Wordle "Farbenblind-Modus" mapping so users coming from there feel
// at home.
val ColorblindPalette = WortelPalette(
    correct = Color(0xFFF5793A),
    present = Color(0xFF85C0F9),
    absent = Color(0xFF3A3A3C),
)

val LocalWortelPalette = staticCompositionLocalOf { DefaultPalette }

@Deprecated("Read LocalWortelPalette.current instead", ReplaceWith("LocalWortelPalette.current"))
object WortelColors {
    val correct = DefaultPalette.correct
    val present = DefaultPalette.present
    val absent = DefaultPalette.absent
}

@Composable
fun WortelTheme(
    palette: WortelPalette = DefaultPalette,
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val light = lightColorScheme(
        primary = palette.correct,
        onPrimary = Color.White,
        secondary = palette.present,
        onSecondary = Color.White,
        background = Color(0xFFFAFAFA),
        surface = Color(0xFFFFFFFF),
    )
    val dark = darkColorScheme(
        primary = palette.correct,
        onPrimary = Color.White,
        secondary = palette.present,
        onSecondary = Color.White,
        background = Color(0xFF121213),
        surface = Color(0xFF1E1E1F),
    )

    CompositionLocalProvider(LocalWortelPalette provides palette) {
        MaterialTheme(
            colorScheme = if (darkTheme) dark else light,
            content = content,
        )
    }
}
