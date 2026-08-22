package io.ericchernuka.pintprogress.core

import org.junit.Assert.assertEquals
import org.junit.Test

class PintFieldLayoutTest {
    @Test
    fun `picker always uses its dedicated compact artwork`() {
        assertEquals(PintFieldLayout.PICKER, PintFieldLayout.forSize(preview = true, widthPx = 480, heightPx = 200))
        assertEquals(PintFieldLayout.PICKER, PintFieldLayout.forSize(preview = true, widthPx = 120, heightPx = 80))
    }

    @Test
    fun `roomy fields keep the full counter and mug`() {
        assertEquals(PintFieldLayout.REGULAR, PintFieldLayout.forSize(preview = false, widthPx = 480, heightPx = 200))
    }

    @Test
    fun `narrow or short fields use the compact counter and mug`() {
        assertEquals(PintFieldLayout.COMPACT, PintFieldLayout.forSize(preview = false, widthPx = 320, heightPx = 200))
        assertEquals(PintFieldLayout.COMPACT, PintFieldLayout.forSize(preview = false, widthPx = 480, heightPx = 180))
    }

    @Test
    fun `tiles too small for a readable counter retain only the live mug`() {
        assertEquals(PintFieldLayout.ICON_ONLY, PintFieldLayout.forSize(preview = false, widthPx = 180, heightPx = 200))
        assertEquals(PintFieldLayout.ICON_ONLY, PintFieldLayout.forSize(preview = false, widthPx = 480, heightPx = 120))
    }
}
