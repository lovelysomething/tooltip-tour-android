@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.lovelysomething.tooltiptour.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.lovelysomething.tooltiptour.models.TTSplashCarousel
import kotlinx.coroutines.launch

/**
 * Full-screen carousel shown before the normal tour welcome card.
 *
 * Supports horizontal (default) and vertical swipe via [TTSplashCarousel.direction].
 * Swipe is handled by Compose Pager; dot navigation row is also tappable.
 *
 * Callbacks:
 * - [onDone]    — last slide was completed (Next → Done)
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
    val isVertical = carousel.direction == "vertical"
    val scope = rememberCoroutineScope()
    val pageCount = slides.size
    val pagerState = rememberPagerState(pageCount = { pageCount })
    val currentPage by remember { derivedStateOf { pagerState.currentPage } }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor),
    ) {
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

        // ── Pager ────────────────────────────────────────────────────────────
        if (isVertical) {
            VerticalPager(
                state = pagerState,
                pageSize = PageSize.Fill,
                modifier = Modifier.fillMaxSize(),
            ) { page ->
                SlideContent(slides[page].logoUrl, slides[page].imageUrl,
                    slides[page].title, slides[page].description, textColor)
            }
        } else {
            HorizontalPager(
                state = pagerState,
                pageSize = PageSize.Fill,
                modifier = Modifier.fillMaxSize(),
            ) { page ->
                SlideContent(slides[page].logoUrl, slides[page].imageUrl,
                    slides[page].title, slides[page].description, textColor)
            }
        }

        // ── Bottom nav row ────────────────────────────────────────────────────
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 24.dp, vertical = 40.dp),
        ) {
            // Dot row
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
                            ) { scope.launch { pagerState.animateScrollToPage(i) } },
                    )
                }
            }

            // Prev + Next/Done row
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                // Prev — hidden on first slide
                if (currentPage > 0) {
                    Text(
                        text = "← Back",
                        color = textColor.copy(alpha = 0.65f),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                            ) { scope.launch { pagerState.animateScrollToPage(currentPage - 1) } },
                    )
                } else {
                    Spacer(Modifier.width(64.dp))
                }

                // Next / Done
                val isLast = currentPage == pageCount - 1
                Button(
                    onClick = {
                        if (isLast) onDone()
                        else scope.launch { pagerState.animateScrollToPage(currentPage + 1) }
                    },
                    colors = ButtonDefaults.buttonColors(
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

/** Single slide content — logo (top-centre), image (middle), title + description (below). */
@Composable
private fun SlideContent(
    logoUrl: String?,
    imageUrl: String?,
    title: String?,
    description: String?,
    textColor: Color,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 28.dp)
            .padding(top = 100.dp, bottom = 160.dp),
    ) {
        // Logo
        if (!logoUrl.isNullOrBlank()) {
            AsyncImage(
                model              = logoUrl,
                contentDescription = "Logo",
                contentScale       = ContentScale.Fit,
                modifier           = Modifier.size(80.dp),
            )
            Spacer(modifier = Modifier.height(20.dp))
        }

        // Slide image — 3:2 aspect ratio, rounded corners
        if (!imageUrl.isNullOrBlank()) {
            AsyncImage(
                model              = imageUrl,
                contentDescription = "Slide image",
                contentScale       = ContentScale.Crop,
                modifier           = Modifier
                    .fillMaxWidth()
                    .aspectRatio(3f / 2f)
                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(12.dp)),
            )
            Spacer(modifier = Modifier.height(24.dp))
        }

        // Title
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

        // Description
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
