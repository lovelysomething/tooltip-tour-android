package com.lovelysomething.tooltiptour

import android.app.Activity
import android.app.Application
import android.os.Bundle
import com.lovelysomething.tooltiptour.inspector.TTInspector
import com.lovelysomething.tooltiptour.inspector.TTInspectorMode
import com.lovelysomething.tooltiptour.models.TTConfig
import com.lovelysomething.tooltiptour.networking.TTEventTracker
import com.lovelysomething.tooltiptour.networking.TTNetworkClient
import com.lovelysomething.tooltiptour.session.TTWalkthroughSession
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Main entry point for the Tooltip Tour Android SDK.
 *
 * Setup (Application.onCreate or Activity.onCreate):
 * ```kotlin
 * TooltipTour.configure(application, siteKey = "sk_your_key")
 * ```
 *
 * Add the launcher to each screen:
 * ```kotlin
 * Box(modifier = Modifier.fillMaxSize().ttPage("home")) {
 *     YourContent()
 *     TTLauncherView()
 * }
 * ```
 *
 * Handle deep links for the Visual Inspector:
 * ```kotlin
 * intent?.data?.let { TooltipTour.instance?.handleDeepLink(it) }
 * ```
 *
 * Mirrors iOS TooltipTour.swift.
 */
class TooltipTour private constructor() {

    companion object {
        /** The configured singleton — null until [configure] is called. */
        @Volatile
        var instance: TooltipTour? = null
            private set

        /**
         * Configure the SDK. Call once in Application.onCreate.
         * @param application Your Application instance (used for Activity lifecycle tracking).
         * @param siteKey     Your site key from the Tooltip Tour dashboard.
         * @param baseUrl     Override the API base URL (optional, for self-hosted installs).
         */
        @JvmStatic
        fun configure(
            application: Application,
            siteKey: String,
            baseUrl: String = "https://app.lovelysomething.com",
        ): TooltipTour {
            val sdk = TooltipTour().also {
                it.siteKey       = siteKey
                it.baseUrl       = baseUrl
                it.networkClient = TTNetworkClient(baseUrl)
                it.tracker       = TTEventTracker(baseUrl, it.scope)
            }
            instance = sdk
            application.registerActivityLifecycleCallbacks(sdk.lifecycleCallbacks)
            return sdk
        }
    }

    // ── Private state ──────────────────────────────────────────────────────────

    internal var siteKey: String = ""
    internal var baseUrl: String = "https://app.lovelysomething.com"
    internal var networkClient: TTNetworkClient? = null
    internal var tracker: TTEventTracker? = null

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    /** Shared config cache — populated by [prefetchAll] or lazily by [loadConfig]. */
    private val configCache = mutableMapOf<String, TTConfig>()

    private var activeSession: TTWalkthroughSession? = null
    private var activeInspector: TTInspector? = null

    /** Session IDs that the user explicitly minimised this session (not persisted). */
    private val sessionMinimisedIds = mutableSetOf<String>()

    // ── Inspector-active observable (read by TTLauncherView) ──────────────────

    private val _isInspectorActive = MutableStateFlow(false)
    val isInspectorActiveFlow: StateFlow<Boolean> = _isInspectorActive.asStateFlow()
    val isInspectorActive: Boolean get() = activeInspector != null

    // ── Activity tracking ──────────────────────────────────────────────────────

    internal var currentActivity: Activity? = null

    private val lifecycleCallbacks = object : Application.ActivityLifecycleCallbacks {
        override fun onActivityResumed(activity: Activity)  { currentActivity = activity }
        override fun onActivityPaused(activity: Activity)   { if (currentActivity == activity) currentActivity = null }
        override fun onActivityCreated(a: Activity, b: Bundle?) = Unit
        override fun onActivityStarted(activity: Activity)  = Unit
        override fun onActivityStopped(activity: Activity)  = Unit
        override fun onActivitySaveInstanceState(a: Activity, b: Bundle) = Unit
        override fun onActivityDestroyed(activity: Activity) {
            if (currentActivity == activity) currentActivity = null
        }
    }

    // ── Public API ─────────────────────────────────────────────────────────────

    /**
     * Pre-fetch all tour configs for this site and cache them.
     * Call once at app startup (after configure) so every screen loads instantly.
     */
    fun prefetchAll() {
        scope.launch {
            val fetched = networkClient?.fetchAllConfigs(siteKey) ?: return@launch
            configCache.putAll(fetched)
        }
    }

    /**
     * Fetch the config for the given [page] identifier (or global if null).
     * Checks the shared cache first so prefetchAll() results are instant.
     */
    suspend fun loadConfig(page: String? = null): TTConfig? {
        if (page != null) configCache[page]?.let { return it }
        val config = networkClient?.fetchConfig(siteKey, page) ?: return null
        if (page != null) configCache[page] = config
        return config
    }

    /**
     * Start the walkthrough with a pre-loaded config. Called internally by TTLauncherView.
     */
    fun startSession(config: TTConfig) {
        if (activeSession != null) return
        val t = tracker ?: return
        val activity = currentActivity ?: return
        val session = TTWalkthroughSession(config, siteKey, t, scope)
        session.onEnd = {
            activeSession = null
        }
        activeSession = session
        session.start(activity)
    }

    /** End the active session programmatically. */
    fun endSession() {
        activeSession?.dismiss()
        activeSession = null
    }

    // ── Session-minimised tracking ─────────────────────────────────────────────

    fun isSessionMinimised(id: String): Boolean = sessionMinimisedIds.contains(id)

    fun markSessionMinimised(id: String) { sessionMinimisedIds.add(id) }

    // ── Visual Inspector ───────────────────────────────────────────────────────

    /**
     * Handle a deep link URL. Call from your Activity's onNewIntent or onCreate.
     *
     * Supported scheme: `tooltiptour://inspect?session={id}&base={url}&mode=element`
     */
    fun handleDeepLink(uri: android.net.Uri) {
        if (uri.scheme != "tooltiptour") return
        if (uri.host != "inspect") return
        val sessionId = uri.getQueryParameter("session") ?: return
        val base      = uri.getQueryParameter("base")    ?: baseUrl
        val modeStr   = uri.getQueryParameter("mode")    ?: "element"
        val mode      = if (modeStr == "page") TTInspectorMode.PAGE else TTInspectorMode.ELEMENT
        scope.launch {
            delay(500) // let the activity settle
            startInspector(sessionId, base, mode)
        }
    }

    /** Launch the Visual Inspector overlay. */
    fun startInspector(sessionId: String, base: String, mode: TTInspectorMode = TTInspectorMode.ELEMENT) {
        if (activeInspector != null) return
        val activity = currentActivity ?: return
        val client   = TTNetworkClient(base)
        val inspector = TTInspector(sessionId, client, mode, scope)
        inspector.onEnd = {
            activeInspector = null
            _isInspectorActive.value = false
        }
        activeInspector = inspector
        _isInspectorActive.value = true
        inspector.start(activity)
    }
}
