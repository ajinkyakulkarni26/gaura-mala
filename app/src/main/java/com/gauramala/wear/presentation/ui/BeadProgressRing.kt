package com.gauramala.wear.presentation.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.gauramala.wear.presentation.theme.GauraAmberDark
import com.gauramala.wear.presentation.theme.GauraGold
import com.gauramala.wear.presentation.theme.GauraGoldLight
import com.gauramala.wear.presentation.theme.MilestoneCyan
import com.gauramala.wear.presentation.theme.SurfaceVariantDark
import kotlin.math.cos
import kotlin.math.sin

/**
 * Circular progress ring for round Wear OS displays.
 * Outer ring: 107 count beads plus the Meru/Guru bead, lit as the current round advances.
 * Milestones at beads 27, 54, and 81 use a distinct color.
 * Inner ring dots: Completed rounds out of daily goal (e.g. 16).
 */
@Composable
fun BeadProgressRing(
    beadCount: Int,
    completedRounds: Int,
    dailyGoal: Int,
    isAmbient: Boolean,
    modifier: Modifier = Modifier
) {
    val animatedProgress by animateFloatAsState(
        targetValue = (beadCount.toFloat() / 108f).coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = if (isAmbient) 0 else 180),
        label = "beadProgress"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        val strokeWidth = if (isAmbient) 1.dp.toPx() else 1.5.dp.toPx()
        val diameter = size.minDimension - 12.dp.toPx()
        val radius = diameter / 2f
        val center = Offset(size.width / 2f, size.height / 2f)

        // A faint guide keeps the bead spacing legible when the watch is dimmed.
        drawCircle(
            color = if (isAmbient) Color(0xFF222222) else SurfaceVariantDark,
            radius = radius,
            center = center,
            style = Stroke(width = strokeWidth)
        )

        // 108 individual beads replace the continuous arc so progress reads like a mala.
        for (index in 0 until 108) {
            val isMeru = index == 0
            val angleDeg = -90f + (index * 360f / 108f)
            val angleRad = Math.toRadians(angleDeg.toDouble())
            val beadCenter = Offset(
                x = (center.x + radius * cos(angleRad)).toFloat(),
                y = (center.y + radius * sin(angleRad)).toFloat()
            )
            val isReached = if (isMeru) {
                beadCount >= 107
            } else {
                animatedProgress >= (index / 108f)
            }
            val isMilestone = index == 27 || index == 54 || index == 81
            val beadColor = when {
                isAmbient && isReached -> Color.White
                isAmbient -> Color(0xFF454545)
                isMeru && isReached -> GauraGoldLight
                isMeru -> GauraAmberDark
                isReached && isMilestone -> MilestoneCyan
                isReached -> GauraGold
                isMilestone -> GauraAmberDark
                else -> Color(0x665F5547)
            }
            val beadRadius = when {
                isMeru -> 3.1.dp.toPx()
                isReached && isMilestone -> 2.8.dp.toPx()
                isReached -> 2.35.dp.toPx()
                isMilestone -> 2.2.dp.toPx()
                else -> 1.8.dp.toPx()
            }

            drawCircle(color = beadColor, radius = beadRadius, center = beadCenter)
            if (isReached && !isAmbient) {
                drawCircle(
                    color = Color.White.copy(alpha = 0.65f),
                    radius = beadRadius * 0.34f,
                    center = Offset(
                        beadCenter.x - beadRadius * 0.2f,
                        beadCenter.y - beadRadius * 0.2f
                    )
                )
            }
        }

        // Daily rounds are tracked on a smaller inner ring.
        if (!isAmbient && dailyGoal > 0) {
            val innerRadius = radius - 14.dp.toPx()
            val totalDots = dailyGoal.coerceAtMost(24) // up to 24 dots for visibility
            val completedFraction = (completedRounds.toFloat() / dailyGoal.toFloat()).coerceIn(0f, 1f)
            for (i in 0 until totalDots) {
                val dotFraction = i.toFloat() / totalDots.toFloat()
                val angleDeg = -90f + (dotFraction * 360f)
                val angleRad = Math.toRadians(angleDeg.toDouble())
                val dotPos = Offset(
                    x = (center.x + innerRadius * cos(angleRad)).toFloat(),
                    y = (center.y + innerRadius * sin(angleRad)).toFloat()
                )
                val isCompleted = completedFraction >= (i + 1).toFloat() / totalDots.toFloat()
                drawCircle(
                    color = if (isCompleted) GauraGold else Color(0x33666666),
                    radius = if (isCompleted) 2.2.dp.toPx() else 1.2.dp.toPx(),
                    center = dotPos
                )
            }
        }
    }
}
