package io.ericchernuka.pintprogress.core

import org.junit.Assert.assertEquals
import org.junit.Test

class PintTextLayoutTest {
    @Test
    fun `preview uses a dedicated picker treatment`() {
        assertEquals(PintTextLayout.PICKER, PintTextLayout.forMode(preview = true))
        assertEquals(PintTextLayout.LIVE, PintTextLayout.forMode(preview = false))
    }

    @Test
    fun `layout metadata exposes both treatments`() {
        assertEquals(listOf(PintTextLayout.PICKER, PintTextLayout.LIVE), PintTextLayout.entries)
        assertEquals(PintTextLayout.PICKER, PintTextLayout.valueOf("PICKER"))
        assertEquals(PintTextLayout.values().toList(), PintTextLayout.entries)
    }
}
