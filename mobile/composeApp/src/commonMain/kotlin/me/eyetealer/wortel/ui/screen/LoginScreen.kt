package me.eyetealer.wortel.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun LoginScreen(
    onGoogleSignIn: () -> Unit,
    onGuestSignIn: () -> Unit,
    error: String? = null,
) {
    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        ) {
            Text(
                "Wortel",
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                fontSize = 56.sp,
            )
            Text(
                "Errate das deutsche Wort.",
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 16.sp,
                textAlign = TextAlign.Center,
            )

            // Spacer
            Box(Modifier.height(32.dp))

            // Primary: Google sign-in.
            Button(
                onClick = onGoogleSignIn,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = Color(0xFF1F1F1F),
                ),
                shape = RoundedCornerShape(8.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    GoogleGlyph()
                    Text(
                        "Mit Google anmelden",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }

            // Secondary: guest play. OutlinedButton de-emphasises it so the
            // primary path is "sign up so your progress survives".
            OutlinedButton(
                onClick = onGuestSignIn,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(8.dp),
            ) {
                Text(
                    "Als Gast spielen",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }

            if (error != null) {
                Text(
                    error,
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                )
            }

            Text(
                "Hinweis: Als Gast verlierst du Fortschritt, wenn du den " +
                    "Browser-Cache löschst oder das Gerät wechselst.",
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
            )
        }
    }
}

/**
 * Minimal Google "G" mark — coloured arcs around a transparent center.
 * Avoids shipping the official multicoloured logo (which has trademark
 * usage rules) while still being recognisable next to the brand name.
 */
@Composable
private fun GoogleGlyph(size: androidx.compose.ui.unit.Dp = 22.dp) {
    androidx.compose.foundation.Canvas(Modifier.size(size)) {
        val w = this.size.width
        val stroke = w * 0.18f
        val inset = stroke / 2f
        val rect = androidx.compose.ui.geometry.Rect(
            offset = Offset(inset, inset),
            size = androidx.compose.ui.geometry.Size(w - 2 * inset, w - 2 * inset),
        )
        // Google brand approximation — quarter-arcs in four colours
        val style = androidx.compose.ui.graphics.drawscope.Stroke(
            width = stroke,
            cap = androidx.compose.ui.graphics.StrokeCap.Round,
        )
        // Blue: 12 o'clock to 3 o'clock
        drawArc(Color(0xFF4285F4), -90f, 90f, false, rect.topLeft, rect.size, 1f, style)
        // Green: 3 o'clock to 6 o'clock
        drawArc(Color(0xFF34A853), 0f, 90f, false, rect.topLeft, rect.size, 1f, style)
        // Yellow: 6 to 9 o'clock
        drawArc(Color(0xFFFBBC05), 90f, 90f, false, rect.topLeft, rect.size, 1f, style)
        // Red: 9 to 12 o'clock
        drawArc(Color(0xFFEA4335), 180f, 90f, false, rect.topLeft, rect.size, 1f, style)
        // Horizontal bar — the diagnostic stroke that makes "G" out of an O
        drawLine(
            color = Color(0xFF4285F4),
            start = Offset(w / 2f, w / 2f),
            end = Offset(w - inset, w / 2f),
            strokeWidth = stroke,
            cap = androidx.compose.ui.graphics.StrokeCap.Round,
        )
    }
}

