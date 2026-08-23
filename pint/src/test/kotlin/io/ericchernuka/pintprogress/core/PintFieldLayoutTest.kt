package io.ericchernuka.pintprogress.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PintFieldLayoutTest {
    @Test
    fun `picker always uses its dedicated artwork`() {
        assertEquals(PintFieldLayout.PICKER, layout(preview = true, widthDp = 480, heightDp = 200))
        assertEquals(PintFieldLayout.PICKER, layout(preview = true, widthDp = 20, heightDp = 20, boundaries = true))
    }

    @Test
    fun `regular treatment requires both count and mug constraints`() {
        assertEquals(PintFieldLayout.REGULAR, layout(widthDp = 184, heightDp = 93))
        assertEquals(PintFieldLayout.COMPACT, layout(widthDp = 183, heightDp = 93))
        assertEquals(PintFieldLayout.COMPACT, layout(widthDp = 184, heightDp = 92))
    }

    @Test
    fun `compact treatment requires both compact constraints`() {
        assertEquals(PintFieldLayout.COMPACT, layout(widthDp = 112, heightDp = 69))
        assertEquals(PintFieldLayout.ICON_ONLY, layout(widthDp = 111, heightDp = 69))
        assertEquals(PintFieldLayout.ICON_ONLY, layout(widthDp = 112, heightDp = 68))
        assertEquals(PintFieldLayout.ICON_ONLY, layout(widthDp = -1, heightDp = -1))
    }

    @Test
    fun `boundary inset participates in fit selection`() {
        assertEquals(PintFieldLayout.REGULAR, layout(widthDp = 192, heightDp = 101, boundaries = true))
        assertEquals(PintFieldLayout.COMPACT, layout(widthDp = 184, heightDp = 93, boundaries = true))
        assertEquals(PintFieldLayout.COMPACT, layout(widthDp = 120, heightDp = 77, boundaries = true))
        assertEquals(PintFieldLayout.ICON_ONLY, layout(widthDp = 112, heightDp = 69, boundaries = true))
    }

    @Test
    fun `equal dp dimensions choose the same treatment across densities`() {
        val densityOne = PintFieldSize.resolve(
            viewSize = 240 to 100,
            gridSize = 30 to 15,
            screenSize = 480 to 800,
            density = 1f,
        )
        val densityTwo = PintFieldSize.resolve(
            viewSize = 480 to 200,
            gridSize = 30 to 15,
            screenSize = 960 to 1600,
            density = 2f,
        )

        assertEquals(densityOne.widthDp, densityTwo.widthDp)
        assertEquals(densityOne.heightDp, densityTwo.heightDp)
        assertEquals(
            layout(widthDp = densityOne.widthDp, heightDp = densityOne.heightDp),
            layout(widthDp = densityTwo.widthDp, heightDp = densityTwo.heightDp),
        )
    }

    @Test
    fun `layout metadata identifies treatments with a counter`() {
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

    @Test
    fun `live content without a completed count uses adaptive mug only layout`() {
        assertEquals(PintFieldLayout.ICON_ONLY, PintFieldLayout.REGULAR.forDisplay(false))
        assertEquals(PintFieldLayout.ICON_ONLY, PintFieldLayout.COMPACT.forDisplay(false))
        assertEquals(PintFieldLayout.ICON_ONLY, PintFieldLayout.ICON_ONLY.forDisplay(false))
    }

    @Test
    fun `completed counts preserve the viewport treatment and preview remains representative`() {
        assertEquals(PintFieldLayout.REGULAR, PintFieldLayout.REGULAR.forDisplay(true))
        assertEquals(PintFieldLayout.COMPACT, PintFieldLayout.COMPACT.forDisplay(true))
        assertEquals(PintFieldLayout.ICON_ONLY, PintFieldLayout.ICON_ONLY.forDisplay(true))
        assertEquals(PintFieldLayout.PICKER, PintFieldLayout.PICKER.forDisplay(false))
        assertEquals(PintFieldLayout.PICKER, PintFieldLayout.PICKER.forDisplay(true))
    }

    private fun layout(
        preview: Boolean = false,
        widthDp: Int,
        heightDp: Int,
        boundaries: Boolean = false,
    ): PintFieldLayout = PintFieldLayout.forSize(
        preview = preview,
        widthDp = widthDp,
        heightDp = heightDp,
        boundariesEnabled = boundaries,
    )
}
