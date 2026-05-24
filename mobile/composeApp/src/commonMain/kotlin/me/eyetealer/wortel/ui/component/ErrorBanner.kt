package me.eyetealer.wortel.ui.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Inline error banner above the board. Stays visible until the user types
 * the next letter / backspace — set `error = null` in the ViewModel on input.
 *
 * Material Snackbars time out too quickly (4s) and slide off-screen, which
 * was the original UX complaint. A persistent banner is easier to read and
 * never gets missed.
 */
@Composable
fun ErrorBanner(
    message: String?,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = !message.isNullOrEmpty(),
        enter = slideInVertically { -it } + fadeIn(),
        exit = slideOutVertically { -it } + fadeOut(),
        modifier = modifier,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.errorContainer)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = message.orEmpty(),
                color = MaterialTheme.colorScheme.onErrorContainer,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
            )
        }
    }
}

/**
 * Compatible color when a theme doesn't define errorContainer cleanly —
 * fallback used internally if needed in tests / preview.
 */
@Suppress("unused")
private val FallbackBackground = Color(0xFFB3261E)
