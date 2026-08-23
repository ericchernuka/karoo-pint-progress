package io.ericchernuka.pintprogress.core

/**
 * Chooses a complete, readable visual treatment for the size supplied by Karoo.
 *
 * Karoo reports the allocated field size in physical pixels; callers convert it to dp first. The
 * thresholds below are calibrated for the 480 x 800 Karoo 3 grid: a 60 x 15 field is about
 * 480 x 200 dp, and a 30 x 15 field is about 240 x 200 dp. The narrow treatments deliberately
 * preserve the current mug state instead of allowing Android to crop a larger RemoteViews layout.
 */
enum class PintFieldLayout(val showsCount: Boolean) {
    PICKER(false),
    REGULAR(true),
    COMPACT(true),
    ICON_ONLY(false),
    ;

    companion object {
        private const val ICON_ONLY_MAX_WIDTH_DP = 180
        private const val ICON_ONLY_MAX_HEIGHT_DP = 120
        private const val COMPACT_MAX_WIDTH_DP = 320
        private const val COMPACT_MAX_HEIGHT_DP = 180

        fun forSize(preview: Boolean, widthDp: Int, heightDp: Int): PintFieldLayout = when {
            preview -> PICKER
            widthDp <= ICON_ONLY_MAX_WIDTH_DP || heightDp <= ICON_ONLY_MAX_HEIGHT_DP -> ICON_ONLY
            widthDp <= COMPACT_MAX_WIDTH_DP || heightDp <= COMPACT_MAX_HEIGHT_DP -> COMPACT
            else -> REGULAR
        }
    }
}
