package io.ericchernuka.pintprogress.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PintFieldLayoutTest {
    @Test
    fun `picker always uses its dedicated compact artwork`() {
        assertEquals(PintFieldLayout.PICKER, PintFieldLayout.forSize(preview = true, widthDp = 480, heightDp = 200))
        assertEquals(PintFieldLayout.PICKER, PintFieldLayout.forSize(preview = true, widthDp = 120, heightDp = 80))
    }

    @Test
    fun `roomy fields keep the full counter and mug`() {
        assertEquals(PintFieldLayout.REGULAR, PintFieldLayout.forSize(preview = false, widthDp = 480, heightDp = 200))
    }

    @Test
    fun `narrow or short fields use the compact counter and mug`() {
        assertEquals(PintFieldLayout.COMPACT, PintFieldLayout.forSize(preview = false, widthDp = 320, heightDp = 200))
        assertEquals(PintFieldLayout.COMPACT, PintFieldLayout.forSize(preview = false, widthDp = 480, heightDp = 180))
    }

    @Test
    fun `tiles too small for a readable counter retain only the live mug`() {
        assertEquals(PintFieldLayout.ICON_ONLY, PintFieldLayout.forSize(preview = false, widthDp = 180, heightDp = 200))
        assertEquals(PintFieldLayout.ICON_ONLY, PintFieldLayout.forSize(preview = false, widthDp = 480, heightDp = 120))
    }

    @Test
    fun `equal density independent dimensions choose the same treatment across densities`() {
        val densityOneSize = PintFieldSize.resolve(
            viewSize = 320 to 200,
            gridSize = 30 to 15,
            screenSize = 480 to 800,
            density = 1f,
        )
        val densityTwoSize = PintFieldSize.resolve(
            viewSize = 640 to 400,
            gridSize = 30 to 15,
            screenSize = 960 to 1600,
            density = 2f,
        )

        assertEquals(densityOneSize.widthDp, densityTwoSize.widthDp)
        assertEquals(densityOneSize.heightDp, densityTwoSize.heightDp)
        assertEquals(
            PintFieldLayout.forSize(false, densityOneSize.widthDp, densityOneSize.heightDp),
            PintFieldLayout.forSize(false, densityTwoSize.widthDp, densityTwoSize.heightDp),
        )
    }

    @Test
    fun `layout metadata identifies the treatments with a counter`() {
        assertFalse(PintFieldLayout.PICKER.showsCount)
        assertTrue(PintFieldLayout.REGULAR.showsCount)
        assertTrue(PintFieldLayout.COMPACT.showsCount)
        assertFalse(PintFieldLayout.ICON_ONLY.showsCount)

        assertEquals(
            listOf(
                PintFieldLayout.PICKER,
                PintFieldLayout.REGULAR,
                PintFieldLayout.COMPACT,
                PintFieldLayout.ICON_ONLY,
            ),
            PintFieldLayout.entries,
        )
        assertEquals(PintFieldLayout.PICKER, PintFieldLayout.valueOf("PICKER"))
        assertEquals(PintFieldLayout.values().toList(), PintFieldLayout.entries)
    }
}
