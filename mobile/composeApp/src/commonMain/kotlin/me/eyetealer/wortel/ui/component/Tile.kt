package me.eyetealer.wortel.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.eyetealer.wortel.domain.LetterResult
import me.eyetealer.wortel.ui.theme.WortelColors

@Composable
fun Tile(
    letter: Char?,
    result: LetterResult?,
    modifier: Modifier = Modifier,
) {
    val background = when (result) {
        LetterResult.CORRECT -> WortelColors.correct
        LetterResult.PRESENT -> WortelColors.present
        LetterResult.ABSENT -> WortelColors.absent
        null -> Color.Transparent
    }
    val textColor = if (result == null) MaterialTheme.colorScheme.onBackground else Color.White

    Box(
        modifier = modifier
            .size(56.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(background)
            .border(
                width = if (result == null) 2.dp else 0.dp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.2f),
                shape = RoundedCornerShape(6.dp),
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (letter != null) {
            Text(
                text = letter.uppercaseChar().toString(),
                color = textColor,
                fontWeight = FontWeight.Bold,
                fontSize = 28.sp,
            )
        }
    }
}
