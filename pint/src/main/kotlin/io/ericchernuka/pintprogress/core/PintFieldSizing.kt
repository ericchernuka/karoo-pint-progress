package io.ericchernuka.pintprogress.core

import kotlin.math.roundToInt

/** Resolves Karoo physical field size into Android layout dp */
internal fun resolveFieldSize(
    viewSize: Pair<Int, Int>,
    gridSize: Pair<Int, Int>,
    screenSize: Pair<Int, Int>,
    density: Float,
): Pair<Int, Int> {
    val widthPx = viewSize.first.takeIf { it > 0 } ?: screenSize.first * gridSize.first / 60
    val heightPx = viewSize.second.takeIf { it > 0 } ?: screenSize.second * gridSize.second / 60
    val validDensity = density.takeIf { it.isFinite() && it > 0f } ?: 1f
    return (widthPx / validDensity).roundToInt() to (heightPx / validDensity).roundToInt()
}

/** Fit an exact Pints Fill count within the content rectangle reported by Karoo */
internal fun resolveFillTextSize(
    karooTextSizeSp: Int,
    fontScale: Float,
    contentWidthDp: Int,
    contentHeightDp: Int,
    measuredTextWidthPerTextSize: Float,
    lineHeightPerTextSize: Float,
): Float = minOf(
    karooTextSizeSp.takeIf { it > 0 }?.toFloat() ?: Float.MAX_VALUE,
    contentWidthDp.coerceAtLeast(1) / measuredTextWidthPerTextSize / fontScale,
    contentHeightDp.coerceAtLeast(1) / lineHeightPerTextSize / fontScale,
)
