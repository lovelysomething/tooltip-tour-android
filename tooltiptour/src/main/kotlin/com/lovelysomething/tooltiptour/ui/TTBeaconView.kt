package com.lovelysomething.tooltiptour.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

enum class TTBeaconStyle { NUMBERED, DOT, RING }

/**
 * Step beacon — matches web embed.js and iOS TTBeaconView exactly.
 * Renders a numbered circle / dot / ring with a sonar-pulse animation.
 */
@Composable
fun TTBeaconView(
    stepNumber: Int,
    style: TTBeaconStyle = TTBeaconStyle.NUMBERED,
    color: Color = Color(0xFF3730A3.toInt()),
    labelColor: Color = Color.White,
    isActive: Boolean = true,
    onClick: (() -> Unit)? = null,
) {
    val sizeDp: Dp = when (style) {
        TTBeaconStyle.DOT      -> 12.dp
        TTBeaconStyle.RING     -> 20.dp
        TTBeaconStyle.NUMBERED -> 32.dp
    }

    // Sonar pulse — scale 1.0 → 1.7, opacity 0.6 → 0, 1.8s ease-out, infinite
    val infiniteTransition = rememberInfiniteTransition(label = "beacon-pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue  = 1.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "pulse-scale",
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue  = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "pulse-alpha",
    )

    val contentAlpha = if (isActive) 1f else 0.5f
    val density = LocalDensity.current

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(sizeDp)
            .then(
                if (onClick != null) Modifier.clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onClick,
                ) else Modifier
            ),
    ) {
        Canvas(modifier = Modifier.size(sizeDp)) {
            val cx = size.width / 2f
            val cy = size.height / 2f
            val r  = size.width / 2f

            // Pulse ring (drawn first, behind the circle)
            if (isActive) {
                scale(scale = pulseScale, pivot = Offset(cx, cy)) {
                    drawCircle(
                        color  = color.copy(alpha = pulseAlpha),
                        radius = r,
                        center = Offset(cx, cy),
                        style  = Stroke(width = 2.dp.toPx()),
                    )
                }
            }

            // Main circle
            when (style) {
                TTBeaconStyle.NUMBERED, TTBeaconStyle.DOT ->
                    drawCircle(
                        color  = color.copy(alpha = contentAlpha),
                        radius = r,
                        center = Offset(cx, cy),
                    )
                TTBeaconStyle.RING ->
                    drawCircle(
                        color  = color.copy(alpha = contentAlpha),
                        radius = r - 1.dp.toPx(),
                        center = Offset(cx, cy),
                        style  = Stroke(width = 2.dp.toPx()),
                    )
            }

            // Number label — draw as text via drawContext is complex; handled below via Composable
        }

        // Step number overlay for NUMBERED style
        if (style == TTBeaconStyle.NUMBERED) {
            androidx.compose.material3.Text(
                text  = "$stepNumber",
                color = labelColor.copy(alpha = contentAlpha),
                style = androidx.compose.material3.MaterialTheme.typography.labelSmall.copy(
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                    fontSize   = 13.dp.value.let { androidx.compose.ui.unit.TextUnit(it * 0.65f, androidx.compose.ui.unit.TextUnitType.Sp) },
                ),
            )
        }
    }
}
