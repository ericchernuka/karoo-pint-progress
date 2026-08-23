package io.ericchernuka.pintprogress.core

/**
 * Chooses a complete, readable visual treatment for the size supplied by Karoo.
 *
 * Karoo reports physical pixels while the layouts below use dp. The caller converts units before
 * selection. Each threshold is a content-fit constraint derived from the fixed mug dimensions,
 * two practical count digits, the suffix, and Karoo's configured boundary inset. A treatment is
 * selected only when both its width and height fit; otherwise the next smaller treatment is used.
 */
enum class PintFieldLayout(val showsCount: Boolean) {
    PICKER(false),
    REGULAR(true),
    COMPACT(true),
    ICON_ONLY(false),
    ;

    companion object {
        private const val REGULAR_MIN_WIDTH_DP = 180
        private const val REGULAR_MIN_HEIGHT_DP = 89
        private const val COMPACT_MIN_WIDTH_DP = 108
        private const val COMPACT_MIN_HEIGHT_DP = 65

        fun forSize(
            preview: Boolean,
            widthDp: Int,
            heightDp: Int,
            boundariesEnabled: Boolean,
        ): PintFieldLayout {
            if (preview) return PICKER

            val edgeInsetsDp = PintFieldChrome.edgeInsetDp(boundariesEnabled) * 2
            val usableWidthDp = widthDp - edgeInsetsDp
            val usableHeightDp = heightDp - edgeInsetsDp

            return when {
                usableWidthDp >= REGULAR_MIN_WIDTH_DP && usableHeightDp >= REGULAR_MIN_HEIGHT_DP -> REGULAR
                usableWidthDp >= COMPACT_MIN_WIDTH_DP && usableHeightDp >= COMPACT_MIN_HEIGHT_DP -> COMPACT
                else -> ICON_ONLY
            }
        }
    }
}
