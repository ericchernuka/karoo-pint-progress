package io.ericchernuka.pintprogress.core

import io.hammerhead.karooext.models.DataPoint
import io.hammerhead.karooext.models.DataType
import io.hammerhead.karooext.models.StreamState
import org.junit.Assert.assertEquals
import org.junit.Test

class PintTextStreamStateTest {
    @Test
    fun `streaming calories become a floored decimal pint total`() {
        assertEquals(0.0, valueFor(0.0), 0.0)
        assertEquals(0.4, valueFor(74.999_999), 0.0)
        assertEquals(0.5, valueFor(75.0), 0.0)
        assertEquals(0.9, valueFor(149.999_999), 0.0)
        assertEquals(1.0, valueFor(150.0), 0.0)
        assertEquals(12.3, valueFor(1_845.0), 0.0)
    }

    @Test
    fun `selected calorie target controls the decimal pint total`() {
        assertEquals(1.0, valueFor(calories = 80.0, caloriesPerBeer = 80), 0.0)
        assertEquals(0.5, valueFor(calories = 200.0, caloriesPerBeer = 400), 0.0)
    }

    @Test
    fun `native stream states and invalid calories remain unavailable`() {
        assertEquals(StreamState.Idle, convert(StreamState.Idle))
        assertEquals(StreamState.Searching, convert(StreamState.Searching))
        assertEquals(StreamState.NotAvailable, convert(StreamState.NotAvailable))
        assertEquals(StreamState.NotAvailable, convert(streaming(Double.NaN)))
        assertEquals(StreamState.NotAvailable, convert(streaming(-0.1)))
    }

    @Test
    fun `native field preview demonstrates progress across a completed pint`() {
        assertEquals(
            listOf("0.5", "0.9", "1", "1.1"),
            PintTextStreamState.previewMessages(),
        )
    }

    private fun valueFor(
        calories: Double,
        caloriesPerBeer: Int = BeerCaloriesPolicy.DEFAULT,
    ): Double {
        val state = convert(streaming(calories), caloriesPerBeer) as StreamState.Streaming
        assertEquals(DATA_TYPE_ID, state.dataPoint.dataTypeId)
        assertEquals(setOf(DataType.Field.SINGLE), state.dataPoint.values.keys)
        return requireNotNull(state.dataPoint.singleValue)
    }

    private fun convert(
        state: StreamState,
        caloriesPerBeer: Int = BeerCaloriesPolicy.DEFAULT,
    ): StreamState = PintTextStreamState.from(
        source = state,
        caloriesPerBeer = caloriesPerBeer,
        dataTypeId = DATA_TYPE_ID,
    )

    private fun streaming(calories: Double): StreamState.Streaming = StreamState.Streaming(
        DataPoint(
            dataTypeId = DataType.Type.CALORIES,
            values = mapOf(DataType.Field.SINGLE to calories),
        ),
    )

    private companion object {
        const val DATA_TYPE_ID = "pintprogress:pint-progress-text"
    }
}
