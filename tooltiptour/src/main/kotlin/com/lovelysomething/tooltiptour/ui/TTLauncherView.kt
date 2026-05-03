package com.lovelysomething.tooltiptour.ui

import android.content.Context
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lovelysomething.tooltiptour.TooltipTour
import com.lovelysomething.tooltiptour.models.TTConfig
import com.lovelysomething.tooltiptour.networking.TTEventType
import com.lovelysomething.tooltiptour.registry.TTPageRegistry

private enum class LauncherState { HIDDEN, LOADING, CAROUSEL, WELCOME, FAB }

/**
 * Launcher composable — add to the root Box of each screen that should show tours.
 *
 * ```kotlin
 * Box(modifier = Modifier.fillMaxSize().ttPage("home")) {
 *     YourContent()
 *     TTLauncherView()
 * }
 * ```
 *
 * Mirrors iOS TTLauncherView.swift exactly: auto-shows welcome card based on
 * show count + session-minimised state, persisted to SharedPreferences.
 */
@Composable
fun TTLauncherView(modifier: Modifier = Modifier) {
    val sdk = TooltipTour.instance ?: return

    val context        = LocalContext.current
    val prefs          = remember { context.getSharedPreferences("tooltiptour", Context.MODE_PRIVATE) }
    val currentPage    by TTPageRegistry.currentPageFlow.collectAsState()
    var config         by remember { mutableStateOf<TTConfig?>(null) }
    var launcherState  by remember { mutableStateOf(LauncherState.HIDDEN) }
    val scope          = rememberCoroutineScope()
    var carouselShownThisSession by remember { mutableStateOf(false) }

    // Observe inspector-active state so we suppress auto-launch during inspection
    val isInspectorActive by sdk.isInspectorActiveFlow.collectAsState()

    // Observe session-active state — when a session ends, slide the FAB back in
    val isSessionActive by sdk.isSessionActiveFlow.collectAsState()
    LaunchedEffect(isSessionActive) {
        if (!isSessionActive && config != null) {
            val cfg        = config ?: return@LaunchedEffect
            val isDismissed = prefs.getBoolean("tt-dismissed-${cfg.id}", false)
            val showCount  = prefs.getInt("tt-shows-${cfg.id}", 0)
            val maxReached = cfg.maxShows != null && showCount >= cfg.maxShows
            if (!isDismissed && !maxReached) launcherState = LauncherState.FAB
        }
    }

    // Re-evaluate whenever the page changes
    LaunchedEffect(currentPage, isInspectorActive) {
        // Immediately hide so the old page's launcher doesn't linger during the transition.
        launcherState = LauncherState.HIDDEN

        if (isInspectorActive) return@LaunchedEffect

        // ── Eager loading FAB ───────────────────────────────────────────────
        // If this page was previously known to have a tour (from tt-known-pages) and
        // hasn't been permanently dismissed, show the FAB with a spinner immediately.
        val pageSnap = currentPage
        if (pageSnap != null) {
            val known = sdk.knownPage(prefs, pageSnap)
            if (known != null) launcherState = LauncherState.LOADING
        }

        val cfg = sdk.loadConfig(currentPage) ?: run {
            config = null
            launcherState = LauncherState.HIDDEN
            return@LaunchedEffect
        }

        // ── Prior-tour display condition (element condition N/A on Android) ─
        cfg.displayConditions?.priorTourCondition?.let { dc ->
            val seen = prefs.getInt("tt-shows-${dc.tourId}", 0) > 0
            val done = prefs.getBoolean("tt-completed-${dc.tourId}", false)
            if (dc.rule == "seen"      && !seen) { launcherState = LauncherState.HIDDEN; return@LaunchedEffect }
            if (dc.rule == "completed" && !done) { launcherState = LauncherState.HIDDEN; return@LaunchedEffect }
        }

        config = cfg

        val id           = cfg.id
        val isDismissed  = prefs.getBoolean("tt-dismissed-$id", false)
        val showCount    = prefs.getInt("tt-shows-$id", 0)
        val isMinimised  = sdk.isSessionMinimised(id)
        val maxReached   = cfg.maxShows != null && showCount >= cfg.maxShows

        android.util.Log.d("TTLauncher", "page=$currentPage maxShows=${cfg.maxShows} showCount=$showCount maxReached=$maxReached isDismissed=$isDismissed isMinimised=$isMinimised")

        // ── Carousel check (fires before welcome card) ────────────────────
        val carousel = cfg.splashCarousel
        android.util.Log.d("TTLauncher", "carousel=${carousel != null} slides=${carousel?.slides?.size ?: 0} shownThisSession=$carouselShownThisSession carouselMaxShows=${carousel?.maxShows} carouselShowCount=${prefs.getInt("tt-carousel-shows-$id", 0)}")
        if (carousel != null && carousel.slides.isNotEmpty() && !carouselShownThisSession && !isDismissed) {
            val carouselShows = prefs.getInt("tt-carousel-shows-$id", 0)
            val carouselMaxReached = carousel.maxShows?.let { carouselShows >= it } ?: false
            if (!carouselMaxReached) {
                prefs.edit().putInt("tt-carousel-shows-$id", carouselShows + 1).apply()
                carouselShownThisSession = true
                launcherState = LauncherState.CAROUSEL
                sdk.tracker?.track(TTEventType.CAROUSEL_SHOWN, cfg.id, sdk.siteKey)
                return@LaunchedEffect
            }
        }

        when {
            isDismissed || maxReached -> launcherState = LauncherState.HIDDEN
            cfg.startMinimized || isMinimised -> launcherState = LauncherState.FAB
            else -> {
                launcherState = LauncherState.WELCOME
                prefs.edit().putInt("tt-shows-$id", showCount + 1).apply()
            }
        }
    }

    // Minimise when an inspector starts (mirror iOS NotificationCenter observer)
    LaunchedEffect(isInspectorActive) {
        if (isInspectorActive) launcherState = LauncherState.HIDDEN
    }

    // Loading FAB style — read from known-pages before config arrives.
    val loadingKnown     = currentPage?.let { sdk.knownPage(prefs, it) }
    val loadingFabOnLeft = loadingKnown?.position == "left"
    val loadingFabBg     = com.lovelysomething.tooltiptour.models.parseColor(loadingKnown?.bgColor)
                               ?: Color(0xFF3730A3.toInt())
    // Default FAB dimensions used for the loading spinner (real values come from cfg).
    val defaultFabSize   = 44.dp
    val defaultFabRadius = 22.dp
    val defaultFabBottom = 40.dp

    Box(modifier = Modifier.fillMaxSize().then(modifier)) {
        // ── Eager loading FAB (shown while config is still fetching) ───────────
        AnimatedVisibility(
            visible = launcherState == LauncherState.LOADING,
            enter   = fadeIn(tween(200)),
            exit    = fadeOut(tween(200)),
            modifier = Modifier
                .align(if (loadingFabOnLeft) Alignment.BottomStart else Alignment.BottomEnd)
                .padding(
                    start  = if (loadingFabOnLeft) 20.dp else 0.dp,
                    end    = if (!loadingFabOnLeft) 20.dp else 0.dp,
                    bottom = defaultFabBottom,
                ),
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(defaultFabSize)
                    .shadow(elevation = 8.dp, shape = RoundedCornerShape(defaultFabRadius))
                    .clip(RoundedCornerShape(defaultFabRadius))
                    .background(loadingFabBg),
            ) {
                androidx.compose.material3.CircularProgressIndicator(
                    color       = Color.White,
                    strokeWidth = 2.dp,
                    modifier    = Modifier.size(22.dp),
                )
            }
        }

        // Everything below requires the config to be loaded.
        val cfg = config ?: return@Box

        val fabBg      = cfg.styles?.resolvedFabBgColor    ?: Color(0xFF3730A3.toInt())
        val fabSize    = (cfg.styles?.fab?.size ?: 44.0).dp
        val fabRadius  = (cfg.styles?.fabCornerRadius ?: 24f).dp
        val fabOnLeft  = cfg.styles?.fab?.position == "left"
        val fabBottom  = (cfg.styles?.fab?.bottomOffset ?: 40.0).dp
        val fabIcon    = TTIcon.from(cfg.styles?.fab?.icon)
        val fabLabel   = cfg.fabLabel ?: "Tour"

        // After carousel completes or is dismissed → fall through to normal tour logic
        fun continueAfterCarousel() {
            val id          = cfg.id
            val isDismissed = prefs.getBoolean("tt-dismissed-$id", false)
            val showCount   = prefs.getInt("tt-shows-$id", 0)
            val maxReached  = cfg.maxShows != null && showCount >= cfg.maxShows
            when {
                cfg.steps.isEmpty()               -> launcherState = LauncherState.FAB
                isDismissed || maxReached         -> launcherState = LauncherState.HIDDEN
                cfg.startMinimized || sdk.isSessionMinimised(id) -> launcherState = LauncherState.FAB
                else -> {
                    prefs.edit().putInt("tt-shows-$id", showCount + 1).apply()
                    launcherState = LauncherState.WELCOME
                }
            }
        }

        // ── Carousel (full-screen, fires before welcome card) ──────────────────
        AnimatedVisibility(
            visible  = launcherState == LauncherState.CAROUSEL,
            enter    = slideInVertically(initialOffsetY = { it }) + fadeIn(tween(350)),
            exit     = slideOutVertically(targetOffsetY = { it }) + fadeOut(tween(250)),
            modifier = Modifier.fillMaxSize(),
        ) {
            cfg.splashCarousel?.let { carousel ->
                TTSplashCarouselView(
                    carousel        = carousel,
                    btnCornerRadius = cfg.styles?.btnCornerRadius ?: 8f,
                    onSlideViewed   = { index ->
                        sdk.tracker?.track(TTEventType.CAROUSEL_SLIDE_VIEWED, cfg.id, sdk.siteKey, index)
                    },
                    onDone          = {
                        sdk.tracker?.track(TTEventType.CAROUSEL_COMPLETED, cfg.id, sdk.siteKey)
                        launcherState = LauncherState.HIDDEN; continueAfterCarousel()
                    },
                    onDismiss       = {
                        sdk.tracker?.track(TTEventType.CAROUSEL_DISMISSED, cfg.id, sdk.siteKey)
                        launcherState = LauncherState.HIDDEN; continueAfterCarousel()
                    },
                )
            }
        }

        // ── Welcome card ───────────────────────────────────────────────────────
        AnimatedVisibility(
            visible = launcherState == LauncherState.WELCOME,
            enter   = slideInVertically(initialOffsetY = { it }) + fadeIn(tween(300)),
            exit    = slideOutVertically(targetOffsetY = { it }) + fadeOut(tween(200)),
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            TTWelcomeCardView(
                config          = cfg,
                onStart         = {
                    launcherState = LauncherState.HIDDEN
                    sdk.startSession(cfg)
                },
                onDismiss       = {
                    sdk.markSessionMinimised(cfg.id)
                    launcherState = LauncherState.FAB
                },
                onDontShowAgain = {
                    prefs.edit().putBoolean("tt-dismissed-${cfg.id}", true).apply()
                    launcherState = LauncherState.HIDDEN
                },
            )
        }

        // ── Minimised FAB ──────────────────────────────────────────────────────
        AnimatedVisibility(
            visible = launcherState == LauncherState.FAB,
            enter   = slideInHorizontally(
                initialOffsetX = { if (fabOnLeft) -it else it },
                animationSpec  = tween(300),
            ) + fadeIn(tween(250)),
            exit    = slideOutHorizontally(
                targetOffsetX = { if (fabOnLeft) -it else it },
                animationSpec = tween(200),
            ) + fadeOut(tween(200)),
            modifier = Modifier
                .align(if (fabOnLeft) Alignment.BottomStart else Alignment.BottomEnd)
                .padding(
                    start  = if (fabOnLeft) 20.dp else 0.dp,
                    end    = if (!fabOnLeft) 20.dp else 0.dp,
                    bottom = fabBottom,
                ),
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(fabSize)
                    .shadow(elevation = 8.dp, shape = RoundedCornerShape(fabRadius))
                    .clip(RoundedCornerShape(fabRadius))
                    .background(fabBg)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) {
                        val showCount  = prefs.getInt("tt-shows-${cfg.id}", 0)
                        val maxReached = cfg.maxShows != null && showCount >= cfg.maxShows
                        if (!maxReached) {
                            prefs.edit().putInt("tt-shows-${cfg.id}", showCount + 1).apply()
                            if (cfg.welcomeMode == "button") {
                                // Button-only mode: skip welcome card, launch tour directly
                                launcherState = LauncherState.HIDDEN
                                sdk.startSession(cfg)
                            } else {
                                launcherState = LauncherState.WELCOME
                            }
                        }
                    },
            ) {
                androidx.compose.foundation.Canvas(modifier = Modifier.size(22.dp)) {
                    drawTTIcon(fabIcon, Color.White, size.width)
                }
            }
        }
    }
}
