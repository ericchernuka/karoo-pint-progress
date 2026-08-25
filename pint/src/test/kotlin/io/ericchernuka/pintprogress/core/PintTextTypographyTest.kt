package io.ericchernuka.pintprogress.core

import org.junit.Assert.assertEquals
import org.junit.Test

class PintTextTypographyTest {
    @Test
    fun `host text size is retained when the full value fits`() {
        assertEquals(
            88f,
            typography(
                textSizeSp = 88,
                value = "1.4",
                contentWidthDp = 120,
            ).textSizeSp,
        )
    }

    @Test
    fun `long totals shrink to the available width`() {
        assertEquals(
            78.94737f,
            typography(
                textSizeSp = 120,
                value = "100.0",
                contentWidthDp = 120,
            ).textSizeSp,
            0.00001f,
        )
    }

    @Test
    fun `shallow tiles shrink the value to the available height`() {
        assertEquals(
            40f,
            typography(
                textSizeSp = 72,
                value = "0.5",
                contentHeightDp = 40,
            ).textSizeSp,
        )
    }

    @Test
    fun `missing host guidance falls back to a stable default`() {
        assertEquals(
            72f,
            typography(
                textSizeSp = 0,
                value = "1.0",
                contentWidthDp = 120,
            ).textSizeSp,
        )
    }

    @Test
    fun `invalid font scale and a narrow width remain safe`() {
        listOf(0f, -1f, Float.NaN, Float.POSITIVE_INFINITY).forEach { invalidScale ->
            assertEquals(
                72f,
                typography(
                    textSizeSp = 0,
                    value = "—",
                    fontScale = invalidScale,
                    contentWidthDp = 120,
                ).textSizeSp,
            )
        }
        assertEquals(
            1.2195122f,
            typography(
                textSizeSp = 999,
                value = "1.0",
                contentWidthDp = 0,
            ).textSizeSp,
            0.00001f,
        )
    }

    @Test
    fun `an empty value still receives a finite width cap`() {
        assertEquals(
            240f,
            typography(
                textSizeSp = 999,
                value = "",
                contentWidthDp = 120,
            ).textSizeSp,
        )
    }

    private fun typography(
        textSizeSp: Int,
        value: String,
        fontScale: Float = 1f,
        contentWidthDp: Int = 120,
        contentHeightDp: Int = 1_000,
    ): PintTextTypography = PintTextTypography.forField(
        karooTextSizeSp = textSizeSp,
        value = value,
        fontScale = fontScale,
        contentWidthDp = contentWidthDp,
        contentHeightDp = contentHeightDp,
    )
}
