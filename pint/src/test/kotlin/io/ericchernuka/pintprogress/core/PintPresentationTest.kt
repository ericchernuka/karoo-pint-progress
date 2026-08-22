package io.ericchernuka.pintprogress.core

import org.junit.Assert.assertEquals
import org.junit.Test

class PintPresentationTest {
    @Test
    fun `steady frames use each generated fill asset and the correct display text`() {
        for (bucket in 0..19) {
            val display = PintPresentation.displayFor(PintFrame.Steady(PintProgress(2, bucket)))

            assertEquals(PintAsset.entries[bucket], display.asset)
            assertEquals("${bucket * 5}% to next", display.detail)
            assertEquals("×2", display.count)
        }
    }

    @Test
    fun `presentation handles unavailable celebration and drain frames`() {
        assertEquals(
            PintDisplay(PintAsset.UNAVAILABLE, "Ride calories needed", ""),
            PintPresentation.displayFor(PintFrame.Unavailable),
        )
        assertEquals(
            PintDisplay(PintAsset.FULL_BUBBLES, "Cheers!", "×3"),
            PintPresentation.displayFor(PintFrame.FullBubbles(3)),
        )
        assertEquals(
            PintDisplay(PintAsset.DRAINING, "Next round", "×3"),
            PintPresentation.displayFor(PintFrame.Draining(3)),
        )
    }

    @Test
    fun `zero completions do not show a counter`() {
        assertEquals(
            "",
            PintPresentation.displayFor(PintFrame.Steady(PintProgress(0, 0))).count,
        )
    }

}
