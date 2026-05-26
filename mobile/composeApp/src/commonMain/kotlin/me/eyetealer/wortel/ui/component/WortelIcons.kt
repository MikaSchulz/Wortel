package me.eyetealer.wortel.ui.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Hand-drawn vector icons.
 *
 * Compose Multiplatform's wasmJs target uses a bundled Skia font that is
 * missing several Unicode symbol glyphs (e.g. `←` U+2190 renders empty,
 * `⌫` U+232B too). Rather than ship a Noto Symbols font (~200 KB) or pull
 * the material-icons-extended artifact (heavier still and version-fragile
 * across CMP releases), draw the few icons we need with Canvas primitives.
 *
 * Each icon respects `LocalContentColor` so it inherits the surrounding
 * Material 3 theme — Tile/Key text and icon colour stay consistent.
 */
object WortelIcons {

    @Composable
    fun ArrowLeft(size: Dp = 24.dp, modifier: Modifier = Modifier) {
        val color = LocalContentColor.current
        Canvas(modifier.size(size)) {
            val w = this.size.width
            val h = this.size.height
            val stroke = w * 0.10f
            val tipX = w * 0.22f
            val endX = w * 0.85f
            val midY = h / 2f
            val barb = w * 0.20f
            drawLine(color, Offset(tipX, midY), Offset(endX, midY), stroke, StrokeCap.Round)
            drawLine(color, Offset(tipX, midY), Offset(tipX + barb, midY - barb), stroke, StrokeCap.Round)
            drawLine(color, Offset(tipX, midY), Offset(tipX + barb, midY + barb), stroke, StrokeCap.Round)
        }
    }

    /**
     * Stylised eye glyph — two arcs forming an almond + a filled pupil.
     * The pupil colour follows LocalContentColor so it reads in both
     * normal and colorblind themes; pair with onClick to make a toggle.
     */
    @Composable
    fun Eye(size: Dp = 24.dp, modifier: Modifier = Modifier) {
        val color = LocalContentColor.current
        Canvas(modifier.size(size)) {
            val w = this.size.width
            val h = this.size.height
            val stroke = w * 0.09f
            val topLeftX = w * 0.10f
            val topLeftY = h * 0.25f
            val arcW = w * 0.80f
            val arcH = h * 0.50f
            // Top arc bulging up
            drawArc(
                color = color,
                startAngle = 180f,
                sweepAngle = 180f,
                useCenter = false,
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = stroke, cap = StrokeCap.Round),
                topLeft = androidx.compose.ui.geometry.Offset(topLeftX, topLeftY),
                size = androidx.compose.ui.geometry.Size(arcW, arcH),
            )
            // Bottom arc bulging down
            drawArc(
                color = color,
                startAngle = 0f,
                sweepAngle = 180f,
                useCenter = false,
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = stroke, cap = StrokeCap.Round),
                topLeft = androidx.compose.ui.geometry.Offset(topLeftX, topLeftY),
                size = androidx.compose.ui.geometry.Size(arcW, arcH),
            )
            // Pupil
            drawCircle(color, radius = w * 0.13f, center = androidx.compose.ui.geometry.Offset(w / 2, h / 2))
        }
    }

    /**
     * Lightbulb glyph for the hint button. A round bulb on top, two short
     * lines for the base (screw threads), and a tiny filament inside.
     * Stroked-only so it stays readable in both palettes via LocalContentColor.
     */
    @Composable
    fun Lightbulb(size: Dp = 24.dp, modifier: Modifier = Modifier) {
        val color = LocalContentColor.current
        Canvas(modifier.size(size)) {
            val w = this.size.width
            val h = this.size.height
            val stroke = w * 0.09f
            // Bulb (circle), upper ~60% of the icon
            val bulbR = w * 0.30f
            val bulbCx = w / 2f
            val bulbCy = h * 0.38f
            drawCircle(
                color = color,
                radius = bulbR,
                center = Offset(bulbCx, bulbCy),
                style = Stroke(width = stroke),
            )
            // Base — two horizontal lines below the bulb
            val baseW = w * 0.30f
            val baseLeft = w / 2f - baseW / 2f
            val baseRight = w / 2f + baseW / 2f
            val baseY1 = h * 0.74f
            val baseY2 = h * 0.86f
            drawLine(
                color = color,
                start = Offset(baseLeft, baseY1),
                end = Offset(baseRight, baseY1),
                strokeWidth = stroke,
                cap = StrokeCap.Round,
            )
            drawLine(
                color = color,
                start = Offset(baseLeft + w * 0.04f, baseY2),
                end = Offset(baseRight - w * 0.04f, baseY2),
                strokeWidth = stroke,
                cap = StrokeCap.Round,
            )
            // Tiny vertical filament
            drawLine(
                color = color,
                start = Offset(bulbCx, bulbCy - bulbR * 0.5f),
                end = Offset(bulbCx, bulbCy + bulbR * 0.4f),
                strokeWidth = stroke * 0.7f,
                cap = StrokeCap.Round,
            )
        }
    }

    @Composable
    fun Backspace(size: Dp = 24.dp, modifier: Modifier = Modifier) {
        val color = LocalContentColor.current
        Canvas(modifier.size(size)) {
            val w = this.size.width
            val h = this.size.height
            val stroke = w * 0.09f
            // Pentagon body: tip on the left, rectangle on the right.
            val tipX = w * 0.12f
            val midY = h / 2f
            val leftX = w * 0.40f
            val rightX = w * 0.90f
            val topY = h * 0.22f
            val botY = h * 0.78f
            val path = Path().apply {
                moveTo(tipX, midY)
                lineTo(leftX, topY)
                lineTo(rightX, topY)
                lineTo(rightX, botY)
                lineTo(leftX, botY)
                close()
            }
            drawPath(path, color, style = Stroke(width = stroke))
            // Small "X" inside to denote delete.
            val pad = w * 0.10f
            val xLeft = leftX + pad
            val xRight = rightX - pad
            val xTop = topY + pad
            val xBot = botY - pad
            val xStroke = stroke * 0.75f
            drawLine(color, Offset(xLeft, xTop), Offset(xRight, xBot), xStroke, StrokeCap.Round)
            drawLine(color, Offset(xRight, xTop), Offset(xLeft, xBot), xStroke, StrokeCap.Round)
        }
    }
}
