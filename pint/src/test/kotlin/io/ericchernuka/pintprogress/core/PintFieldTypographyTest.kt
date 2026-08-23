package io.ericchernuka.pintprogress.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PintFieldTypographyTest {
    @Test
    fun `regular and compact counters use the host size up to their calibrated mug height`() {
        val regularTypography = PintFieldTypography.forLayout(PintFieldLayout.REGULAR, karooTextSizeSp = 92)

        assertEquals(
            PintFieldTypography(countTextSizeSp = 92f, suffixTextSizeSp = 43f),
            regularTypography,
        )
        requireNotNull(regularTypography)
        assertEquals(92f, regularTypography.countTextSizeSp)
        assertEquals(43f, regularTypography.suffixTextSizeSp)
        assertEquals(92f, regularTypography.component1())
        assertEquals(43f, regularTypography.component2())
        assertEquals(regularTypography, regularTypography.copy())
        assertEquals(
            PintFieldTypography(countTextSizeSp = 93f, suffixTextSizeSp = 43f),
            regularTypography.copy(countTextSizeSp = 93f),
        )
        assertEquals(
            PintFieldTypography(countTextSizeSp = 92f, suffixTextSizeSp = 44f),
            regularTypography.copy(suffixTextSizeSp = 44f),
        )
        assertEquals(regularTypography.hashCode(), regularTypography.copy().hashCode())
        assertEquals(
            "PintFieldTypography(countTextSizeSp=92.0, suffixTextSizeSp=43.0)",
            regularTypography.toString(),
        )
        assertEquals(regularTypography, regularTypography)
        assertNotEquals(regularTypography, "92 plus 43")
        assertNotEquals(
            regularTypography,
            PintFieldTypography(countTextSizeSp = 93f, suffixTextSizeSp = 43f),
        )
        assertNotEquals(
            regularTypography,
            PintFieldTypography(countTextSizeSp = 92f, suffixTextSizeSp = 44f),
        )

        assertEquals(
            PintFieldTypography(countTextSizeSp = 68f, suffixTextSizeSp = 32f),
            PintFieldTypography.forLayout(PintFieldLayout.COMPACT, karooTextSizeSp = 68),
        )
    }

    @Test
    fun `unexpected and oversized host sizes retain the calibrated maximum`() {
        assertEquals(
            PintFieldTypography(countTextSizeSp = 110f, suffixTextSizeSp = 52f),
            PintFieldTypography.forLayout(PintFieldLayout.REGULAR, karooTextSizeSp = 999),
        )
        assertEquals(
            PintFieldTypography(countTextSizeSp = 68f, suffixTextSizeSp = 32f),
            PintFieldTypography.forLayout(PintFieldLayout.COMPACT, karooTextSizeSp = 0),
        )
    }

    @Test
    fun `picker and icon only treatments have no counter typography`() {
        assertNull(PintFieldTypography.forLayout(PintFieldLayout.PICKER, karooTextSizeSp = 110))
        assertNull(PintFieldTypography.forLayout(PintFieldLayout.ICON_ONLY, karooTextSizeSp = 68))
    }
}
