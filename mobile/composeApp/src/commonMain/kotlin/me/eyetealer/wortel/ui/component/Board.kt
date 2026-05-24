package me.eyetealer.wortel.ui.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import me.eyetealer.wortel.domain.GuessResult

private val SHAKE_KEYFRAMES = listOf(-14f, 14f, -12f, 12f, -8f, 8f, -4f, 4f, 0f)
private const val SHAKE_STEP_MS = 45

@Composable
fun Board(
    attempts: List<GuessResult>,
    currentGuessChars: List<Char?>,
    cursorIndex: Int,
    onTileClick: (Int) -> Unit,
    wordLength: Int,
    maxAttempts: Int,
    shakeTrigger: Int = 0,
    modifier: Modifier = Modifier,
) {
    // Animate translationX of the active row when shakeTrigger changes.
    // graphicsLayer is used instead of Modifier.offset so the animation
    // doesn't re-run layout / measurement — pure GPU transform.
    val shakeOffset = remember { Animatable(0f) }
    LaunchedEffect(shakeTrigger) {
        if (shakeTrigger > 0) {
            for (target in SHAKE_KEYFRAMES) {
                shakeOffset.animateTo(target, animationSpec = tween(SHAKE_STEP_MS))
            }
        }
    }

    Column(
        modifier = modifier.padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        repeat(maxAttempts) { rowIndex ->
            val attempt = attempts.getOrNull(rowIndex)
            val isCurrent = rowIndex == attempts.size && attempt == null
            val rowModifier = if (isCurrent) {
                Modifier.graphicsLayer { translationX = shakeOffset.value }
            } else {
                Modifier
            }
            Row(
                modifier = rowModifier,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                for (col in 0 until wordLength) {
                    when {
                        attempt != null -> Tile(
                            letter = attempt.guess.getOrNull(col),
                            result = attempt.result.getOrNull(col),
                        )
                        isCurrent -> Tile(
                            letter = currentGuessChars.getOrNull(col),
                            result = null,
                            selected = col == cursorIndex,
                            onClick = { onTileClick(col) },
                        )
                        else -> Tile(letter = null, result = null)
                    }
                }
            }
        }
    }
}
