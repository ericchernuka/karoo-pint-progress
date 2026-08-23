package io.ericchernuka.pintprogress.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PintFieldTypographyTest {
    @Test
    fun `regular and compact counters use the host size up to their calibrated mug height`() {
        assertEquals(
            PintFieldTypography(countTextSizeSp = 92f, suffixTextSizeSp = 43f),
            PintFieldTypography.forLayout(PintFieldLayout.REGULAR, karooTextSizeSp = 92),
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
