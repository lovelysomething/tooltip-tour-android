package com.lovelysomething.tooltiptour.registry

import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import java.util.concurrent.ConcurrentHashMap

/**
 * Stores window-space bounding rects for views tagged with [Modifier.ttTarget].
 *
 * Mirrors iOS TTViewRegistry.swift. Uses [boundsInWindow] which gives coordinates
 * relative to the Window root — the same space the overlay ComposeView occupies.
 */
object TTViewRegistry {

    private val frames = ConcurrentHashMap<String, Rect>()

    /** All currently registered identifier → frame pairs (window coordinates, px). */
    val allFrames: Map<String, Rect> get() = frames

    fun register(identifier: String, frame: Rect) {
        frames[identifier] = frame
    }

    fun unregister(identifier: String) {
        frames.remove(identifier)
    }

    fun frame(identifier: String): Rect? = frames[identifier]

    /**
     * Returns the identifier whose registered frame contains [point].
     * Picks the smallest matching frame so nested elements resolve to the most specific target.
     */
    fun identifier(at: androidx.compose.ui.geometry.Offset): String? =
        frames.entries
            .filter { it.value.contains(at) }
            .minByOrNull { it.value.width * it.value.height }
            ?.key
}

// ── Compose modifier ──────────────────────────────────────────────────────────

/**
 * Register this composable as a Tooltip Tour target with the given [identifier].
 * The identifier must match the selector set in the dashboard.
 *
 * ```kotlin
 * Button(
 *     modifier = Modifier.ttTarget("get-started"),
 *     onClick = { /* … */ }
 * ) { Text("Get started") }
 * ```
 */
fun Modifier.ttTarget(identifier: String): Modifier = composed {
    DisposableEffect(identifier) {
        onDispose { TTViewRegistry.unregister(identifier) }
    }
    onGloballyPositioned { coords ->
        TTViewRegistry.register(identifier, coords.boundsInWindow())
    }
}
