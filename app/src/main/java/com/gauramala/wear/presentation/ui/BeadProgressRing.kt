package com.gauramala.wear.presentation.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.gauramala.wear.presentation.theme.GauraAmberDark
import com.gauramala.wear.presentation.theme.GauraGold
import com.gauramala.wear.presentation.theme.GauraGoldLight
import com.gauramala.wear.presentation.theme.GauraSaffron
import com.gauramala.wear.presentation.theme.MilestoneCyan
import com.gauramala.wear.presentation.theme.SurfaceVariantDark
import kotlin.math.cos
import kotlin.math.sin

/**
 * Circular progress ring for round Wear OS displays.
 * Outer arc: 108 beads of the current round.
 * Milestone markers: 27, 54, 81, and 108 (Meru/Guru bead).
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
        val strokeWidth = if (isAmbient) 3.dp.toPx() else 6.dp.toPx()
        val diameter = size.minDimension - strokeWidth - 8.dp.toPx()
        val radius = diameter / 2f
        val center = Offset(size.width / 2f, size.height / 2f)

        // 1. Background Track
        drawCircle(
            color = if (isAmbient) Color(0xFF222222) else SurfaceVariantDark,
            radius = radius,
            center = center,
            style = Stroke(width = strokeWidth)
        )

        // 2. Active Bead Progress Arc
        if (animatedProgress > 0f) {
            val sweepAngle = animatedProgress * 360f
            val arcBrush = if (isAmbient) {
                Brush.sweepGradient(listOf(Color.White, Color.White))
            } else {
                Brush.sweepGradient(
                    colors = listOf(
                        GauraSaffron,
                        GauraAmberDark,
                        GauraGold,
                        GauraGoldLight
                    ),
                    center = center
                )
            }

            drawArc(
                brush = arcBrush,
                startAngle = -90f,
                sweepAngle = sweepAngle,
                useCenter = false,
                topLeft = Offset(center.x - radius, center.y - radius),
                size = Size(diameter, diameter),
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
        }

        // 3. Milestone Markers at 27, 54, 81 beads
        if (!isAmbient) {
            val milestoneFractions = listOf(0.25f, 0.50f, 0.75f)
            milestoneFractions.forEach { fraction ->
                val angleDeg = -90f + (fraction * 360f)
                val angleRad = Math.toRadians(angleDeg.toDouble())
                val dotCenter = Offset(
                    x = (center.x + radius * cos(angleRad)).toFloat(),
                    y = (center.y + radius * sin(angleRad)).toFloat()
                )
                val isReached = animatedProgress >= fraction
                drawCircle(
                    color = if (isReached) MilestoneCyan else Color(0x66FFFFFF),
                    radius = 2.5.dp.toPx(),
                    center = dotCenter
                )
            }

            // Top Apex Marker (Bead 108 / Meru Bead indicator)
            val meruAngleRad = Math.toRadians(-90.0)
            val meruCenter = Offset(
                x = (center.x + radius * cos(meruAngleRad)).toFloat(),
                y = (center.y + radius * sin(meruAngleRad)).toFloat()
            )
            drawCircle(
                color = if (beadCount >= 107) GauraGoldLight else GauraAmberDark,
                radius = 4.dp.toPx(),
                center = meruCenter
            )
        }

        // 4. Daily Rounds Completed Indicator (Dots track inside the main ring)
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
