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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lovelysomething.tooltiptour.models.TTStep
import com.lovelysomething.tooltiptour.models.TTStyles

/**
 * Step-by-step walkthrough card — mirrors iOS TTStepCardView exactly.
 *
 * Shows: step counter · title · body · progress dots · Prev/Next buttons.
 * An X close button is overlaid at the top-right.
 */
@Composable
fun TTStepCardView(
    step: TTStep,
    stepIndex: Int,
    totalSteps: Int,
    styles: TTStyles?,
    onNext: () -> Unit,
    onBack: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val cardBg     = styles?.resolvedCardBgColor  ?: Color.White
    val titleColor = styles?.resolvedTitleColor    ?: Color(0xFF0D0A1C.toInt())
    val bodyColor  = styles?.resolvedBodyColor     ?: Color(0xFF6B7280.toInt())
    val btnBg      = styles?.resolvedBtnBgColor    ?: Color(0xFF3730A3.toInt())
    val btnText    = styles?.resolvedBtnTextColor  ?: Color.White
    val cardRadius = (styles?.cardCornerRadius ?: 16f).dp
    val btnRadius  = (styles?.btnCornerRadius  ?: 8f).dp
    val isLast     = stepIndex == totalSteps - 1

    Box(modifier = modifier) {
        // ── Card ───────────────────────────────────────────────────────────────
        Column(
            modifier = Modifier
                .shadow(elevation = 16.dp, shape = RoundedCornerShape(cardRadius))
                .background(cardBg, RoundedCornerShape(cardRadius))
                .padding(20.dp)
                .fillMaxWidth(),
        ) {
            // Step counter
            Text(
                text       = "STEP ${stepIndex + 1} OF $totalSteps",
                fontSize   = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp,
                color      = btnBg,
                modifier   = Modifier.padding(bottom = 6.dp, end = 24.dp),
            )

            // Title
            Text(
                text       = step.title,
                fontSize   = 18.sp,
                fontWeight = FontWeight.Bold,
                color      = titleColor,
                modifier   = Modifier.padding(bottom = 6.dp),
            )

            // Body
            Text(
                text     = step.content,
                fontSize = 14.sp,
                color    = bodyColor,
                modifier = Modifier.padding(bottom = 16.dp),
            )

            // Progress dots
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(bottom = 12.dp),
            ) {
                repeat(totalSteps) { i ->
                    if (i == stepIndex) {
                        Box(
                            modifier = Modifier
                                .size(width = 14.dp, height = 6.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(btnBg),
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFD1D5DB.toInt())),
                        )
                    }
                }
            }

            // Navigation row
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                // ← Prev (ghost — only shown when not on first step)
                if (stepIndex > 0) {
                    TextButton(
                        onClick  = onBack,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(
                            "← Prev",
                            color      = btnBg,
                            fontSize   = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }

                // Next → / Finish ✓ (filled)
                Button(
                    onClick  = onNext,
                    shape    = RoundedCornerShape(btnRadius),
                    colors   = ButtonDefaults.buttonColors(containerColor = btnBg, contentColor = btnText),
                    modifier = Modifier.weight(1f),
                ) {
                    Text(
                        if (isLast) "Finish ✓" else "Next →",
                        fontWeight = FontWeight.Bold,
                        fontSize   = 14.sp,
                    )
                }
            }
        }

        // ── X close button (top-right overlay) ────────────────────────────────
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 14.dp, end = 12.dp)
                .size(24.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss,
                ),
        ) {
            Text("✕", color = Color(0xFF9CA3B0.toInt()), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}
