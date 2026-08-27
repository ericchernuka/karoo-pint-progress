package io.ericchernuka.pintprogress.core

import kotlin.math.roundToInt

private const val SUFFIX_TO_COUNT_RATIO = 52f / 110f

// DIGIT_WIDTH_PER_TEXT_SIZE and SUFFIX_WIDTH_PER_TEXT_SIZE exceed measured font advances so
// Android rounding and system-font revisions cannot make boundary fitting optimistic
private const val DIGIT_WIDTH_PER_TEXT_SIZE = 0.35f
private const val SUFFIX_WIDTH_PER_TEXT_SIZE = 0.16f

/** Insets artwork so Karoo-owned field boundaries remain visible and separate from the mug */
internal fun edgeInsetDp(boundariesEnabled: Boolean) =
    if (boundariesEnabled) 6 else 2

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

/** Resolves Karoo numeric size so the condensed count never outgrows its paired mug */
internal fun resolveTypography(
    layout: PintFieldLayout,
    karooTextSizeSp: Int,
    countLength: Int,
    fontScale: Float,
    contentWidthDp: Int,
): Pair<Float, Float>? {
    val (maxCountTextSizeSp, fixedWidthDp) = when (layout) {
        PintFieldLayout.REGULAR -> 110f to 68f
        PintFieldLayout.COMPACT -> 68f to 49f
        PintFieldLayout.PICKER -> return null
        PintFieldLayout.ICON_ONLY -> return null
    }
    val validFontScale = fontScale.takeIf { it.isFinite() && it > 0f } ?: 1f
    val heightCapSp = maxCountTextSizeSp / validFontScale
    val safeCountLength = countLength.coerceAtLeast(1)
    val textWidthDp = (contentWidthDp - fixedWidthDp).coerceAtLeast(1f)
    val widthPerTextSize = (
        safeCountLength * DIGIT_WIDTH_PER_TEXT_SIZE + SUFFIX_WIDTH_PER_TEXT_SIZE
        ) * validFontScale
    val widthCapSp = textWidthDp / widthPerTextSize
    val requestedTextSizeSp = karooTextSizeSp.takeIf { it > 0 }?.toFloat() ?: Float.MAX_VALUE
    val countTextSizeSp = minOf(requestedTextSizeSp, heightCapSp, widthCapSp)
    return countTextSizeSp to (countTextSizeSp * SUFFIX_TO_COUNT_RATIO).roundToInt().toFloat()
}

/**
 * Chooses a complete, readable visual treatment for the size supplied by Karoo
 *
 * Karoo reports physical pixels while the layouts below use dp. The caller converts units before
 * selection. Each threshold is a content-fit constraint derived from the fixed mug dimensions,
 * two practical count digits, the suffix, and Karoo's configured boundary inset. A treatment is
 * selected only when both its width and height fit; otherwise the next smaller treatment is used
 */
enum class PintFieldLayout(private val showsCount: Boolean) {
    PICKER(false),
    REGULAR(true),
    COMPACT(true),
    ICON_ONLY(false),
    ;

    /**
     * Applies the current presentation state after the viewport treatment has been selected
     *
     * Preview owns its representative artwork. Live content without a completed count uses the
     * adaptive mug-only treatment instead of retaining an empty fixed-size counter layout
     */
    fun forDisplay(hasCompletedCount: Boolean): PintFieldLayout = when {
        this == PICKER -> PICKER
        !hasCompletedCount -> ICON_ONLY
        else -> this
    }

    fun visibleCount(count: String): String = count.takeIf { showsCount }.orEmpty()

    companion object {
        fun forSize(
            preview: Boolean,
            widthDp: Int,
            heightDp: Int,
            boundariesEnabled: Boolean,
        ): PintFieldLayout {
            if (preview) return PICKER

            val edgeInsetsDp = edgeInsetDp(boundariesEnabled) * 2
            val usableWidthDp = widthDp - edgeInsetsDp
            val usableHeightDp = heightDp - edgeInsetsDp

            return when {
                usableWidthDp >= 180 && usableHeightDp >= 89 -> REGULAR
                usableWidthDp >= 108 && usableHeightDp >= 65 -> COMPACT
                else -> ICON_ONLY
            }
        }
    }
}
