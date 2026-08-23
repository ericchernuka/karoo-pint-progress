package io.ericchernuka.pintprogress

import android.view.Gravity
import io.hammerhead.karooext.models.ViewConfig
import org.junit.Assert.assertEquals
import org.junit.Test

class PintFieldGravityTest {
    @Test
    fun `all Karoo alignment settings position the complete group`() {
        assertEquals(Gravity.LEFT or Gravity.CENTER_VERTICAL, ViewConfig.Alignment.LEFT.gravity())
        assertEquals(Gravity.CENTER, ViewConfig.Alignment.CENTER.gravity())
        assertEquals(Gravity.RIGHT or Gravity.CENTER_VERTICAL, ViewConfig.Alignment.RIGHT.gravity())
    }
}
