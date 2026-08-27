package io.ericchernuka.pintprogress.core

import io.hammerhead.karooext.models.StreamState

/**
 * Converts Karoo stream states into visible render plans synchronously so product decisions stay
 * deterministic and duplicate visible states are suppressed
 */
class PintViewReducer {
    private var previous: PintProgress? = null
    private var previousCaloriesPerBeer: Int? = null

    fun accept(
        state: StreamState,
        caloriesPerBeer: Int = BeerCaloriesPolicy.DEFAULT,
    ): RenderPlan? {
        val normalizedTarget = BeerCaloriesPolicy.normalize(caloriesPerBeer)
        val previousTarget = previousCaloriesPerBeer
        val targetChanged = previousTarget != null && normalizedTarget != previousTarget
        val current = progressFrom(state, normalizedTarget)
        if (previousTarget != null && current == previous) {
            previousCaloriesPerBeer = normalizedTarget
            return null
        }

        // Treat a settings change as a new baseline to prevent a false threshold celebration
        val plan = PintProgressReducer.plan(if (targetChanged) null else previous, current)
        previous = current
        previousCaloriesPerBeer = normalizedTarget
        return plan
    }
}

fun graphicalPreviewFrames(): List<PintFrame> = listOf(
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
