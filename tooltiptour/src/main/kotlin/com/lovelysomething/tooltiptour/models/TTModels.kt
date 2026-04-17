package com.lovelysomething.tooltiptour.models

import androidx.compose.ui.graphics.Color
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ── API response models ────────────────────────────────────────────────────────

@Serializable
data class TTConfig(
    val id: String,
    @SerialName("page_pattern") val pagePattern: String? = null,
    @SerialName("fab_label") val fabLabel: String? = null,
    @SerialName("welcome_emoji") val welcomeEmoji: String? = null,
    @SerialName("welcome_title") val welcomeTitle: String? = null,
    @SerialName("welcome_message") val welcomeMessage: String? = null,
    @SerialName("auto_open") val autoOpen: Boolean = false,
    @SerialName("start_minimized") val startMinimized: Boolean = false,
    @SerialName("max_shows") val maxShows: Int? = null,
    val steps: List<TTStep> = emptyList(),
    val styles: TTStyles? = null,
)

@Serializable
data class TTStep(
    val title: String,
    val content: String,
    val selector: String,
)

// ── Styles ─────────────────────────────────────────────────────────────────────

@Serializable
data class TTStyles(
    val fab: TTFabStyle? = null,
    val card: TTCardStyle? = null,
    val type: TTTypeStyle? = null,
    val btn: TTBtnStyle? = null,
    val beacon: TTBeaconStyle? = null,
) {
    // FAB
    val resolvedFabBgColor: Color get() = parseColor(fab?.bgColor) ?: Color(0xFF3730A3.toInt())
    val fabCornerRadius: Float get() = (fab?.borderRadius ?: 24.0).toFloat()

    // Card
    val resolvedCardBgColor: Color get() = parseColor(card?.bgColor) ?: Color.White
    val cardCornerRadius: Float get() = (card?.borderRadius ?: 14.0).toFloat()

    // Text
    val resolvedTitleColor: Color get() = parseColor(type?.titleColor) ?: Color(0xFF0D0A1C.toInt())
    val resolvedBodyColor: Color get() = parseColor(type?.bodyColor) ?: Color(0xFF6B7280.toInt())

    // Button
    val resolvedBtnBgColor: Color get() = parseColor(btn?.bgColor) ?: Color(0xFF3730A3.toInt())
    val resolvedBtnTextColor: Color get() = parseColor(btn?.textColor) ?: Color.White
    val btnCornerRadius: Float get() = (btn?.borderRadius ?: 8.0).toFloat()

    // Beacon — falls back to btn color, then indigo
    val resolvedBeaconBgColor: Color
        get() = parseColor(beacon?.bgColor ?: btn?.bgColor) ?: Color(0xFF3730A3.toInt())
    val resolvedBeaconTextColor: Color
        get() = parseColor(beacon?.textColor) ?: Color.White
}

@Serializable
data class TTFabStyle(
    @SerialName("bg_color") val bgColor: String? = null,
    @SerialName("border_radius") val borderRadius: Double? = null,
    val icon: String? = null,
    /** "left" or "right" */
    val position: String? = null,
    @SerialName("bottom_offset") val bottomOffset: Double? = null,
    val size: Double? = null,
)

@Serializable
data class TTCardStyle(
    @SerialName("bg_color") val bgColor: String? = null,
    @SerialName("border_radius") val borderRadius: Double? = null,
)

@Serializable
data class TTTypeStyle(
    @SerialName("title_color") val titleColor: String? = null,
    @SerialName("body_color") val bodyColor: String? = null,
)

@Serializable
data class TTBtnStyle(
    @SerialName("bg_color") val bgColor: String? = null,
    @SerialName("text_color") val textColor: String? = null,
    @SerialName("border_radius") val borderRadius: Double? = null,
)

@Serializable
data class TTBeaconStyle(
    val style: String? = null,
    @SerialName("bg_color") val bgColor: String? = null,
    @SerialName("text_color") val textColor: String? = null,
)

// ── Color parsing ─────────────────────────────────────────────────────────────

/**
 * Parses "#RRGGBB", "rgb(r,g,b)", or "rgba(r,g,b,a)" to a Compose Color.
 * Returns null if the string is null, blank, or unparseable.
 */
fun parseColor(hex: String?): Color? {
    if (hex.isNullOrBlank()) return null
    val s = hex.trim()
    return try {
        if (s.startsWith("rgb", ignoreCase = true)) {
            val nums = s.dropWhile { it != '(' }.drop(1).takeWhile { it != ')' }
                .split(",").mapNotNull { it.trim().toDoubleOrNull() }
            if (nums.size < 3) return null
            Color(
                red   = (nums[0] / 255f).toFloat(),
                green = (nums[1] / 255f).toFloat(),
                blue  = (nums[2] / 255f).toFloat(),
                alpha = if (nums.size >= 4) nums[3].toFloat() else 1f,
            )
        } else {
            val h = if (s.startsWith("#")) s else "#$s"
            Color(android.graphics.Color.parseColor(h))
        }
    } catch (_: Exception) {
        null
    }
}
