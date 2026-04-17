package com.lovelysomething.tooltiptour.inspector

import android.app.Activity
import android.view.ViewGroup
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.ViewTreeLifecycleOwner
import androidx.lifecycle.ViewTreeViewModelStoreOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.lovelysomething.tooltiptour.networking.TTNetworkClient
import com.lovelysomething.tooltiptour.registry.TTViewRegistry
import kotlinx.coroutines.*

enum class TTInspectorMode { ELEMENT, PAGE }
private enum class InspectorMode { NAVIGATE, HIGHLIGHT, SELECT }
private enum class InspectorPhase { TAPPING, CONFIRMING, DONE }

/**
 * Visual inspector overlay — lets developers tap elements to capture identifiers
 * that are then sent to the dashboard via PATCH /api/inspector/sessions/{id}.
 *
 * Mirrors iOS TTInspector.swift. Activated via deep link:
 *   `tooltiptour://inspect?session=xxx&base=https://app.lovelysomething.com&mode=element`
 */
internal class TTInspector(
    private val sessionId: String,
    private val networkClient: TTNetworkClient,
    private val mode: TTInspectorMode = TTInspectorMode.ELEMENT,
    private val scope: CoroutineScope,
) {
    var onEnd: (() -> Unit)? = null
    private var composeView: ComposeView? = null
    private var currentActivity: Activity? = null

    // ── Observable UI state ────────────────────────────────────────────────────

    private val _inspectorMode  = mutableStateOf(InspectorMode.NAVIGATE)
    private val _phase          = mutableStateOf(InspectorPhase.TAPPING)
    private val _capturedId     = mutableStateOf("")
    private val _capturedName   = mutableStateOf("")
    private val _tapPoint       = mutableStateOf<Offset?>(null)

    // ── Start ──────────────────────────────────────────────────────────────────

    fun start(activity: Activity) {
        currentActivity = activity
        val view = ComposeView(activity).apply {
            val lifecycle = activity as? androidx.lifecycle.LifecycleOwner
            if (lifecycle != null) ViewTreeLifecycleOwner.set(this, lifecycle)
            val vmso = activity as? ViewModelStoreOwner
            if (vmso != null) ViewTreeViewModelStoreOwner.set(this, vmso)
            val ssro = activity as? androidx.savedstate.SavedStateRegistryOwner
            if (ssro != null) setViewTreeSavedStateRegistryOwner(ssro)
            setViewCompositionStrategy(
                androidx.compose.ui.platform.ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed
            )
            setContent { InspectorOverlay() }
        }
        val decorView = activity.window.decorView as ViewGroup
        decorView.addView(view, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        composeView = view
    }

    // ── Overlay composable ─────────────────────────────────────────────────────

    @Composable
    private fun InspectorOverlay() {
        val inspMode  by _inspectorMode
        val phase     by _phase
        val density   = LocalDensity.current
        val allFrames = TTViewRegistry.allFrames

        val brandColor = Color(0xFF1925AA.toInt())

        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (inspMode == InspectorMode.SELECT || inspMode == InspectorMode.HIGHLIGHT)
                        Modifier.pointerInput(inspMode) {
                            detectTapGestures { offset ->
                                if (phase == InspectorPhase.TAPPING) {
                                    handleTap(offset, allFrames)
                                }
                            }
                        }
                    else Modifier
                )
        ) {
            // Subtle blue tint in Select mode
            if (inspMode == InspectorMode.SELECT && phase == InspectorPhase.TAPPING) {
                Box(modifier = Modifier.fillMaxSize().background(Color.Blue.copy(alpha = 0.04f)))
            }

            // Highlight mode: draw colored borders around all registered views
            if (inspMode == InspectorMode.HIGHLIGHT) {
                allFrames.forEach { (id, rect) ->
                    Box(
                        modifier = Modifier
                            .offset { IntOffset(rect.left.toInt(), rect.top.toInt()) }
                            .size(
                                with(density) { rect.width.toDp() },
                                with(density) { rect.height.toDp() },
                            )
                            .border(2.dp, Color(0xFF3B82F6.toInt()).copy(alpha = 0.6f))
                            .background(Color(0xFF3B82F6.toInt()).copy(alpha = 0.12f))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                            ) {
                                if (phase == InspectorPhase.TAPPING) {
                                    _capturedId.value   = id
                                    _capturedName.value = id
                                    _phase.value = InspectorPhase.CONFIRMING
                                }
                            },
                    ) {
                        Text(
                            text     = id,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color    = Color.White,
                            modifier = Modifier
                                .padding(4.dp)
                                .background(Color(0xFF3B82F6.toInt()).copy(alpha = 0.8f))
                                .padding(horizontal = 4.dp, vertical = 2.dp),
                        )
                    }
                }
            }

            // ── Bottom sheet confirm card ──────────────────────────────────────
            AnimatedVisibility(
                visible  = phase == InspectorPhase.CONFIRMING || phase == InspectorPhase.DONE,
                enter    = slideInVertically { it } + fadeIn(tween(200)),
                exit     = slideOutVertically { it } + fadeOut(tween(200)),
                modifier = Modifier.align(Alignment.BottomCenter),
            ) {
                ConfirmCard(brandColor = brandColor)
            }

            // Dim when confirming
            if (phase == InspectorPhase.CONFIRMING || phase == InspectorPhase.DONE) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.45f))
                        .pointerInput(Unit) { detectTapGestures { } },
                )
            }

            // ── Top banner ─────────────────────────────────────────────────────
            InspectorBanner(brandColor = brandColor, mode = inspMode, onModeChange = {
                _inspectorMode.value = it
            })
        }
    }

    @Composable
    private fun ConfirmCard(brandColor: Color) {
        val isDone = _phase.value == InspectorPhase.DONE
        var identifier by remember { mutableStateOf("") }
        val capturedId = _capturedId.value

        LaunchedEffect(capturedId) {
            identifier = if (capturedId != "unknown") capturedId else ""
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(bottom = 48.dp)
        ) {
            // Header
            Column(
                modifier = Modifier.padding(20.dp).fillMaxWidth()
            ) {
                Text(
                    text = if (isDone) "SENT TO DASHBOARD ✓" else "SET IDENTIFIER",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp,
                    color = brandColor.copy(alpha = 0.65f),
                )
                Text(
                    text = if (isDone) identifier
                           else if (mode == TTInspectorMode.PAGE) "Page identified as"
                           else "Name this element",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFF0D0A1C.toInt()),
                )
            }

            if (!isDone) {
                // Editable field
                BasicTextField(
                    value = identifier,
                    onValueChange = { identifier = it },
                    singleLine = true,
                    textStyle = TextStyle(
                        fontSize = 14.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Medium,
                        color = brandColor,
                    ),
                    cursorBrush = SolidColor(brandColor),
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(brandColor.copy(alpha = 0.06f))
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    decorationBox = { inner ->
                        if (identifier.isEmpty()) {
                            Text("e.g. loginButton or welcomeTitle",
                                fontSize = 14.sp, fontFamily = FontFamily.Monospace,
                                color = brandColor.copy(alpha = 0.35f))
                        }
                        inner()
                    }
                )

                // Retry / Send row
                Row(modifier = Modifier.fillMaxWidth()) {
                    TextButton(
                        onClick  = { retryCapture() },
                        modifier = Modifier.weight(1f).padding(vertical = 4.dp),
                    ) {
                        Text("RETRY", fontSize = 11.sp, fontWeight = FontWeight.Bold,
                            letterSpacing = 1.5.sp, color = Color(0xFF0D0A1C.toInt()).copy(alpha = 0.4f))
                    }
                    Button(
                        onClick  = { if (identifier.isNotBlank()) submitCapture(identifier) },
                        enabled  = identifier.isNotBlank(),
                        shape    = RoundedCornerShape(0.dp),
                        colors   = ButtonDefaults.buttonColors(containerColor = brandColor),
                        modifier = Modifier.weight(1f).padding(vertical = 4.dp),
                    ) {
                        Text("SEND TO SITE →", fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
                    }
                }
            }
        }
    }

    @Composable
    private fun InspectorBanner(
        brandColor: Color,
        mode: InspectorMode,
        onModeChange: (InspectorMode) -> Unit,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 48.dp, start = 16.dp, end = 16.dp)
                .background(brandColor, RoundedCornerShape(0.dp))
                .padding(horizontal = 16.dp)
                .height(48.dp),
        ) {
            if (this@TTInspector.mode == TTInspectorMode.PAGE) {
                Text(
                    "Navigate to your screen",
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = { capturePage() }) {
                    Text("SET PAGE", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp)
                }
            } else {
                // Navigate / Highlight / Select tabs
                val tabs = listOf("Navigate" to InspectorMode.NAVIGATE, "Highlight" to InspectorMode.HIGHLIGHT, "Select" to InspectorMode.SELECT)
                Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    tabs.forEach { (label, tabMode) ->
                        val selected = mode == tabMode
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .clip(RoundedCornerShape(0.dp))
                                .background(if (selected) Color.White.copy(0.28f) else Color.Transparent)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                ) { onModeChange(tabMode) }
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                        ) {
                            Text(label, color = if (selected) Color.White else Color.White.copy(0.6f),
                                fontSize = 12.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold)
                        }
                    }
                }
            }

            // ✕ close
            TextButton(onClick = { tearDown() }) {
                Text("✕", color = Color.White.copy(alpha = 0.7f), fontSize = 16.sp)
            }
        }
    }

    // ── Tap handling ───────────────────────────────────────────────────────────

    private fun handleTap(point: Offset, allFrames: Map<String, Rect>) {
        // Find closest registered view that contains the tap point
        val id = TTViewRegistry.identifier(point) ?: "unknown"
        _capturedId.value   = id
        _capturedName.value = id
        _phase.value = InspectorPhase.CONFIRMING
    }

    private fun capturePage() {
        val page = com.lovelysomething.tooltiptour.registry.TTPageRegistry.currentPage ?: "screen"
        _capturedId.value   = page
        _capturedName.value = page
        _phase.value = InspectorPhase.CONFIRMING
    }

    private fun retryCapture() {
        _capturedId.value   = ""
        _capturedName.value = ""
        _phase.value = InspectorPhase.TAPPING
        _inspectorMode.value = InspectorMode.NAVIGATE
    }

    private fun submitCapture(identifier: String) {
        _phase.value = InspectorPhase.DONE
        scope.launch {
            networkClient.updateInspectorSession(sessionId, identifier, identifier)
            delay(1200)
            tearDown()
        }
    }

    // ── Tear down ──────────────────────────────────────────────────────────────

    private fun tearDown() {
        val activity = currentActivity ?: return
        val view     = composeView    ?: return
        activity.runOnUiThread {
            (activity.window.decorView as? ViewGroup)?.removeView(view)
        }
        composeView     = null
        currentActivity = null
        onEnd?.invoke()
    }
}
