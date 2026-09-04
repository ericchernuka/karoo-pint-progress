package io.ericchernuka.pintprogress.core

import io.hammerhead.karooext.models.StreamState

sealed interface PintViewUpdate {
    data class Render(val frame: PintFrame) : PintViewUpdate

    data class BeginTransition(
        val transientFrames: List<TimedFrame>,
        val steadyDelayMillis: Long,
        val steady: PintFrame.Steady,
    ) : PintViewUpdate

    data class RefreshTransition(
        val steady: PintFrame.Steady,
    ) : PintViewUpdate
}

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
        activeTransitionCompleted: Int? = null,
    ): PintViewUpdate? {
        val normalizedTarget = BeerCaloriesPolicy.normalize(caloriesPerBeer)
        val previousTarget = previousCaloriesPerBeer
        val targetChanged = previousTarget != null && normalizedTarget != previousTarget
        val current = progressFrom(state, normalizedTarget)
        if (!targetChanged && previousTarget != null && current == previous) {
            previousCaloriesPerBeer = normalizedTarget
            return null
        }

        // Treat a settings change as a new baseline to prevent a false threshold celebration
        val plan = PintProgressReducer.plan(if (targetChanged) null else previous, current)
        previous = current
        previousCaloriesPerBeer = normalizedTarget
        if (plan.size > 1) {
            val steady = plan.last().frame as PintFrame.Steady
            return PintViewUpdate.BeginTransition(
                transientFrames = plan.dropLast(1),
                steadyDelayMillis = plan.last().delayMillis,
                steady = steady,
            )
        }

        val timedFrame = plan.single()
        val steady = timedFrame.frame as? PintFrame.Steady
        return if (
            !targetChanged &&
            activeTransitionCompleted != null &&
            steady?.progress?.completed == activeTransitionCompleted
        ) {
            PintViewUpdate.RefreshTransition(steady)
        } else {
            PintViewUpdate.Render(timedFrame.frame)
        }
    }
}

fun fillPreviewFrames(): List<PintFrame> = listOf(
    PintFrame.Steady(PintProgress(completed = 0, fillBucket = 10)),
    PintFrame.Steady(PintProgress(completed = 0, fillBucket = 16)),
    PintFrame.FullBubbles(completed = 1),
    PintFrame.Draining(completed = 1),
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
