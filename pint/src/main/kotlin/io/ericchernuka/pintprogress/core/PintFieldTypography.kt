package io.ericchernuka.pintprogress.core

import kotlin.math.roundToInt

/** Karoo's numeric size, constrained so the condensed count never outgrows its paired mug. */
data class PintFieldTypography(
    val countTextSizeSp: Float,
    val suffixTextSizeSp: Float,
) {
    companion object {
        private const val REGULAR_MAX_COUNT_TEXT_SIZE_SP = 110
        private const val COMPACT_MAX_COUNT_TEXT_SIZE_SP = 68
        private const val SUFFIX_TO_COUNT_RATIO = 52f / 110f

        fun forLayout(layout: PintFieldLayout, karooTextSizeSp: Int): PintFieldTypography? {
            val maxCountTextSizeSp = when (layout) {
                PintFieldLayout.REGULAR -> REGULAR_MAX_COUNT_TEXT_SIZE_SP
                PintFieldLayout.COMPACT -> COMPACT_MAX_COUNT_TEXT_SIZE_SP
                PintFieldLayout.PICKER -> return null
                PintFieldLayout.ICON_ONLY -> return null
            }
            val countTextSizeSp = karooTextSizeSp
                .takeIf { it > 0 }
                ?.coerceAtMost(maxCountTextSizeSp)
                ?: maxCountTextSizeSp

            return PintFieldTypography(
                countTextSizeSp = countTextSizeSp.toFloat(),
                suffixTextSizeSp = (countTextSizeSp * SUFFIX_TO_COUNT_RATIO).roundToInt().toFloat(),
            )
        }
    }
}
