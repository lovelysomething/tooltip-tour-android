package com.lovelysomething.tooltiptour.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lovelysomething.tooltiptour.models.TTConfig
import com.lovelysomething.tooltiptour.models.TTStyles

/**
 * The initial launch card shown at the bottom of the screen.
 *
 * Layout mirrors iOS TTWelcomeCardView — white card + gap + X dismiss circle below.
 */
@Composable
fun TTWelcomeCardView(
    config: TTConfig,
    onStart: () -> Unit,
    onDismiss: () -> Unit,
    onDontShowAgain: () -> Unit,
) {
    val styles = config.styles

    val cardBg      = styles?.resolvedCardBgColor     ?: Color.White
    val titleColor  = styles?.resolvedTitleColor       ?: Color(0xFF0D0A1C.toInt())
    val bodyColor   = styles?.resolvedBodyColor        ?: Color(0xFF6B7280.toInt())
    val btnBg       = styles?.resolvedBtnBgColor       ?: Color(0xFF3730A3.toInt())
    val btnText     = styles?.resolvedBtnTextColor     ?: Color.White
    val cardRadius  = (styles?.cardCornerRadius ?: 16f).dp
    val btnRadius   = (styles?.btnCornerRadius  ?: 8f).dp

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(horizontal = 20.dp),
    ) {
        // ── White card ─────────────────────────────────────────────────────────
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .shadow(elevation = 12.dp, shape = RoundedCornerShape(cardRadius))
                .background(cardBg, RoundedCornerShape(cardRadius))
                .padding(horizontal = 20.dp, vertical = 20.dp)
                .fillMaxWidth(),
        ) {
            // Emoji
            if (!config.welcomeEmoji.isNullOrBlank()) {
                Text(
                    text  = config.welcomeEmoji,
                    fontSize = 40.sp,
                    modifier = Modifier.padding(bottom = 12.dp),
                )
            }

            // Title
            if (!config.welcomeTitle.isNullOrBlank()) {
                Text(
                    text       = config.welcomeTitle,
                    fontSize   = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color      = titleColor,
                    textAlign  = TextAlign.Center,
                    modifier   = Modifier.padding(bottom = 8.dp),
                )
            }

            // Body
            if (!config.welcomeMessage.isNullOrBlank()) {
                Text(
                    text      = config.welcomeMessage,
                    fontSize  = 14.sp,
                    color     = bodyColor,
                    textAlign = TextAlign.Center,
                    modifier  = Modifier.padding(bottom = 20.dp),
                )
            }

            // CTA button
            Button(
                onClick  = onStart,
                shape    = RoundedCornerShape(btnRadius),
                colors   = ButtonDefaults.buttonColors(containerColor = btnBg, contentColor = btnText),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 14.dp),
            ) {
                Text("Yes, show me around!", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }

            // Don't show again
            TextButton(onClick = onDontShowAgain) {
                Text(
                    text     = "Don't show again",
                    fontSize = 14.sp,
                    color    = Color(0xFF9CA3B0.toInt()),
                )
            }
        }

        // ── Gap ────────────────────────────────────────────────────────────────
        Spacer(modifier = Modifier.height(16.dp))

        // ── X dismiss circle (below the card, centred) ─────────────────────────
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.35f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss,
                ),
        ) {
            Text("✕", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(20.dp))
    }
}
