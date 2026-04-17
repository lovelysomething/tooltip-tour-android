package com.lovelysomething.tooltiptour.networking

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

enum class TTEventType(val value: String) {
    GUIDE_SHOWN("guide_shown"),
    STEP_COMPLETED("step_completed"),
    GUIDE_COMPLETED("guide_completed"),
    GUIDE_DISMISSED("guide_dismissed"),
}

/** Fire-and-forget event tracker — mirrors iOS TTEventTracker exactly. */
internal class TTEventTracker(
    private val baseUrl: String,
    private val scope: CoroutineScope,
) {
    private val sessionId = UUID.randomUUID().toString()

    fun track(
        event: TTEventType,
        walkthroughId: String,
        siteKey: String,
        stepIndex: Int? = null,
    ) {
        scope.launch(Dispatchers.IO) {
            try {
                val body = buildString {
                    append("""{"walkthroughId":"$walkthroughId","siteKey":"$siteKey",""")
                    append(""""eventType":"${event.value}","sessionId":"$sessionId"""")
                    if (stepIndex != null) append(""","stepIndex":$stepIndex""")
                    append("}")
                }
                val conn = URL("$baseUrl/api/events").openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.doOutput      = true
                conn.setRequestProperty("Content-Type", "application/json")
                conn.connectTimeout = 8_000
                conn.readTimeout    = 8_000
                conn.outputStream.write(body.toByteArray())
                conn.responseCode // trigger the request
                conn.disconnect()
            } catch (_: Exception) { /* fire and forget */ }
        }
    }
}
