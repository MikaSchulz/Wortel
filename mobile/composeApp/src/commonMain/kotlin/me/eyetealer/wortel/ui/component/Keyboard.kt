package me.eyetealer.wortel.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.eyetealer.wortel.domain.GuessResult
import me.eyetealer.wortel.domain.LetterResult
import me.eyetealer.wortel.ui.theme.LocalWortelPalette

private val ROW_1 = "qwertzuiopü".toList()
private val ROW_2 = "asdfghjklöä".toList()
private val ROW_3 = "yxcvbnmß".toList()

@Composable
fun Keyboard(
    onLetter: (Char) -> Unit,
    onBackspace: () -> Unit,
    onEnter: () -> Unit,
    attempts: List<GuessResult>,
    modifier: Modifier = Modifier,
) {
    val letterStates = remember(attempts) { computeLetterStates(attempts) }

    Column(
        modifier = modifier.padding(horizontal = 4.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        KeyRow(ROW_1, letterStates, onLetter)
        KeyRow(ROW_2, letterStates, onLetter)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            ActionKey(onEnter, width = 64.dp) {
                Text(
                    text = "ENTER",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            ROW_3.forEach { ch -> LetterKey(ch, letterStates[ch], onLetter) }
            ActionKey(onBackspace, width = 48.dp) {
                WortelIcons.Backspace(size = 22.dp)
            }
        }
    }
}

@Composable
private fun KeyRow(
    chars: List<Char>,
    states: Map<Char, LetterResult>,
    onLetter: (Char) -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        chars.forEach { ch -> LetterKey(ch, states[ch], onLetter) }
    }
}

@Composable
private fun LetterKey(
    char: Char,
    result: LetterResult?,
    onLetter: (Char) -> Unit,
) {
    val palette = LocalWortelPalette.current
    val background = when (result) {
        LetterResult.CORRECT -> palette.correct
        LetterResult.PRESENT -> palette.present
        LetterResult.ABSENT -> palette.absent
        null -> MaterialTheme.colorScheme.surface
    }
    val textColor = if (result == null) MaterialTheme.colorScheme.onSurface else Color.White
    Box(
        modifier = Modifier
            .width(30.dp)
            .height(48.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(background)
            .clickable { onLetter(char) },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = char.uppercaseChar().toString(),
            color = textColor,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun ActionKey(
    onClick: () -> Unit,
    width: Dp,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = Modifier
            .width(width)
            .height(48.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

/**
 * Pure helper — kept outside any @Composable so it stays testable.
 * Preserves the strongest state seen so far: CORRECT > PRESENT > ABSENT.
 */
private fun computeLetterStates(attempts: List<GuessResult>): Map<Char, LetterResult> {
    val map = mutableMapOf<Char, LetterResult>()
    for (a in attempts) {
        a.guess.forEachIndexed { idx, ch ->
            val r = a.result.getOrNull(idx) ?: return@forEachIndexed
            val existing = map[ch]
            map[ch] = when {
                existing == LetterResult.CORRECT -> existing
                existing == LetterResult.PRESENT && r == LetterResult.ABSENT -> existing
                else -> r
            }
        }
    }
    return map
}
