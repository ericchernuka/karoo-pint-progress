package io.ericchernuka.pintprogress.core

import java.math.BigDecimal
import java.math.RoundingMode

/** The default target is a typical 12 fl oz / 355 ml, 5% ABV beer. */
const val DEFAULT_BEER_CALORIES: Double = 150.0

/**
 * The visible state deliberately has 20 fill levels. It avoids an IPC update for every calorie
 * while still reading as a smoothly filling mug at a glance.
 */
data class PintProgress(
    val completed: Int,
    val fillBucket: Int,
) {
    init {
        require(completed >= 0)
        require(fillBucket in 0..19)
    }
}

sealed interface PintFrame {
    data object Unavailable : PintFrame

    data class Steady(val progress: PintProgress) : PintFrame

    data class FullBubbles(val completed: Int) : PintFrame

    data class Draining(val completed: Int) : PintFrame
}

data class TimedFrame(
    val delayMillis: Long,
    val frame: PintFrame,
)

data class RenderPlan(val frames: List<TimedFrame>)

/** Pure decision engine for the field. Android-specific code only renders its output. */
object PintProgressReducer {
    private const val BUCKETS_PER_PINT = 20
    private val bucketsPerPint = BigDecimal.valueOf(BUCKETS_PER_PINT.toLong())
    private val beerCalories = BigDecimal.valueOf(DEFAULT_BEER_CALORIES)
    private val maximumBucketCount = BigDecimal.valueOf(Int.MAX_VALUE.toLong())
        .multiply(bucketsPerPint)

    const val ANIMATION_STEP_MILLIS = 1_000L

    fun progressFor(calories: Double?): PintProgress? {
        if (calories == null || !calories.isFinite() || calories < 0.0) return null

        // Work in whole 5% buckets before splitting them into full and partial pints. This keeps
        // decimal boundaries such as 180 kcal (one pint plus 20%) from underflowing after a
        // floating-point subtraction.
        val totalBuckets = BigDecimal.valueOf(calories)
            .multiply(bucketsPerPint)
            .divide(beerCalories, 0, RoundingMode.FLOOR)
        if (totalBuckets >= maximumBucketCount) {
            return PintProgress(completed = Int.MAX_VALUE, fillBucket = 0)
        }

        val (completed, fillBucket) = totalBuckets.divideAndRemainder(bucketsPerPint)

        return PintProgress(completed = completed.toInt(), fillBucket = fillBucket.toInt())
    }

    /**
     * Animate only a single, observed threshold crossing. This prevents a celebration on first
     * attach, a resumed field, a data-source reset, or a skipped threshold.
     */
    fun plan(previous: PintProgress?, current: PintProgress?): RenderPlan {
        if (current == null) return RenderPlan(listOf(TimedFrame(0, PintFrame.Unavailable)))

        return if (previous != null && current.completed == previous.completed + 1) {
            RenderPlan(
                listOf(
                    TimedFrame(0, PintFrame.FullBubbles(current.completed)),
                    TimedFrame(ANIMATION_STEP_MILLIS, PintFrame.Draining(current.completed)),
                    TimedFrame(ANIMATION_STEP_MILLIS, PintFrame.Steady(current)),
                ),
            )
        } else {
            RenderPlan(listOf(TimedFrame(0, PintFrame.Steady(current))))
        }
    }
}
