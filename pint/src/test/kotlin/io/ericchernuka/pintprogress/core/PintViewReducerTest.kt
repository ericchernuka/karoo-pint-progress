package io.ericchernuka.pintprogress.core

import io.hammerhead.karooext.models.DataPoint
import io.hammerhead.karooext.models.StreamState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PintViewReducerTest {
    @Test
    fun `first unavailable state renders once and duplicate unavailable states are suppressed`() {
        val reducer = PintViewReducer()

        assertEquals(unavailablePlan(), reducer.accept(StreamState.Idle))
        assertNull(reducer.accept(StreamState.NotAvailable))
        assertNull(reducer.accept(StreamState.Searching))
        assertNull(reducer.accept(calories(-1.0)))
    }

    @Test
    fun `reducer suppresses duplicate visible fill and accepts the next fill bucket`() {
        val reducer = PintViewReducer()

        assertEquals(steadyPlan(0, 0), reducer.accept(calories(0.0)))
        assertNull(reducer.accept(calories(3.0)))
        assertEquals(steadyPlan(0, 1), reducer.accept(calories(8.0)))
    }

    @Test
    fun `one observed threshold produces bubbles drain then the next glass`() {
        val reducer = PintViewReducer()
        reducer.accept(calories(149.0))

        assertEquals(
            RenderPlan(
                listOf(
                    TimedFrame(0, PintFrame.FullBubbles(1)),
                    TimedFrame(PintProgressReducer.ANIMATION_STEP_MILLIS, PintFrame.Draining(1)),
                    TimedFrame(
                        PintProgressReducer.ANIMATION_STEP_MILLIS,
                        PintFrame.Steady(PintProgress(1, 0)),
                    ),
                ),
            ),
            reducer.accept(calories(DEFAULT_BEER_CALORIES)),
        )
        assertNull(reducer.accept(calories(DEFAULT_BEER_CALORIES + 1.0)))
        assertEquals(steadyPlan(1, 19), reducer.accept(calories(299.0)))
    }

    @Test
    fun `reset skipped threshold and recovery remain steady`() {
        val reducer = PintViewReducer()

        assertEquals(steadyPlan(2, 0), reducer.accept(calories(300.0)))
        assertEquals(steadyPlan(0, 0), reducer.accept(calories(0.0)))
        assertEquals(steadyPlan(2, 0), reducer.accept(calories(300.0)))
        assertEquals(unavailablePlan(), reducer.accept(StreamState.Idle))
        assertEquals(steadyPlan(1, 0), reducer.accept(calories(DEFAULT_BEER_CALORIES)))
    }

    @Test
    fun `stream-state conversion and preview are deterministic`() {
        assertEquals(steadyPlan(0, 10), PintViewReducer().accept(calories(75.0)))
        assertEquals(unavailablePlan(), PintViewReducer().accept(StreamState.Idle))
        assertEquals(unavailablePlan(), PintViewReducer().accept(StreamState.NotAvailable))
        assertEquals(unavailablePlan(), PintViewReducer().accept(StreamState.Searching))
        assertEquals(
            listOf(
                PintFrame.Steady(PintProgress(0, 10)),
                PintFrame.Steady(PintProgress(0, 16)),
                PintFrame.FullBubbles(0),
            ),
            PintViewReducer.previewFrames(),
        )
    }

    private fun unavailablePlan() = RenderPlan(listOf(TimedFrame(0, PintFrame.Unavailable)))

    private fun steadyPlan(completed: Int, bucket: Int) = RenderPlan(
        listOf(TimedFrame(0, PintFrame.Steady(PintProgress(completed, bucket)))),
    )

    private fun calories(value: Double): StreamState.Streaming = StreamState.Streaming(
        DataPoint(
            dataTypeId = "TYPE_CALORIES_ID",
            values = mapOf("FIELD_CALORIES_ID" to value),
        ),
    )
}
