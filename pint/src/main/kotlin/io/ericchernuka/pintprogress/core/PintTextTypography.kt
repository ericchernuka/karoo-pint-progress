package io.ericchernuka.pintprogress.core

/** Width-constrained typography for a single, text-only pint value. */
class PintTextTypography(
    val textSizeSp: Float,
) {
    companion object {
        private const val DEFAULT_TEXT_SIZE_SP = 72f
        private const val DEFAULT_FONT_SCALE = 1f

        // Conservative advances for Roboto Condensed Bold after textScaleX=0.68 in XML.
        private const val DIGIT_WIDTH_PER_TEXT_SIZE = 0.35f
        private const val DECIMAL_WIDTH_PER_TEXT_SIZE = 0.12f
        private const val OTHER_WIDTH_PER_TEXT_SIZE = 0.50f

        fun forField(
            karooTextSizeSp: Int,
            value: String,
            fontScale: Float,
            contentWidthDp: Int,
            contentHeightDp: Int,
        ): PintTextTypography {
            val validFontScale = fontScale.takeIf { it.isFinite() && it > 0f } ?: DEFAULT_FONT_SCALE
            val safeWidthDp = contentWidthDp.coerceAtLeast(1).toFloat()
            val safeHeightDp = contentHeightDp.coerceAtLeast(1).toFloat()
            val widthPerTextSize = value.sumOf { character ->
                when {
                    character.isDigit() -> DIGIT_WIDTH_PER_TEXT_SIZE.toDouble()
                    character == '.' -> DECIMAL_WIDTH_PER_TEXT_SIZE.toDouble()
                    else -> OTHER_WIDTH_PER_TEXT_SIZE.toDouble()
                }
            }.toFloat().coerceAtLeast(OTHER_WIDTH_PER_TEXT_SIZE) * validFontScale
            val widthCapSp = safeWidthDp / widthPerTextSize
            val heightCapSp = safeHeightDp / validFontScale
            val hostCapSp = karooTextSizeSp.takeIf { it > 0 }?.toFloat() ?: DEFAULT_TEXT_SIZE_SP

            return PintTextTypography(minOf(hostCapSp, widthCapSp, heightCapSp))
        }
    }
}
