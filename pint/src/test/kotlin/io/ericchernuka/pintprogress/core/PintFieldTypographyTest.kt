package io.ericchernuka.pintprogress.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PintFieldTypographyTest {
    @Test
    fun `regular and compact counters use the host size up to their calibrated mug height`() {
        val regularTypography = PintFieldTypography.forLayout(PintFieldLayout.REGULAR, karooTextSizeSp = 92)

        requireNotNull(regularTypography)
        assertEquals(92f, regularTypography.countTextSizeSp)
        assertEquals(43f, regularTypography.suffixTextSizeSp)

        val compactTypography = requireNotNull(
            PintFieldTypography.forLayout(PintFieldLayout.COMPACT, karooTextSizeSp = 68),
        )
        assertEquals(68f, compactTypography.countTextSizeSp)
        assertEquals(32f, compactTypography.suffixTextSizeSp)
    }

    @Test
    fun `unexpected and oversized host sizes retain the calibrated maximum`() {
        val cappedRegular = requireNotNull(
            PintFieldTypography.forLayout(PintFieldLayout.REGULAR, karooTextSizeSp = 999),
        )
        assertEquals(110f, cappedRegular.countTextSizeSp)
        assertEquals(52f, cappedRegular.suffixTextSizeSp)

        val defaultCompact = requireNotNull(
            PintFieldTypography.forLayout(PintFieldLayout.COMPACT, karooTextSizeSp = 0),
        )
        assertEquals(68f, defaultCompact.countTextSizeSp)
        assertEquals(32f, defaultCompact.suffixTextSizeSp)
    }

    @Test
    fun `picker and icon only treatments have no counter typography`() {
        assertNull(PintFieldTypography.forLayout(PintFieldLayout.PICKER, karooTextSizeSp = 110))
        assertNull(PintFieldTypography.forLayout(PintFieldLayout.ICON_ONLY, karooTextSizeSp = 68))
    }
}
