package me.eyetealer.wortel.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
    selected: Boolean = false,
    onClick: (() -> Unit)? = null,
) {
    val background = when (result) {
        LetterResult.CORRECT -> WortelColors.correct
        LetterResult.PRESENT -> WortelColors.present
        LetterResult.ABSENT -> WortelColors.absent
        null -> Color.Transparent
    }
    val textColor = if (result == null) MaterialTheme.colorScheme.onBackground else Color.White

    // Border:
    //   - revealed tile: no border (background carries the info)
    //   - selected empty tile: thick primary-coloured border to mark the cursor
    //   - other empty tile: subtle outline so the grid is visible
    val borderWidth = when {
        result != null -> 0.dp
        selected -> 3.dp
        else -> 2.dp
    }
    val borderColor = when {
        result != null -> Color.Transparent
        selected -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onBackground.copy(alpha = 0.2f)
    }

    val clickModifier = if (onClick != null) Modifier.clickable { onClick() } else Modifier

    Box(
        modifier = modifier
            .size(56.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(background)
            .border(width = borderWidth, color = borderColor, shape = RoundedCornerShape(6.dp))
            .then(clickModifier),
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
