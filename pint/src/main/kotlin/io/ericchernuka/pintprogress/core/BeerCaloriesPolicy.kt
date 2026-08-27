package io.ericchernuka.pintprogress.core

import kotlin.math.roundToInt

/** Domain policy for the user-configurable calories represented by one full beer */
object BeerCaloriesPolicy {
    const val DEFAULT = 150
    const val MIN = 80
    const val MAX = 400
    const val STEP = 5
    const val STEP_COUNT = (MAX - MIN) / STEP

    fun normalize(value: Int): Int =
        MIN + ((value.coerceIn(MIN, MAX) - MIN).toFloat() / STEP).roundToInt() * STEP

    fun fromSliderProgress(progress: Int): Int =
        MIN + progress.coerceIn(0, STEP_COUNT) * STEP

    fun toSliderProgress(value: Int): Int =
        (normalize(value) - MIN) / STEP
}
