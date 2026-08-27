package io.ericchernuka.pintprogress.core

import kotlin.math.roundToInt

private const val TEXT_VERTICAL_GAP_DP = 4
private const val REGULAR_GLYPH_CLEARANCE_DP = 8
internal const val COUNT_START_PADDING_DP = 6
internal const val COUNT_END_PADDING_DP = 2

// DIGIT_WIDTH_PER_TEXT_SIZE and SUFFIX_WIDTH_PER_TEXT_SIZE exceed measured font advances so
// Android rounding and system-font revisions cannot make boundary fitting optimistic
private const val DIGIT_WIDTH_PER_TEXT_SIZE = 0.35f
private const val SUFFIX_WIDTH_PER_SUFFIX_TEXT_SIZE = 0.34f

/** Insets artwork so Karoo-owned field boundaries remain visible and separate from the mug */
internal fun edgeInsetDp(boundariesEnabled: Boolean) =
    if (boundariesEnabled) 6 else 2

/** Offsets visible glyph bounds to the optical center of the font line */
internal fun opticalCenterOffset(
    lineTop: Float,
    lineBottom: Float,
    glyphTop: Float,
    glyphBottom: Float,
): Float = (lineTop + lineBottom - glyphTop - glyphBottom) / 2f

/** Applies a measured visual-center offset plus a small raster correction */
internal fun opticalTranslationY(offsetPx: Float, upwardBiasPx: Float): Float =
    offsetPx - upwardBiasPx

internal fun PintFieldLayout.countRasterUpwardBiasDp(countTextSizeSp: Float): Float = when {
    this == PintFieldLayout.COMPACT -> 4f
    this == PintFieldLayout.REGULAR && countTextSizeSp <= nominalCountTextSizeSp -> 8f
    else -> 0f
}

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
    contentHeightDp: Int = Int.MAX_VALUE,
    lineHeightPerTextSize: Float = layout.nominalMugHeightDp / layout.nominalCountTextSizeSp,
    measuredTextWidthPerCountSize: Float = Float.NaN,
    maximumScale: Float = layout.maxScale,
): Pair<Float, Float>? {
    if (!layout.showsCount) return null
    val validFontScale = fontScale.takeIf { it.isFinite() && it > 0f } ?: 1f
    val validLineHeightPerTextSize = lineHeightPerTextSize.takeIf { it.isFinite() && it > 0f }
        ?: layout.nominalMugHeightDp / layout.nominalCountTextSizeSp
    val pairedHeightPerTextSize = minOf(
        validLineHeightPerTextSize,
        layout.nominalMugHeightDp / layout.nominalCountTextSizeSp,
    )
    val heightPerTextSize = if (layout == PintFieldLayout.REGULAR) {
        maxOf(1f, pairedHeightPerTextSize)
    } else {
        pairedHeightPerTextSize
    }
    val validMaximumScale = maximumScale.takeIf { it.isFinite() && it > 0f }
        ?.coerceAtMost(layout.maxScale)
        ?: layout.maxScale
    val roomyScale = if (
        contentHeightDp != Int.MAX_VALUE &&
        contentHeightDp >= layout.nominalMugHeightDp * validMaximumScale +
        layout.hostLabelReserveDp + TEXT_VERTICAL_GAP_DP
    ) validMaximumScale else 1f
    val nominalCapSp = layout.nominalCountTextSizeSp * roomyScale / validFontScale
    val glyphClearanceDp = if (layout == PintFieldLayout.REGULAR) {
        REGULAR_GLYPH_CLEARANCE_DP
    } else {
        0
    }
    val heightCapSp = (
        contentHeightDp - layout.hostLabelReserveDp - TEXT_VERTICAL_GAP_DP - glyphClearanceDp
        )
        .coerceAtLeast(1) / heightPerTextSize / validFontScale
    val safeCountLength = countLength.coerceAtLeast(1)
    val textWidthPerCountSize = measuredTextWidthPerCountSize
        .takeIf { it.isFinite() && it > 0f }
        ?: (safeCountLength * DIGIT_WIDTH_PER_TEXT_SIZE
            + layout.suffixToCountRatio * SUFFIX_WIDTH_PER_SUFFIX_TEXT_SIZE)
    val scalableWidthPerTextSize = (
        textWidthPerCountSize
            + layout.nominalMugWidthDp / layout.nominalCountTextSizeSp
        ) * validFontScale
    val scalableWidthDp = (
        contentWidthDp - layout.itemGapDp * 2 - COUNT_START_PADDING_DP - COUNT_END_PADDING_DP
        ).coerceAtLeast(1f)
    val widthCapSp = scalableWidthDp / scalableWidthPerTextSize
    val requestedTextSizeSp = karooTextSizeSp.takeIf { it > 0 }
        ?.let { it * roomyScale }
        ?: Float.MAX_VALUE
    val countTextSizeSp = minOf(requestedTextSizeSp, nominalCapSp, heightCapSp, widthCapSp)
    return countTextSizeSp to (countTextSizeSp * layout.suffixToCountRatio).roundToInt().toFloat()
}

/**
 * Chooses a complete, readable visual treatment for the size supplied by Karoo
 *
 * Karoo reports physical pixels while the layouts below use dp. The caller converts units before
 * selection. Each threshold is a content-fit constraint derived from the fixed mug dimensions,
 * two practical count digits, the suffix, and Karoo's configured boundary inset. Regular is used
 * when it fits. Compact is the minimum count-and-mug treatment and scales into smaller viewports.
 */
enum class PintFieldLayout(
    internal val showsCount: Boolean,
    internal val nominalCountTextSizeSp: Float = 0f,
    internal val suffixToCountRatio: Float = 0f,
    internal val nominalMugWidthDp: Float = 0f,
    internal val nominalMugHeightDp: Float = 0f,
    internal val itemGapDp: Float = 0f,
    internal val hostLabelReserveDp: Int = 0,
    internal val maxScale: Float = 1f,
) {
    PICKER(false),
    REGULAR(true, 110f, 52f / 110f, 66f, 89f, 2f, 24, 2f),
    COMPACT(true, 68f, 52f / 68f, 48f, 65f, 2f, 37),
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

    fun mugScaleFor(countTextSizeSp: Float, fontScale: Float): Float? {
        if (!showsCount) return null
        val validFontScale = fontScale.takeIf { it.isFinite() && it > 0f } ?: 1f
        return (countTextSizeSp * validFontScale / nominalCountTextSizeSp).coerceAtMost(maxScale)
    }

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
                else -> COMPACT
            }
        }
    }
}

internal fun previewLayoutFor(
    hasCompletedCount: Boolean,
    widthDp: Int,
    heightDp: Int,
    boundariesEnabled: Boolean,
): PintFieldLayout = if (hasCompletedCount) {
    PintFieldLayout.forSize(
        preview = false,
        widthDp = widthDp,
        heightDp = heightDp,
        boundariesEnabled = boundariesEnabled,
    )
} else {
    PintFieldLayout.PICKER
}
