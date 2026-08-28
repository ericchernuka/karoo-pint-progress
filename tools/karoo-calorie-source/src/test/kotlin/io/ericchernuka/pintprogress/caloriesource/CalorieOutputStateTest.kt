package io.ericchernuka.pintprogress.caloriesource

import org.junit.Assert.assertEquals
import org.junit.Test

class CalorieOutputStateTest {
    @Test
    fun `silence preserves the last value for resume`() {
        val selected = CalorieOutputState().select(CaloriePreset.NINETY_FIVE_PERCENT.calories)
        val silent = selected.silence()

        assertEquals(false, silent.isEmitting)
        assertEquals(171.0, silent.resume().targetCalories, 0.0)
        assertEquals(true, silent.resume().isEmitting)
    }

    @Test
    fun `initial output is 95 percent`() {
        assertEquals(171.0, CalorieOutputState().targetCalories, 0.0)
    }

    @Test
    fun `only the Karoo system package can control the extension`() {
        assertEquals(true, allowsKarooCaller(arrayOf("io.hammerhead.appstore")))
        assertEquals(false, allowsKarooCaller(arrayOf("example.other")))
        assertEquals(false, allowsKarooCaller(null))
    }
}
