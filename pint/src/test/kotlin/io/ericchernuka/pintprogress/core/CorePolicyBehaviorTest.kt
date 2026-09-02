package io.ericchernuka.pintprogress.core

import io.hammerhead.karooext.models.DataPoint
import io.hammerhead.karooext.models.DataType
import io.hammerhead.karooext.models.StreamState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class CorePolicyBehaviorTest {
    @Test
    fun `all deterministic core behavior remains exact`() {
        assertEquals(BeerCaloriesPolicy.MIN, BeerCaloriesPolicy.normalize(Int.MIN_VALUE))
        assertEquals(BeerCaloriesPolicy.MIN, BeerCaloriesPolicy.normalize(80))
        assertEquals(BeerCaloriesPolicy.MIN, BeerCaloriesPolicy.normalize(82))
        assertEquals(85, BeerCaloriesPolicy.normalize(83))
        assertEquals(85, BeerCaloriesPolicy.normalize(87))
        assertEquals(90, BeerCaloriesPolicy.normalize(88))
        assertEquals(BeerCaloriesPolicy.MAX, BeerCaloriesPolicy.normalize(Int.MAX_VALUE))
        assertEquals(BeerCaloriesPolicy.MIN, BeerCaloriesPolicy.fromSliderProgress(-1))
        assertEquals(BeerCaloriesPolicy.DEFAULT, BeerCaloriesPolicy.fromSliderProgress(14))
        assertEquals(
            BeerCaloriesPolicy.MAX,
            BeerCaloriesPolicy.fromSliderProgress(BeerCaloriesPolicy.STEP_COUNT + 1),
        )
        assertEquals(0, BeerCaloriesPolicy.toSliderProgress(Int.MIN_VALUE))
        assertEquals(14, BeerCaloriesPolicy.toSliderProgress(BeerCaloriesPolicy.DEFAULT))
        assertEquals(BeerCaloriesPolicy.STEP_COUNT, BeerCaloriesPolicy.toSliderProgress(Int.MAX_VALUE))
        assertFalse(allowsKarooCaller(null))
        assertFalse(allowsKarooCaller(emptyArray()))
        assertFalse(allowsKarooCaller(arrayOf("com.example.untrusted")))
        assertTrue(
            allowsKarooCaller(
                arrayOf("com.example.shareduid", KAROO_SYSTEM_PACKAGE),
            ),
        )
        for (bucket in 0..19) {
            val fillDisplay = fillDisplayFor(PintFrame.Steady(PintProgress(2, bucket)))
            assertEquals(PintFillAsset.entries[bucket], fillDisplay.first)
            assertEquals("2", fillDisplay.second)
        }
        assertEquals(
            PintFillAsset.FILL_UNAVAILABLE to "—",
            fillDisplayFor(PintFrame.Unavailable),
        )
        assertEquals(
            PintFillAsset.FILL_00 to "0",
            fillDisplayFor(PintFrame.Steady(PintProgress(0, 0))),
        )
        assertEquals(
            PintFillAsset.FILL_FULL_FOAM to "1",
            fillDisplayFor(PintFrame.FullBubbles(1)),
        )
        assertEquals(
            PintFillAsset.FILL_DRAINING to "1",
            fillDisplayFor(PintFrame.Draining(1)),
        )
        assertEquals(
            listOf(
                PintFrame.Steady(PintProgress(completed = 0, fillBucket = 10)),
                PintFrame.Steady(PintProgress(completed = 0, fillBucket = 16)),
                PintFrame.FullBubbles(completed = 1),
                PintFrame.Draining(completed = 1),
            ),
            fillPreviewFrames(),
        )
        assertNull(PintProgressReducer.progressFor(null))
        assertNull(PintProgressReducer.progressFor(Double.NaN))
        assertNull(PintProgressReducer.progressFor(Double.POSITIVE_INFINITY))
        assertNull(PintProgressReducer.progressFor(-0.01))
        val target = BeerCaloriesPolicy.DEFAULT.toDouble()
        for (completed in 0..3) {
            for (bucket in 0..19) {
                val calories = completed * target + bucket * (target / 20.0)

                assertEquals(PintProgress(completed, bucket), PintProgressReducer.progressFor(calories))
            }
        }

        assertEquals(PintProgress(1, 3), PintProgressReducer.progressFor(179.999_999))
        listOf(
            Triple(0.0, 150, PintProgress(0, 0)),
            Triple(150.0, 150, PintProgress(1, 0)),
            Triple(375.0, 150, PintProgress(2, 10)),
            Triple(80.0, 80, PintProgress(1, 0)),
            Triple(200.0, 400, PintProgress(0, 10)),
            Triple(80.0, 1, PintProgress(1, 0)),
            Triple(400.0, 999, PintProgress(1, 0)),
            Triple(Double.MAX_VALUE, 150, PintProgress(Int.MAX_VALUE, 0)),
        ).forEach { (calories, target, expected) ->
            assertEquals(expected, PintProgressReducer.progressFor(calories, target))
        }
        listOf(-1 to 0, 0 to 20, 0 to -1).forEach { (completed, bucket) ->
            assertThrows(IllegalArgumentException::class.java) { PintProgress(completed, bucket) }
        }
        assertEquals(0.0, valueFor(0.0), 0.0)
        assertEquals(0.4, valueFor(74.999_999), 0.0)
        assertEquals(0.5, valueFor(75.0), 0.0)
        assertEquals(0.9, valueFor(149.999_999), 0.0)
        assertEquals(1.0, valueFor(150.0), 0.0)
        assertEquals(12.3, valueFor(1_845.0), 0.0)
        assertEquals(1.0, valueFor(calories = 80.0, caloriesPerBeer = 80), 0.0)
        assertEquals(0.5, valueFor(calories = 200.0, caloriesPerBeer = 400), 0.0)
        assertEquals(StreamState.Idle, convert(StreamState.Idle))
        assertEquals(StreamState.Searching, convert(StreamState.Searching))
        assertEquals(StreamState.NotAvailable, convert(StreamState.NotAvailable))
        assertEquals(StreamState.NotAvailable, convert(streaming(Double.NaN)))
        assertEquals(StreamState.NotAvailable, convert(streaming(-0.1)))
        listOf(
            listOf(
                idle to unavailable,
                unavailableState to null,
                searching to null,
                calories(-1.0) to null,
            ),
            listOf(
                calories(0.0) to steady(0, 0),
                calories(3.0) to null,
                calories(8.0) to steady(0, 1),
            ),
            listOf(
                calories(149.0) to steady(0, 19),
                calories(150.0) to animation(1),
                calories(151.0) to null,
                calories(299.0) to steady(1, 19),
            ),
            listOf(
                calories(300.0) to steady(2, 0),
                calories(0.0) to steady(0, 0),
                calories(300.0) to steady(2, 0),
                idle to unavailable,
                calories(150.0) to steady(1, 0),
            ),
        ).forEach { history ->
            val reducer = PintViewReducer()
            history.forEach { (state, expected) -> assertEquals(expected, reducer.accept(state)) }
        }
        val reducer = PintViewReducer()
        assertEquals(steady(0, 19), reducer.accept(calories(149.0)))
        assertEquals(steady(1, 17), reducer.accept(calories(149.0), 80))
        assertEquals(animation(2), reducer.accept(calories(160.0), 80))

        val activeReducer = PintViewReducer()
        assertEquals(steady(0, 19), activeReducer.accept(calories(149.0)))
        assertEquals(animation(1), activeReducer.accept(calories(150.0)))
        assertEquals(
            PintViewUpdate.RefreshTransition(
                steady = PintFrame.Steady(PintProgress(1, 1)),
            ),
            activeReducer.accept(calories(158.0), activeTransitionCompleted = 1),
        )

        val bucketReducer = PintViewReducer()
        assertEquals(steady(0, 0), bucketReducer.accept(calories(1.0)))
        assertEquals(steady(0, 0), bucketReducer.accept(calories(1.0), 400))
        assertEquals(steady(0, 1), bucketReducer.accept(calories(20.0), 400))
        listOf(idle, unavailableState, searching).forEach {
            assertEquals(unavailable, PintViewReducer().accept(it))
        }
        assertEquals(steady(0, 10), PintViewReducer().accept(calories(75.0)))
        assertEquals(480 to 200, size(480 to 200, 1f))
        assertEquals(481 to 201, size(961 to 401, 2f))
        assertEquals(120 to 200, size(0 to 400, 2f))
        assertEquals(200 to 100, size(400 to 0, 2f))
        listOf(0f, -2f, Float.NaN, Float.POSITIVE_INFINITY).forEach {
            assertEquals(480 to 200, size(480 to 200, it))
        }
        fun checkFillTypography(
            expected: Float,
            textSize: Int,
            fontScale: Float,
            width: Int,
            height: Int,
            textWidthPerSize: Float = 1f,
            lineHeightPerSize: Float = 1.2f,
        ) = assertEquals(
            expected,
            resolveFillTextSize(
                karooTextSizeSp = textSize,
                fontScale = fontScale,
                contentWidthDp = width,
                contentHeightDp = height,
                measuredTextWidthPerTextSize = textWidthPerSize,
                lineHeightPerTextSize = lineHeightPerSize,
            ),
            0.001f,
        )
        checkFillTypography(96f, textSize = 96, fontScale = 1f, width = 300, height = 200)
        checkFillTypography(40f, textSize = 96, fontScale = 1f, width = 40, height = 100)
        checkFillTypography(20f, textSize = 96, fontScale = 1f, width = 300, height = 24)
        checkFillTypography(83.333f, textSize = 96, fontScale = 2f, width = 300, height = 200)
        checkFillTypography(50f, textSize = 0, fontScale = 1f, width = 50, height = 100)
        checkFillTypography(0.833f, textSize = 0, fontScale = 1f, width = 0, height = -1)
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
    ): StreamState = numericStateFrom(state, caloriesPerBeer, DATA_TYPE_ID)

    private fun streaming(calories: Double): StreamState.Streaming = StreamState.Streaming(
        DataPoint(
            dataTypeId = DataType.Type.CALORIES,
            values = mapOf(DataType.Field.SINGLE to calories),
        ),
    )

    private fun steady(completed: Int, bucket: Int) = render(
        PintFrame.Steady(PintProgress(completed, bucket)),
    )
    private fun animation(completed: Int) = PintViewUpdate.BeginTransition(
        transientFrames = listOf(
            TimedFrame(0, PintFrame.FullBubbles(completed)),
            TimedFrame(PintProgressReducer.ANIMATION_STEP_MILLIS, PintFrame.Draining(completed)),
        ),
        steadyDelayMillis = PintProgressReducer.ANIMATION_STEP_MILLIS,
        steady = PintFrame.Steady(PintProgress(completed, 0)),
    )
    private fun render(frame: PintFrame) = PintViewUpdate.Render(frame)
    private fun calories(value: Double): StreamState = StreamState.Streaming(DataPoint(
        dataTypeId = "TYPE_CALORIES_ID",
        values = mapOf("FIELD_CALORIES_ID" to value),
    ))

    private fun size(
        view: Pair<Int, Int>,
        density: Float,
        screen: Pair<Int, Int> = 480 to 800,
    ) = resolveFieldSize(view, 30 to 15, screen, density)
    private companion object {
        const val DATA_TYPE_ID = "pintprogress:pint-progress-text"
        val idle = StreamState.Idle
        val unavailableState = StreamState.NotAvailable
        val searching = StreamState.Searching
        val unavailable = PintViewUpdate.Render(PintFrame.Unavailable)
    }
}
