package io.ericchernuka.pintprogress

import io.ericchernuka.pintprogress.core.PintAsset
import io.ericchernuka.pintprogress.core.PintFieldLayout
import org.junit.Assert.assertEquals
import org.junit.Test

class PintAssetDrawablesTest {
    @Test
    fun `picker maps each preview state to its compact drawable`() {
        assertEquals(
            R.drawable.pint_50_compact,
            PintAsset.PINT_50.drawableRes(PintFieldLayout.PICKER),
        )
        assertEquals(
            R.drawable.pint_80_compact,
            PintAsset.PINT_80.drawableRes(PintFieldLayout.PICKER),
        )
        assertEquals(
            R.drawable.pint_full_bubbles_compact,
            PintAsset.FULL_BUBBLES.drawableRes(PintFieldLayout.PICKER),
        )
    }
}
