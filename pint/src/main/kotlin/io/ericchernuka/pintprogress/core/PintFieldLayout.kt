package io.ericchernuka.pintprogress.core

enum class PintFieldLayout(val showsCount: Boolean) { PICKER(false), REGULAR(true), COMPACT(true), ICON_ONLY(false), ;

    fun forDisplay(hasCompletedCount: Boolean): PintFieldLayout = when { this == PICKER -> PICKER
        !hasCompletedCount -> ICON_ONLY
        else -> this }
    fun visibleCount(count: String): String = count.takeIf { showsCount }.orEmpty()
    companion object { private const val REGULAR_MIN_WIDTH_DP = 180
        private const val REGULAR_MIN_HEIGHT_DP = 89
        private const val COMPACT_MIN_WIDTH_DP = 108
        private const val COMPACT_MIN_HEIGHT_DP = 65
        fun forSize(preview: Boolean, widthDp: Int, heightDp: Int, boundariesEnabled: Boolean, ): PintFieldLayout { if (preview) return PICKER
            val edgeInsetsDp = PintFieldChrome.edgeInsetDp(boundariesEnabled) * 2
            val usableWidthDp = widthDp - edgeInsetsDp
            val usableHeightDp = heightDp - edgeInsetsDp
            return when { usableWidthDp >= REGULAR_MIN_WIDTH_DP && usableHeightDp >= REGULAR_MIN_HEIGHT_DP -> REGULAR
                usableWidthDp >= COMPACT_MIN_WIDTH_DP && usableHeightDp >= COMPACT_MIN_HEIGHT_DP -> COMPACT
                else -> ICON_ONLY } } } }
