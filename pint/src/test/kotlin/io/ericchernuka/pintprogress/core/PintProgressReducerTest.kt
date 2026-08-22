package io.ericchernuka.pintprogress.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PintProgressReducerTest {
    @Test
    fun `progress rejects unavailable and invalid calorie values`() {
        assertNull(PintProgressReducer.progressFor(null))
        assertNull(PintProgressReducer.progressFor(Double.NaN))
        assertNull(PintProgressReducer.progressFor(Double.POSITIVE_INFINITY))
        assertNull(PintProgressReducer.progressFor(-0.01))
    }

    @Test
    fun `progress maps exact five percent boundaries across completed pints`() {
        for (completed in 0..3) {
            for (bucket in 0..19) {
                val calories = completed * DEFAULT_BEER_CALORIES + bucket * (DEFAULT_BEER_CALORIES / 20.0)

                assertEquals(PintProgress(completed, bucket), PintProgressReducer.progressFor(calories))
            }
        }

        assertEquals(PintProgress(1, 3), PintProgressReducer.progressFor(179.999_999))
    }

    @Test
    fun `progress rolls exact target calories into the completed counter`() {
        assertEquals(PintProgress(0, 0), PintProgressReducer.progressFor(0.0))
        assertEquals(PintProgress(1, 0), PintProgressReducer.progressFor(DEFAULT_BEER_CALORIES))
        assertEquals(PintProgress(2, 10), PintProgressReducer.progressFor(DEFAULT_BEER_CALORIES * 2.5))
    }

    @Test
    fun `progress caps an extreme calorie input without overflowing`() {
        assertEquals(
            PintProgress(Int.MAX_VALUE, 0),
            PintProgressReducer.progressFor(Double.MAX_VALUE),
        )
    }

    @Test
    fun `first available reading is steady rather than a retrospective celebration`() {
        assertEquals(
            RenderPlan(listOf(TimedFrame(0, PintFrame.Steady(PintProgress(3, 8))))),
            PintProgressReducer.plan(null, PintProgress(3, 8)),
        )
    }

    @Test
    fun `unavailable source renders an unavailable frame`() {
        assertEquals(
            RenderPlan(listOf(TimedFrame(0, PintFrame.Unavailable))),
            PintProgressReducer.plan(PintProgress(2, 19), null),
        )
    }

    @Test
    fun `one observed completion plays full bubbles drain then steady`() {
        val current = PintProgress(2, 4)

        assertEquals(
            RenderPlan(
                listOf(
                    TimedFrame(0, PintFrame.FullBubbles(2)),
                    TimedFrame(PintProgressReducer.ANIMATION_STEP_MILLIS, PintFrame.Draining(2)),
                    TimedFrame(PintProgressReducer.ANIMATION_STEP_MILLIS, PintFrame.Steady(current)),
                ),
            ),
            PintProgressReducer.plan(PintProgress(1, 19), current),
        )
    }

    @Test
    fun `unchanged backwards and skipped completion counts stay steady`() {
        val previous = PintProgress(3, 10)

        assertEquals(
            RenderPlan(listOf(TimedFrame(0, PintFrame.Steady(PintProgress(3, 11))))),
            PintProgressReducer.plan(previous, PintProgress(3, 11)),
        )
        assertEquals(
            RenderPlan(listOf(TimedFrame(0, PintFrame.Steady(PintProgress(2, 19))))),
            PintProgressReducer.plan(previous, PintProgress(2, 19)),
        )
        assertEquals(
            RenderPlan(listOf(TimedFrame(0, PintFrame.Steady(PintProgress(5, 0))))),
            PintProgressReducer.plan(previous, PintProgress(5, 0)),
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun `progress rejects a negative completion count`() {
        PintProgress(-1, 0)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `progress rejects an out of range fill bucket`() {
        PintProgress(0, 20)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `progress rejects a negative fill bucket`() {
        PintProgress(0, -1)
    }

    @Test
    fun `progress and render models expose their values`() {
        val progress = PintProgress(completed = 4, fillBucket = 13)
        val steady = PintFrame.Steady(progress)
        val full = PintFrame.FullBubbles(4)
        val draining = PintFrame.Draining(4)
        val timed = TimedFrame(delayMillis = 123, frame = steady)
        val plan = RenderPlan(listOf(timed))

        assertEquals(4, progress.completed)
        assertEquals(13, progress.fillBucket)
        assertEquals(progress, steady.progress)
        assertEquals(4, full.completed)
        assertEquals(4, draining.completed)
        assertEquals(123, timed.delayMillis)
        assertEquals(steady, timed.frame)
        assertEquals(listOf(timed), plan.frames)
    }
}
