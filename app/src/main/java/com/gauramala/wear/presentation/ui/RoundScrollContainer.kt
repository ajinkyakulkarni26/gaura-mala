package com.gauramala.wear.presentation.ui

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.findRootCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.hierarchicalFocusGroup
import androidx.wear.compose.foundation.requestFocusOnHierarchyActive
import androidx.wear.compose.foundation.rotary.RotaryScrollableDefaults
import androidx.wear.compose.foundation.rotary.rotaryScrollable
import androidx.wear.compose.material3.ScrollIndicator
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/**
 * Scrollable container for round Wear OS displays that preserves continuous
 * [Column] + [verticalScroll] + [rotaryScrollable] scrolling while rendering a
 * curved [ScrollIndicator] whenever the content is scrollable.
 */
@Composable
fun RoundScrollContainer(
    scrollState: ScrollState,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester = remember { FocusRequester() },
    contentPadding: PaddingValues = PaddingValues(
        start = 14.dp,
        end = 14.dp,
        top = 26.dp,
        bottom = 42.dp
    ),
    verticalSpacing: Dp = 4.dp,
    verticalArrangement: Arrangement.Vertical = Arrangement.spacedBy(verticalSpacing),
    scrollIndicatorTag: String = "scroll-indicator",
    content: @Composable ColumnScope.() -> Unit
) {
    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .hierarchicalFocusGroup(active = true)
                .requestFocusOnHierarchyActive()
                .rotaryScrollable(
                    behavior = RotaryScrollableDefaults.behavior(scrollableState = scrollState),
                    focusRequester = focusRequester
                )
                .verticalScroll(scrollState)
                .padding(contentPadding),
            verticalArrangement = verticalArrangement,
            horizontalAlignment = Alignment.CenterHorizontally,
            content = content
        )

        if (scrollState.maxValue > 0) {
            ScrollIndicator(
                state = scrollState,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 2.dp)
                    .testTag(scrollIndicatorTag)
            )
        }
    }
}

/**
 * Visually scales and fades an item inside a scrollable [Column] as it approaches
 * the top or bottom bezel of a circular watch screen so controls and text never
 * extend outside the physical round display area, without altering layout height
 * or semantic touch target bounds.
 */
fun Modifier.roundScrollItem(): Modifier = composed {
    var drawScale by remember { mutableFloatStateOf(1f) }
    var drawAlpha by remember { mutableFloatStateOf(1f) }
    var pivotYFraction by remember { mutableFloatStateOf(0.5f) }

    this
        .onGloballyPositioned { coordinates ->
            if (!coordinates.isAttached) return@onGloballyPositioned
            val root = coordinates.findRootCoordinates()
            val rootWidth = root.size.width.toFloat()
            val rootHeight = root.size.height.toFloat()
            val itemWidth = coordinates.size.width.toFloat()
            val itemHeight = coordinates.size.height.toFloat()
            if (rootWidth <= 0f || rootHeight <= 0f || itemWidth <= 0f || itemHeight <= 0f) {
                drawScale = 1f
                drawAlpha = 1f
                pivotYFraction = 0.5f
                return@onGloballyPositioned
            }

            val pos = coordinates.positionInWindow()
            val top = pos.y
            val bottom = top + itemHeight
            val itemCenterY = (top + bottom) * 0.5f
            val screenCenterY = rootHeight * 0.5f
            val radius = min(rootWidth, rootHeight) * 0.5f

            val centerNormDist = abs(itemCenterY - screenCenterY) / radius
            if (centerNormDist <= 0.16f && top >= 0.08f * rootHeight && bottom <= 0.92f * rootHeight) {
                drawScale = 1f
                drawAlpha = 1f
                pivotYFraction = 0.5f
                return@onGloballyPositioned
            }

            val isTopHalf = itemCenterY < screenCenterY
            pivotYFraction = if (isTopHalf) 1f else 0f

            // 1. Vertical containment so the outer edge never touches y = 0 or y = rootHeight.
            val verticalScale = if (isTopHalf) {
                val safeTop = 0.045f * rootHeight
                if (bottom <= safeTop) {
                    0f
                } else {
                    ((bottom - safeTop) / itemHeight).coerceIn(0f, 1f)
                }
            } else {
                val safeBottom = 0.955f * rootHeight
                if (top >= safeBottom) {
                    0f
                } else {
                    ((safeBottom - top) / itemHeight).coerceIn(0f, 1f)
                }
            }

            if (verticalScale <= 0.28f) {
                drawScale = 0.35f
                drawAlpha = 0f
                return@onGloballyPositioned
            }

            // 2. Horizontal circular chord containment at the scaled vertical position.
            val clampedVertScale = verticalScale.coerceIn(0.35f, 1f)
            val outerCheckY = if (isTopHalf) {
                bottom - 0.78f * clampedVertScale * itemHeight
            } else {
                top + 0.78f * clampedVertScale * itemHeight
            }
            val midCheckY = if (isTopHalf) {
                bottom - 0.50f * clampedVertScale * itemHeight
            } else {
                top + 0.50f * clampedVertScale * itemHeight
            }
            val maxDistFromCenter = max(
                abs(outerCheckY - screenCenterY),
                abs(midCheckY - screenCenterY)
            )
            val normDist = (maxDistFromCenter / radius).coerceIn(0f, 0.96f)
            val chordWidth = 2f * radius * sqrt(max(0f, 1f - normDist * normDist))
            val horizontalMargin = 0.11f * rootWidth
            val maxAllowedWidth = (chordWidth - horizontalMargin).coerceAtLeast(0.28f * rootWidth)
            val chordScale = (maxAllowedWidth / itemWidth).coerceIn(0.35f, 1f)

            val combinedScale = min(clampedVertScale, chordScale)
            drawScale = combinedScale
            drawAlpha = ((combinedScale - 0.28f) / 0.18f).coerceIn(0f, 1f)
        }
        .graphicsLayer {
            alpha = drawAlpha
        }
        .drawWithContent {
            if (drawAlpha <= 0.02f) {
                return@drawWithContent
            }
            val s = drawScale
            if (s >= 0.995f) {
                drawContent()
            } else {
                val pivotY = size.height * pivotYFraction
                withTransform({
                    scale(
                        scaleX = s,
                        scaleY = s,
                        pivot = Offset(size.width * 0.5f, pivotY)
                    )
                }) {
                    this@drawWithContent.drawContent()
                }
            }
        }
}
