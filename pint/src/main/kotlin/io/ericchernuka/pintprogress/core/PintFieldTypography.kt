package io.ericchernuka.pintprogress.core
import kotlin.math.roundToInt

class PintFieldTypography(val countTextSizeSp: Float, val suffixTextSizeSp: Float,
) { companion object { private const val REGULAR_MAX_COUNT_TEXT_SIZE_SP = 110
        private const val COMPACT_MAX_COUNT_TEXT_SIZE_SP = 68
        private const val SUFFIX_TO_COUNT_RATIO = 52f / 110f
        private const val DEFAULT_FONT_SCALE = 1f
        private const val REGULAR_MUG_AND_MARGIN_WIDTH_DP = 68f
        private const val COMPACT_MUG_AND_MARGIN_WIDTH_DP = 49f
        private const val DIGIT_WIDTH_PER_TEXT_SIZE = 0.35f
        private const val SUFFIX_WIDTH_PER_TEXT_SIZE = 0.16f
        fun forLayout(layout: PintFieldLayout, karooTextSizeSp: Int, countLength: Int, fontScale: Float, contentWidthDp: Int, ): PintFieldTypography? { val (maxCountTextSizeSp, fixedWidthDp) = when (layout) { PintFieldLayout.REGULAR -> REGULAR_MAX_COUNT_TEXT_SIZE_SP to REGULAR_MUG_AND_MARGIN_WIDTH_DP
                PintFieldLayout.COMPACT -> COMPACT_MAX_COUNT_TEXT_SIZE_SP to COMPACT_MUG_AND_MARGIN_WIDTH_DP
                PintFieldLayout.PICKER -> return null
                PintFieldLayout.ICON_ONLY -> return null }
            val validFontScale = fontScale.takeIf { it.isFinite() && it > 0f } ?: DEFAULT_FONT_SCALE
            val heightCapSp = maxCountTextSizeSp / validFontScale
            val safeCountLength = countLength.coerceAtLeast(1)
            val textWidthDp = (contentWidthDp - fixedWidthDp).coerceAtLeast(1f)
            val widthPerTextSize = (safeCountLength * DIGIT_WIDTH_PER_TEXT_SIZE + SUFFIX_WIDTH_PER_TEXT_SIZE) * validFontScale
            val widthCapSp = textWidthDp / widthPerTextSize
            val countTextSizeSp = karooTextSizeSp
                .takeIf { it > 0 }
                ?.toFloat()
                ?.coerceAtMost(minOf(heightCapSp, widthCapSp))
                ?: minOf(heightCapSp, widthCapSp)
            return PintFieldTypography(countTextSizeSp = countTextSizeSp, suffixTextSizeSp = (countTextSizeSp * SUFFIX_TO_COUNT_RATIO).roundToInt().toFloat(), ) } } }
