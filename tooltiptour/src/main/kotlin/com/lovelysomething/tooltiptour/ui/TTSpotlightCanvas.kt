package com.lovelysomething.tooltiptour.ui

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer

/**
 * Full-screen dimmed overlay with a rounded-rect cutout around the highlighted view.
 *
 * Mirrors iOS TTSpotlightView — same 0.55 dim, same 10 px inset, same 14 dp radius.
 * Uses [BlendMode.Clear] + [CompositingStrategy.Offscreen] for the punch-through effect.
 *
 * [highlightRect] should be in window-pixel coordinates (from TTViewRegistry / boundsInWindow).
 */
@Composable
fun TTSpotlightCanvas(
    highlightRect: Rect?,
    modifier: Modifier = Modifier,
) {
    Canvas(
        modifier = modifier
            .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
    ) {
        // Dim the whole screen
        drawRect(Color.Black.copy(alpha = 0.55f))

        // Cut out the target area
        if (highlightRect != null) {
            val inset = Rect(
                left   = highlightRect.left   - 10f,
                top    = highlightRect.top    - 10f,
                right  = highlightRect.right  + 10f,
                bottom = highlightRect.bottom + 10f,
            )
            val path = Path().apply {
                addRoundRect(RoundRect(inset, CornerRadius(14f)))
            }
            drawPath(
                path      = path,
                color     = Color.Transparent,
                blendMode = BlendMode.Clear,
            )
        }
    }
}
