package me.eyetealer.wortel.ui.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import me.eyetealer.wortel.domain.LetterResult
import me.eyetealer.wortel.ui.theme.WortelColors

private const val FLIP_HALF_MS = 250
private const val FLIP_PEAK_ANGLE = 90f

@Composable
fun Tile(
    letter: Char?,
    result: LetterResult?,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    onClick: (() -> Unit)? = null,
    /**
     * When non-null, the tile waits this many ms then plays a 3D-flip
     * reveal: rotates around its X axis to 90°, swaps the displayed
     * colour at the midpoint (face hidden edge-on), then unrotates.
     * Null = render the result immediately, no animation. Used by Board
     * to stagger the reveal of a freshly submitted row left-to-right.
     */
    revealDelayMs: Int? = null,
) {
    val rotation = remember { Animatable(0f) }
    // Track the colour we actually paint right now. During the first half
    // of the flip we keep the previous (often empty) colour so the swap
    // looks like the back face appearing; flipping back finishes with the
    // new colour. When revealDelayMs is null we just mirror `result`.
    var displayedResult by remember { mutableStateOf<LetterResult?>(null) }

    LaunchedEffect(result, revealDelayMs) {
        if (result == null) {
            displayedResult = null
            rotation.snapTo(0f)
            return@LaunchedEffect
        }
        if (revealDelayMs == null) {
            displayedResult = result
            rotation.snapTo(0f)
            return@LaunchedEffect
        }
        // Animated reveal.
        displayedResult = null
        rotation.snapTo(0f)
        delay(revealDelayMs.toLong())
        rotation.animateTo(FLIP_PEAK_ANGLE, animationSpec = tween(FLIP_HALF_MS))
        displayedResult = result
        rotation.animateTo(0f, animationSpec = tween(FLIP_HALF_MS))
    }

    val background = when (displayedResult) {
        LetterResult.CORRECT -> WortelColors.correct
        LetterResult.PRESENT -> WortelColors.present
        LetterResult.ABSENT -> WortelColors.absent
        null -> Color.Transparent
    }
    val textColor = if (displayedResult == null) {
        MaterialTheme.colorScheme.onBackground
    } else {
        Color.White
    }

    val borderWidth = when {
        displayedResult != null -> 0.dp
        selected -> 3.dp
        else -> 2.dp
    }
    val borderColor = when {
        displayedResult != null -> Color.Transparent
        selected -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onBackground.copy(alpha = 0.2f)
    }

    val clickModifier = if (onClick != null) Modifier.clickable { onClick() } else Modifier

    Box(
        modifier = modifier
            .size(56.dp)
            .graphicsLayer {
                rotationX = rotation.value
                // Distance large enough that the perspective at 90° still
                // looks like a card edge, not a violent zoom.
                cameraDistance = 12f * density
            }
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
