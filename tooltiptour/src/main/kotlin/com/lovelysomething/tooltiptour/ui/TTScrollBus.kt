package com.lovelysomething.tooltiptour.ui

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Scroll bus that lets the walkthrough session scroll content into view.
 *
 * Mirrors iOS TTScrollBus.swift. The SDK writes a target identifier; host-side
 * [Modifier.ttScrollable] wrappers listen and call the appropriate scroll API.
 *
 * Usage (LazyColumn):
 * ```kotlin
 * val listState = rememberLazyListState()
 * LazyColumn(
 *     state = listState,
 *     modifier = Modifier.ttScrollable(listState, indexMap)
 * ) { … }
 * ```
 * where `indexMap` is a Map<String, Int> mapping ttTarget identifiers to list indices.
 */
object TTScrollBus {

    private val _events = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val events = _events.asSharedFlow()

    /** Called by [TTWalkthroughSession] to request a scroll to [identifier]. */
    suspend fun scrollTo(identifier: String) {
        _events.emit(identifier)
    }
}

// ── Compose helper ────────────────────────────────────────────────────────────

/**
 * Attach to the content of a [LazyListState]-backed list.
 * [indexMap] must map ttTarget identifiers to their item indices.
 *
 * ```kotlin
 * LazyColumn(state = listState) {
 *     items(items, key = { it.id }) { item ->
 *         Row(modifier = Modifier.ttTarget(item.id)) { … }
 *     }
 * }
 * // Then tell the bus how to find each item:
 * LaunchedEffect(items) { ttScrollable(listState, items.mapIndexed { i, it -> it.id to i }.toMap()) }
 * ```
 */
@Composable
fun ttScrollable(state: LazyListState, indexMap: Map<String, Int>) {
    LaunchedEffect(indexMap) {
        TTScrollBus.events.collect { id ->
            val index = indexMap[id] ?: return@collect
            state.animateScrollToItem(index)
            // Clear the event after a short delay so the same id can fire again
            delay(50)
        }
    }
}
