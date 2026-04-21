package com.lovelysomething.tooltiptour.ui

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import com.lovelysomething.tooltiptour.registry.TTViewRegistry
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Scroll bus that lets the walkthrough session scroll content into view.
 *
 * Mirrors iOS TTScrollBus.swift. The SDK writes a target identifier; host-side
 * [Modifier.ttScrollable] / [Modifier.ttScrollableColumn] wrappers listen and
 * call the appropriate scroll API.
 */
object TTScrollBus {

    private val _events = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val events = _events.asSharedFlow()

    /** Called by [TTWalkthroughSession] to request a scroll to [identifier]. */
    suspend fun scrollTo(identifier: String) {
        _events.emit(identifier)
    }
}

// ── LazyColumn helper ─────────────────────────────────────────────────────────

/**
 * Attach to a [LazyListState]-backed list.
 * [indexMap] must map ttTarget identifiers to their item indices.
 *
 * ```kotlin
 * val listState = rememberLazyListState()
 * val indexMap  = items.mapIndexed { i, it -> it.id to i }.toMap()
 * LazyColumn(state = listState, modifier = Modifier.ttScrollable(listState, indexMap)) { … }
 * ```
 */
@Composable
fun ttScrollable(state: LazyListState, indexMap: Map<String, Int>) {
    LaunchedEffect(indexMap) {
        TTScrollBus.events.collect { id ->
            val index = indexMap[id] ?: return@collect
            state.animateScrollToItem(index)
            delay(50)
        }
    }
}

// ── Column + verticalScroll helper ────────────────────────────────────────────

/**
 * Attach to a [ScrollState]-backed Column so the walkthrough session can
 * scroll any ttTarget into view.
 *
 * Apply this BEFORE [Modifier.verticalScroll] in the modifier chain:
 * ```kotlin
 * val scrollState = rememberScrollState()
 * Column(
 *     modifier = Modifier
 *         .fillMaxSize()
 *         .ttScrollableColumn(scrollState)   // ← here
 *         .verticalScroll(scrollState)
 * ) { … }
 * ```
 *
 * The function uses the target's current window-space frame from [TTViewRegistry]
 * together with the current scroll offset to derive the new scroll position that
 * places the item roughly 1/3 of the way down the screen.
 */
@Composable
fun Modifier.ttScrollableColumn(scrollState: ScrollState): Modifier {
    LaunchedEffect(scrollState) {
        TTScrollBus.events.collect { id ->
            val frame = TTViewRegistry.frame(id) ?: run {
                android.util.Log.d("TTScrollBus", "[$id] frame is NULL — not registered yet")
                return@collect
            }
            val screenH = android.content.res.Resources.getSystem()
                .displayMetrics.heightPixels.toFloat()
            val currentScroll = scrollState.value

            android.util.Log.d("TTScrollBus", "[$id] frame=$frame screenH=$screenH scroll=$currentScroll maxScroll=${scrollState.maxValue}")

            // Skip if the item is already comfortably visible on screen
            val margin = screenH * 0.15f
            if (frame.top >= margin && frame.bottom <= screenH - margin) {
                android.util.Log.d("TTScrollBus", "[$id] already visible — skipping scroll")
                return@collect
            }

            // frame.top is the item's visual position in window space, already accounting
            // for the current scroll offset (verticalScroll uses placeRelativeWithLayer
            // which shifts layout coordinates, so boundsInWindow reflects the translated pos).
            //
            // Visual pos: frame.top = contentY - currentScroll + containerTop
            // We want:    contentY - newScroll + containerTop = targetY
            // Therefore:  newScroll = (frame.top + currentScroll) - targetY
            val targetY = screenH / 3f
            val newScroll = (frame.top + currentScroll - targetY)
                .toInt()
                .coerceIn(0, scrollState.maxValue.takeIf { it > 0 } ?: Int.MAX_VALUE)

            android.util.Log.d("TTScrollBus", "[$id] targetY=$targetY newScroll=$newScroll (raw=${frame.top + currentScroll - targetY})")

            scrollState.animateScrollTo(newScroll)
        }
    }
    return this
}
