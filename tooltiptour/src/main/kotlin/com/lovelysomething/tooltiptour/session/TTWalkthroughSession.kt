package com.lovelysomething.tooltiptour.session

import android.app.Activity
import android.view.ViewGroup
import androidx.compose.animation.core.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.*
import androidx.savedstate.findViewTreeSavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.lovelysomething.tooltiptour.models.TTConfig
import com.lovelysomething.tooltiptour.networking.TTEventTracker
import com.lovelysomething.tooltiptour.networking.TTEventType
import com.lovelysomething.tooltiptour.registry.TTViewRegistry
import com.lovelysomething.tooltiptour.ui.*
import kotlinx.coroutines.*

/**
 * Manages the active walkthrough overlay.
 *
 * Adds a full-screen [ComposeView] to the Activity's decor view, rendering:
 *  - [TTSpotlightCanvas] dim with cutout
 *  - [TTBeaconView] positioned at the target
 *  - [TTStepCardView] anchored above or below the target
 *
 * Mirrors iOS TTWalkthroughSession.swift.
 */
internal class TTWalkthroughSession(
    private val config: TTConfig,
    private val siteKey: String,
    private val tracker: TTEventTracker,
    private val scope: CoroutineScope,
) {
    var onEnd: (() -> Unit)? = null

    private var composeView: ComposeView? = null
    private var currentActivity: Activity? = null

    // ── Observable state ───────────────────────────────────────────────────────
    private val _currentStep   = mutableIntStateOf(0)
    private val _highlightRect = mutableStateOf<Rect?>(null)

    // ── Start ──────────────────────────────────────────────────────────────────

    fun start(activity: Activity) {
        currentActivity = activity
        tracker.track(TTEventType.GUIDE_SHOWN, config.id, siteKey)
        goToStep(0)
        attachOverlay(activity)
    }

    private fun attachOverlay(activity: Activity) {
        val view = ComposeView(activity).apply {
            // Wire up lifecycle so Compose works when added programmatically
            val lifecycle = activity as? LifecycleOwner
            if (lifecycle != null) {
                ViewTreeLifecycleOwner.set(this, lifecycle)
            }
            val vmso = activity as? ViewModelStoreOwner
            if (vmso != null) {
                ViewTreeViewModelStoreOwner.set(this, vmso)
            }
            val ssro = activity.findViewTreeSavedStateRegistryOwner()
                ?: (activity as? androidx.savedstate.SavedStateRegistryOwner)
            if (ssro != null) {
                setViewTreeSavedStateRegistryOwner(ssro)
            }
            setViewCompositionStrategy(
                ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed
            )
            setContent { OverlayContent() }
        }

        val decorView = activity.window.decorView as ViewGroup
        decorView.addView(
            view,
            ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            ),
        )
        composeView = view
    }

    // ── Overlay composable ─────────────────────────────────────────────────────

    @Composable
    private fun OverlayContent() {
        val step      = _currentStep.intValue
        val stepData  = config.steps.getOrNull(step) ?: return
        val highlight = _highlightRect.value
        val density   = LocalDensity.current

        // Animate beacon/card position changes
        val animatedRect by animateValueAsState(
            targetValue  = highlight ?: Rect.Zero,
            typeConverter = RectConverter,
            animationSpec = tween(300),
            label = "spotlight-rect",
        )

        // Card position: above target if the target is in the lower half, below otherwise.
        // Compute in pixels from the rect.
        val screenHeight = with(density) { 800.dp.toPx() } // fallback; actual via BoxWithConstraints
        val showCardAbove = animatedRect.center.y > screenHeight * 0.55f

        // Beacon position: top-centre of the highlight rect
        val beaconSizePx = with(density) { 32.dp.toPx() }
        val beaconX = (animatedRect.left + animatedRect.width / 2f - beaconSizePx / 2f).toInt()
        val beaconY = (if (showCardAbove) animatedRect.top - beaconSizePx - 8f
                       else animatedRect.bottom + 8f).toInt()

        val cardPaddingPx = with(density) { 20.dp.toPx() }.toInt()
        val cardY = if (showCardAbove) {
            (animatedRect.top - with(density) { 220.dp.toPx() }).toInt().coerceAtLeast(cardPaddingPx)
        } else {
            (animatedRect.bottom + beaconSizePx + 16f + with(density) { 8.dp.toPx() }).toInt()
        }

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures { /* consume all background touches */ }
                },
        ) {
            val maxHeightPx = with(density) { maxHeight.toPx() }

            // Spotlight
            TTSpotlightCanvas(
                highlightRect = if (highlight != null) animatedRect else null,
                modifier      = Modifier.fillMaxSize(),
            )

            // Beacon
            if (highlight != null) {
                val beaconStyle = TTBeaconStyle.entries.firstOrNull {
                    it.name.lowercase() == config.styles?.beacon?.style
                } ?: TTBeaconStyle.NUMBERED

                TTBeaconView(
                    stepNumber = step + 1,
                    style      = beaconStyle,
                    color      = config.styles?.resolvedBeaconBgColor ?: Color(0xFF3730A3.toInt()),
                    labelColor = config.styles?.resolvedBeaconTextColor ?: Color.White,
                    isActive   = true,
                    modifier   = Modifier.offset { IntOffset(beaconX, beaconY) },
                )
            }

            // Step card
            TTStepCardView(
                step       = stepData,
                stepIndex  = step,
                totalSteps = config.steps.size,
                styles     = config.styles,
                onNext     = { advance() },
                onBack     = { goBack() },
                onDismiss  = { dismiss() },
                modifier   = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .offset { IntOffset(0, cardY.coerceIn(cardPaddingPx, (maxHeightPx - 300f).toInt())) },
            )
        }
    }

    // ── Step navigation ────────────────────────────────────────────────────────

    private fun goToStep(index: Int) {
        _currentStep.intValue = index
        val selector = config.steps.getOrNull(index)?.selector ?: return

        // Scroll to the target if needed, then update the highlight rect
        scope.launch {
            TTScrollBus.scrollTo(selector)
            delay(500) // wait for scroll to settle
            _highlightRect.value = TTViewRegistry.frame(selector)
        }
    }

    private fun advance() {
        val next = _currentStep.intValue + 1
        tracker.track(TTEventType.STEP_COMPLETED, config.id, siteKey, _currentStep.intValue)
        if (next >= config.steps.size) {
            tracker.track(TTEventType.GUIDE_COMPLETED, config.id, siteKey)
            dismiss()
        } else {
            goToStep(next)
        }
    }

    private fun goBack() {
        val prev = (_currentStep.intValue - 1).coerceAtLeast(0)
        goToStep(prev)
    }

    // ── Dismiss ────────────────────────────────────────────────────────────────

    fun dismiss() {
        tracker.track(TTEventType.GUIDE_DISMISSED, config.id, siteKey)
        tearDown()
    }

    private fun tearDown() {
        val activity = currentActivity ?: return
        val view     = composeView    ?: return
        activity.runOnUiThread {
            (activity.window.decorView as? ViewGroup)?.removeView(view)
        }
        composeView      = null
        currentActivity  = null
        onEnd?.invoke()
    }
}

// ── Rect animation type converter ─────────────────────────────────────────────

private val RectConverter = TwoWayConverter<Rect, AnimationVector4D>(
    convertToVector = { r ->
        AnimationVector4D(r.left, r.top, r.right, r.bottom)
    },
    convertFromVector = { v ->
        Rect(v.v1, v.v2, v.v3, v.v4)
    },
)
