package io.ericchernuka.pintprogress.core

import org.junit.Assert.assertEquals
import org.junit.Test

class PintTextPresentationTest {
    @Test
    fun `text presentation uses floored 0 point 1 pint increments`() {
        assertEquals("0.0", textFor(completed = 0, bucket = 0))
        assertEquals("0.0", textFor(completed = 0, bucket = 1))
        assertEquals("0.1", textFor(completed = 0, bucket = 2))
        assertEquals("0.8", textFor(completed = 0, bucket = 17))
        assertEquals("0.9", textFor(completed = 0, bucket = 19))
        assertEquals("1.0", textFor(completed = 1, bucket = 0))
        assertEquals("12.3", textFor(completed = 12, bucket = 6))
    }

    @Test
    fun `full and draining frames show an exact completed pint total`() {
        assertEquals(
            PintTextDisplay("3.0"),
            PintTextPresentation.displayFor(PintFrame.FullBubbles(completed = 3)),
        )
        assertEquals(
            PintTextDisplay("3.0"),
            PintTextPresentation.displayFor(PintFrame.Draining(completed = 3)),
        )
    }

    @Test
    fun `unavailable state and preview values are explicit`() {
        assertEquals(
            PintTextDisplay("—"),
            PintTextPresentation.displayFor(PintFrame.Unavailable),
        )
        assertEquals(
            listOf(
                PintFrame.Steady(PintProgress(completed = 0, fillBucket = 10)),
                PintFrame.Steady(PintProgress(completed = 0, fillBucket = 16)),
                PintFrame.FullBubbles(completed = 1),
            ),
            PintTextPresentation.previewFrames(),
        )
    }

    private fun textFor(completed: Int, bucket: Int): String = PintTextPresentation.displayFor(
        PintFrame.Steady(PintProgress(completed, bucket)),
    ).value
}
