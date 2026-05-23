package me.eyetealer.wortel.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import me.eyetealer.wortel.domain.GuessResult

@Composable
fun Board(
    attempts: List<GuessResult>,
    currentGuess: String,
    wordLength: Int,
    maxAttempts: Int,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        repeat(maxAttempts) { rowIndex ->
            val attempt = attempts.getOrNull(rowIndex)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                for (col in 0 until wordLength) {
                    when {
                        attempt != null -> Tile(
                            letter = attempt.guess.getOrNull(col),
                            result = attempt.result.getOrNull(col),
                        )
                        rowIndex == attempts.size -> Tile(
                            letter = currentGuess.getOrNull(col),
                            result = null,
                        )
                        else -> Tile(letter = null, result = null)
                    }
                }
            }
        }
    }
}
