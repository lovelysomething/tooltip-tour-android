package com.lovelysomething.tooltiptour.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.lovelysomething.tooltiptour.models.TTSplashCarousel

/**
 * Full-screen carousel shown before the normal tour welcome card.
 * Navigation via dot row, ← Back, and Next → / Done buttons.
 *
 * Callbacks:
 * - [onDone]    — last slide completed (Done tapped)
 * - [onDismiss] — ✕ dismiss button tapped (any slide)
 */
@Composable
fun TTSplashCarouselView(
    carousel: TTSplashCarousel,
    onDone: () -> Unit,
    onDismiss: () -> Unit,
) {
    val slides = carousel.slides
    if (slides.isEmpty()) { onDone(); return }

    val bgColor   = parseCarouselColor(carousel.bgColor,   Color(0xFF1a1a2e.toInt()))
    val textColor = parseCarouselColor(carousel.textColor, Color.White)
    val pageCount = slides.size
    var currentPage by remember { mutableStateOf(0) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor),
    ) {
        // ── Current slide ─────────────────────────────────────────────────────
        val slide = slides[currentPage]
        SlideContent(
            logoUrl     = slide.logoUrl,
            imageUrl    = slide.imageUrl,
            title       = slide.title,
            description = slide.description,
            textColor   = textColor,
        )

        // ── Dismiss button ────────────────────────────────────────────────────
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 52.dp, end = 20.dp)
                .size(32.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.2f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss,
                ),
        ) {
            Text("✕", color = textColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }

        // ── Bottom nav row ────────────────────────────────────────────────────
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 24.dp, vertical = 40.dp),
        ) {
            // Dot row
            if (pageCount > 1) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 20.dp),
                ) {
                    repeat(pageCount) { i ->
                        val active = i == currentPage
                        Box(
                            modifier = Modifier
                                .size(if (active) 10.dp else 7.dp)
                                .clip(CircleShape)
                                .background(if (active) textColor else textColor.copy(alpha = 0.35f))
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                ) { currentPage = i },
                        )
                    }
                }
            } else {
                Spacer(Modifier.height(30.dp))
            }

            // Prev + Next/Done row
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (currentPage > 0) {
                    Text(
                        text = "← Back",
                        color = textColor.copy(alpha = 0.65f),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) { currentPage-- },
                    )
                } else {
                    Spacer(Modifier.width(64.dp))
                }

                val isLast = currentPage == pageCount - 1
                Button(
                    onClick = { if (isLast) onDone() else currentPage++ },
                    colors  = ButtonDefaults.buttonColors(
                        containerColor = textColor,
                        contentColor   = bgColor,
                    ),
                    modifier = Modifier.widthIn(min = 100.dp),
                ) {
                    Text(
                        text       = if (isLast) "Done" else "Next →",
                        fontWeight = FontWeight.Bold,
                        fontSize   = 14.sp,
                    )
                }
            }
        }
    }
}

// ── Single slide content ───────────────────────────────────────────────────────

@Composable
private fun SlideContent(
    logoUrl: String?,
    imageUrl: String?,
    title: String?,
    description: String?,
    textColor: Color,
) {
    // Logo width = min(50% of screen width, 400dp); height = width / 2 (2:1 ratio)
    val screenWidthDp = LocalConfiguration.current.screenWidthDp.toFloat()
    val logoWidthDp   = minOf(screenWidthDp * 0.5f, 400f).dp
    val logoHeightDp  = logoWidthDp / 2

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 28.dp)
            .padding(top = 100.dp, bottom = 160.dp),
    ) {
        if (!logoUrl.isNullOrBlank()) {
            AsyncImage(
                model              = logoUrl,
                contentDescription = "Logo",
                contentScale       = ContentScale.Fit,
                modifier           = Modifier.width(logoWidthDp).height(logoHeightDp),
            )
            Spacer(modifier = Modifier.height(20.dp))
        }

        if (!imageUrl.isNullOrBlank()) {
            AsyncImage(
                model              = imageUrl,
                contentDescription = "Slide image",
                contentScale       = ContentScale.Crop,
                modifier           = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(12.dp)),
            )
            Spacer(modifier = Modifier.height(24.dp))
        }

        if (!title.isNullOrBlank()) {
            Text(
                text       = title,
                fontSize   = 22.sp,
                fontWeight = FontWeight.Bold,
                color      = textColor,
                textAlign  = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        if (!description.isNullOrBlank()) {
            Text(
                text       = description,
                fontSize   = 15.sp,
                color      = textColor.copy(alpha = 0.8f),
                textAlign  = TextAlign.Center,
                lineHeight = 22.sp,
            )
        }
    }
}

/** Parses a CSS hex / rgb(a) string to a Compose Color; returns [fallback] on failure. */
private fun parseCarouselColor(hex: String?, fallback: Color): Color {
    if (hex.isNullOrBlank()) return fallback
    return try {
        Color(android.graphics.Color.parseColor(
            if (hex.trim().startsWith("#")) hex.trim() else "#${hex.trim()}"
        ))
    } catch (_: Exception) { fallback }
}
