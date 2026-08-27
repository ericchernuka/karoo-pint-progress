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
        assertEquals(2, edgeInsetDp(false))
        assertEquals(6, edgeInsetDp(true))
        for (bucket in 0..19) {
            val display = displayFor(PintFrame.Steady(PintProgress(2, bucket)))
            assertEquals(PintAsset.entries[bucket], display.first)
            assertEquals("2", display.second)
        }
        assertEquals(
            PintAsset.UNAVAILABLE to "",
            displayFor(PintFrame.Unavailable),
        )
        assertEquals(
            PintAsset.FULL_BUBBLES to "3",
            displayFor(PintFrame.FullBubbles(3)),
        )
        assertEquals(
            PintAsset.DRAINING to "3",
            displayFor(PintFrame.Draining(3)),
        )
        listOf(0 to "", 1 to "1").forEach { (completed, expected) ->
            val display = displayFor(PintFrame.Steady(PintProgress(completed, 0)))
            assertEquals(expected, display.second)
        }
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

        val bucketReducer = PintViewReducer()
        assertEquals(steady(0, 0), bucketReducer.accept(calories(1.0)))
        assertEquals(null, bucketReducer.accept(calories(1.0), 400))
        assertEquals(steady(0, 1), bucketReducer.accept(calories(20.0), 400))
        listOf(idle, unavailableState, searching).forEach {
            assertEquals(unavailable, PintViewReducer().accept(it))
        }
        assertEquals(steady(0, 10), PintViewReducer().accept(calories(75.0)))
        fun check(expected: PintFieldLayout, preview: Boolean, width: Int, height: Int, boundaries: Boolean) =
            assertEquals(expected, PintFieldLayout.forSize(preview, width, height, boundaries))

        check(PintFieldLayout.PICKER, true, 480, 200, false)
        check(PintFieldLayout.PICKER, true, 20, 20, true)
        check(PintFieldLayout.REGULAR, false, 184, 93, false)
        check(PintFieldLayout.COMPACT, false, 183, 93, false)
        check(PintFieldLayout.COMPACT, false, 184, 92, false)
        check(PintFieldLayout.COMPACT, false, 112, 69, false)
        check(PintFieldLayout.ICON_ONLY, false, 111, 69, false)
        check(PintFieldLayout.ICON_ONLY, false, 112, 68, false)
        check(PintFieldLayout.ICON_ONLY, false, -1, -1, false)
        check(PintFieldLayout.REGULAR, false, 192, 101, true)
        check(PintFieldLayout.COMPACT, false, 184, 93, true)
        check(PintFieldLayout.COMPACT, false, 120, 77, true)
        check(PintFieldLayout.ICON_ONLY, false, 112, 69, true)
        fun checkDisplay(layout: PintFieldLayout, count: Boolean, expected: PintFieldLayout) =
            assertEquals(expected, layout.forDisplay(count))
        checkDisplay(PintFieldLayout.REGULAR, false, PintFieldLayout.ICON_ONLY)
        checkDisplay(PintFieldLayout.COMPACT, false, PintFieldLayout.ICON_ONLY)
        checkDisplay(PintFieldLayout.ICON_ONLY, false, PintFieldLayout.ICON_ONLY)
        checkDisplay(PintFieldLayout.REGULAR, true, PintFieldLayout.REGULAR)
        checkDisplay(PintFieldLayout.COMPACT, true, PintFieldLayout.COMPACT)
        checkDisplay(PintFieldLayout.ICON_ONLY, true, PintFieldLayout.ICON_ONLY)
        checkDisplay(PintFieldLayout.PICKER, false, PintFieldLayout.PICKER)
        checkDisplay(PintFieldLayout.PICKER, true, PintFieldLayout.PICKER)
        PintFieldLayout.entries.forEach {
            assertEquals(if (it == PintFieldLayout.REGULAR || it == PintFieldLayout.COMPACT) "12" else "", it.visibleCount("12"))
        }
        assertEquals(480 to 200, size(480 to 200, 1f))
        assertEquals(481 to 201, size(961 to 401, 2f))
        assertEquals(120 to 200, size(0 to 400, 2f))
        assertEquals(200 to 100, size(400 to 0, 2f))
        listOf(0f, -2f, Float.NaN, Float.POSITIVE_INFINITY).forEach {
            assertEquals(480 to 200, size(480 to 200, it))
        }
        fun checkTypography(
            expected: Pair<Float, Float>,
            layout: PintFieldLayout,
            textSize: Int,
            countLength: Int,
            fontScale: Float = 1f,
            width: Int = 240,
        ) {
            val actual = requireNotNull(resolveTypography(layout, textSize, countLength, fontScale, width))
            assertEquals(expected.first, actual.first, 0.001f)
            assertEquals(expected.second, actual.second, 0.001f)
        }
        checkTypography(92f to 43f, PintFieldLayout.REGULAR, 92, 2)
        checkTypography(68f to 32f, PintFieldLayout.COMPACT, 68, 2)
        checkTypography(110f to 52f, PintFieldLayout.REGULAR, 999, 3)
        checkTypography(68f to 32f, PintFieldLayout.COMPACT, 0, 0)
        checkTypography(55f to 26f, PintFieldLayout.REGULAR, 999, 2, 2f)
        checkTypography(92.56198f to 44f, PintFieldLayout.REGULAR, 999, 3, width = 180)
        checkTypography(48.76033f to 23f, PintFieldLayout.COMPACT, 999, 3, width = 108)
        checkTypography(20f to 9f, PintFieldLayout.COMPACT, 20, 4, width = 108)
        checkTypography(110f to 52f, PintFieldLayout.REGULAR, 999, 2, width = 180)
        checkTypography(1.9607843f to 1f, PintFieldLayout.REGULAR, 999, -1, width = 0)
        listOf(0f, -1f, Float.NaN, Float.POSITIVE_INFINITY).forEach {
            checkTypography(110f to 52f, PintFieldLayout.REGULAR, 999, 2, it)
        }
        assertNull(resolveTypography(PintFieldLayout.PICKER, 110, 2, 1f, 240))
        assertNull(resolveTypography(PintFieldLayout.ICON_ONLY, 68, 2, 1f, 240))
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

    private fun steady(completed: Int, bucket: Int) = plan(PintFrame.Steady(PintProgress(completed, bucket)))
    private fun animation(completed: Int) = listOf(
        TimedFrame(0, PintFrame.FullBubbles(completed)),
        TimedFrame(PintProgressReducer.ANIMATION_STEP_MILLIS, PintFrame.Draining(completed)),
        TimedFrame(PintProgressReducer.ANIMATION_STEP_MILLIS, PintFrame.Steady(PintProgress(completed, 0))),
    )
    private fun plan(frame: PintFrame) = listOf(TimedFrame(0, frame))
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
        val unavailable = listOf(TimedFrame(0, PintFrame.Unavailable))
    }
}
