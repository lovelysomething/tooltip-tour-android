package com.lovelysomething.tooltiptour.registry

import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Tracks which screen the user is currently viewing.
 * Populated automatically by the [Modifier.ttPage] Compose modifier.
 *
 * Mirrors iOS TTPageRegistry.swift.
 */
object TTPageRegistry {

    private val _currentPage = MutableStateFlow<String?>(null)
    val currentPageFlow: StateFlow<String?> = _currentPage.asStateFlow()

    /** The identifier of the most recently appeared screen. */
    val currentPage: String? get() = _currentPage.value

    // Stack so nested/overlaid views resolve correctly on disappear.
    private val pageStack = mutableListOf<String>()

    @Synchronized
    fun setPage(id: String) {
        pageStack.removeAll { it == id }
        pageStack.add(id)
        _currentPage.value = id
    }

    @Synchronized
    fun clearPage(id: String) {
        pageStack.removeAll { it == id }
        _currentPage.value = pageStack.lastOrNull()
    }
}

// ── Compose modifier ──────────────────────────────────────────────────────────

/**
 * Register this composable as the current screen for Tooltip Tour page targeting.
 * Add to the root composable of each screen or tab.
 *
 * ```kotlin
 * Box(
 *     modifier = Modifier
 *         .fillMaxSize()
 *         .ttPage("home")
 * ) {
 *     // your screen content
 *     TTLauncherView()
 * }
 * ```
 */
fun Modifier.ttPage(identifier: String): Modifier = composed {
    DisposableEffect(identifier) {
        TTPageRegistry.setPage(identifier)
        onDispose { TTPageRegistry.clearPage(identifier) }
    }
    this
}
