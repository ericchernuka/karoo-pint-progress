package io.ericchernuka.pintprogress.core

/**
 * Chooses a complete, readable visual treatment for the size supplied by Karoo.
 *
 * Karoo reports the allocated field size in physical pixels. The thresholds below are calibrated
 * for the 480 x 800 Karoo 3 grid: a 60 x 15 field is about 480 x 200 px, and a 30 x 15 field is
 * about 240 x 200 px. The narrow treatments deliberately preserve the current mug state instead
 * of allowing Android to crop a larger RemoteViews layout.
 */
enum class PintFieldLayout(val showsCount: Boolean) {
    PICKER(false),
    REGULAR(true),
    COMPACT(true),
    ICON_ONLY(false),
    ;

    companion object {
        private const val ICON_ONLY_MAX_WIDTH_PX = 180
        private const val ICON_ONLY_MAX_HEIGHT_PX = 120
        private const val COMPACT_MAX_WIDTH_PX = 320
        private const val COMPACT_MAX_HEIGHT_PX = 180

        fun forSize(preview: Boolean, widthPx: Int, heightPx: Int): PintFieldLayout = when {
            preview -> PICKER
            widthPx <= ICON_ONLY_MAX_WIDTH_PX || heightPx <= ICON_ONLY_MAX_HEIGHT_PX -> ICON_ONLY
            widthPx <= COMPACT_MAX_WIDTH_PX || heightPx <= COMPACT_MAX_HEIGHT_PX -> COMPACT
            else -> REGULAR
        }
    }
}
