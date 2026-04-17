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
import com.lovelysomething.tooltiptour.registry.TTPageRegistry

private enum class LauncherState { HIDDEN, WELCOME, FAB }

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

    // Observe inspector-active state so we suppress auto-launch during inspection
    val isInspectorActive by sdk.isInspectorActiveFlow.collectAsState()

    // Re-evaluate whenever the page changes
    LaunchedEffect(currentPage, isInspectorActive) {
        if (isInspectorActive) {
            launcherState = LauncherState.HIDDEN
            return@LaunchedEffect
        }
        val cfg = sdk.loadConfig(currentPage) ?: run {
            config = null
            launcherState = LauncherState.HIDDEN
            return@LaunchedEffect
        }
        config = cfg

        val id           = cfg.id
        val isDismissed  = prefs.getBoolean("tt-dismissed-$id", false)
        val showCount    = prefs.getInt("tt-shows-$id", 0)
        val isMinimised  = sdk.isSessionMinimised(id)

        if (isDismissed) {
            launcherState = LauncherState.HIDDEN
            return@LaunchedEffect
        }

        if (!cfg.startMinimized && !isMinimised &&
            (cfg.maxShows == null || showCount < cfg.maxShows)
        ) {
            launcherState = LauncherState.WELCOME
            prefs.edit().putInt("tt-shows-$id", showCount + 1).apply()
        } else {
            launcherState = LauncherState.FAB
        }
    }

    // Minimise when an inspector starts (mirror iOS NotificationCenter observer)
    LaunchedEffect(isInspectorActive) {
        if (isInspectorActive) launcherState = LauncherState.HIDDEN
    }

    val cfg = config ?: return

    val fabBg      = cfg.styles?.resolvedFabBgColor    ?: Color(0xFF3730A3.toInt())
    val fabSize    = (cfg.styles?.fab?.size ?: 44.0).dp
    val fabRadius  = (cfg.styles?.fabCornerRadius ?: 24f).dp
    val fabOnLeft  = cfg.styles?.fab?.position == "left"
    val fabBottom  = (cfg.styles?.fab?.bottomOffset ?: 40.0).dp
    val fabIcon    = TTIcon.from(cfg.styles?.fab?.icon)
    val fabLabel   = cfg.fabLabel ?: "Tour"

    Box(modifier = Modifier.fillMaxSize().then(modifier)) {
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
            enter   = scaleIn() + fadeIn(tween(250)),
            exit    = scaleOut() + fadeOut(tween(200)),
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
                        launcherState = LauncherState.WELCOME
                    },
            ) {
                androidx.compose.foundation.Canvas(modifier = Modifier.size(22.dp)) {
                    drawTTIcon(fabIcon, Color.White, size.width)
                }
            }
        }
    }
}
