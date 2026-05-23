package me.eyetealer.wortel.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Wordle palette — soft greens/yellows on neutral background.
private val Correct = Color(0xFF538D4E)
private val Present = Color(0xFFB59F3B)
private val Absent = Color(0xFF3A3A3C)

private val LightColors = lightColorScheme(
    primary = Correct,
    onPrimary = Color.White,
    secondary = Present,
    onSecondary = Color.White,
    background = Color(0xFFFAFAFA),
    surface = Color(0xFFFFFFFF),
)

private val DarkColors = darkColorScheme(
    primary = Correct,
    onPrimary = Color.White,
    secondary = Present,
    onSecondary = Color.White,
    background = Color(0xFF121213),
    surface = Color(0xFF1E1E1F),
)

object WortelColors {
    val correct = Correct
    val present = Present
    val absent = Absent
}

@Composable
fun WortelTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content,
    )
}
