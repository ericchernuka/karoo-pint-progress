package io.ericchernuka.pintprogress.core

import io.hammerhead.karooext.models.StreamState

/**
 * Converts Karoo stream states into visible render plans and suppresses duplicate visible states.
 * It is synchronous so all product decisions are deterministic and inexpensive to test.
 */
class PintViewReducer {
    private var initialized = false
    private var previous: PintProgress? = null
    private var previousCaloriesPerBeer = BeerCaloriesPolicy.DEFAULT

    fun accept(
        state: StreamState,
        caloriesPerBeer: Int = BeerCaloriesPolicy.DEFAULT,
    ): RenderPlan? {
        val normalizedTarget = BeerCaloriesPolicy.normalize(caloriesPerBeer)
        val targetChanged = initialized && normalizedTarget != previousCaloriesPerBeer
        val current = progressFrom(state, normalizedTarget)
        if (initialized && current == previous) {
            previousCaloriesPerBeer = normalizedTarget
            return null
        }

        // A settings change is a new baseline, not a calorie threshold crossing. Rendering it as
        // steady prevents a false full/drain celebration when the chosen target changes mid-ride.
        val plan = PintProgressReducer.plan(if (targetChanged) null else previous, current)
        previous = current
        previousCaloriesPerBeer = normalizedTarget
        initialized = true
        return plan
    }

    companion object {
        fun previewFrames(): List<PintFrame> = listOf(
            PintFrame.Steady(PintProgress(completed = 0, fillBucket = 10)),
            PintFrame.Steady(PintProgress(completed = 0, fillBucket = 16)),
            PintFrame.FullBubbles(completed = 0),
        )

        private fun progressFrom(state: StreamState, caloriesPerBeer: Int): PintProgress? = when (state) {
            is StreamState.Streaming -> PintProgressReducer.progressFor(
                state.dataPoint.singleValue,
                caloriesPerBeer,
            )
            StreamState.Idle,
            StreamState.NotAvailable,
            StreamState.Searching,
            -> null
        }
    }
}
