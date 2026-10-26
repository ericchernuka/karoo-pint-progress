package io.ericchernuka.pintprogress.core

import io.hammerhead.karooext.models.StreamState

const val VIEW_UPDATE_INTERVAL_MILLIS = 1_000L

/**
 * Owns every decision about which graphical frame is shown and when: baselines, target changes,
 * the completion transition, same-pint refreshes, duplicate suppression, and the one-Hz cap.
 * Callers supply the clock, so the whole sequence stays deterministic
 */
class PintFrameScheduler {
    private var previous: PintProgress? = null
    private var previousCaloriesPerBeer: Int? = null
    private var pending: List<TimedFrame> = emptyList()
    private var anchorMillis = 0L
    private var lastEmissionMillis: Long? = null

    fun accept(state: StreamState, caloriesPerBeer: Int, nowMillis: Long) {
        val normalizedTarget = BeerCaloriesPolicy.normalize(caloriesPerBeer)
        val previousTarget = previousCaloriesPerBeer
        previousCaloriesPerBeer = normalizedTarget
        val targetChanged = previousTarget != null && normalizedTarget != previousTarget
        val current = progressFrom(state, normalizedTarget)
        if (!targetChanged && previousTarget != null && current == previous) return

        // Treat a settings change as a new baseline to prevent a false threshold celebration
        val plan = PintProgressReducer.plan(if (targetChanged) null else previous, current)
        previous = current
        val refreshed = plan.singleOrNull()?.frame as? PintFrame.Steady
        val pendingSteady = pending.lastOrNull()?.frame as? PintFrame.Steady
        if (
            !targetChanged &&
            refreshed != null &&
            pendingSteady != null &&
            refreshed.progress.completed == pendingSteady.progress.completed
        ) {
            // A same-pint update keeps the completion transition running and refreshes where it lands
            pending = pending.dropLast(1) + pending.last().copy(frame = refreshed)
        } else {
            pending = plan
            anchorMillis = nowMillis
        }
    }

    /** Milliseconds until the next frame is due, or null when nothing is pending */
    fun delayMillis(nowMillis: Long): Long? {
        val next = pending.firstOrNull() ?: return null
        val pacedMillis = lastEmissionMillis?.plus(VIEW_UPDATE_INTERVAL_MILLIS) ?: Long.MIN_VALUE
        return (maxOf(anchorMillis + next.delayMillis, pacedMillis) - nowMillis).coerceAtLeast(0)
    }

    /** Removes the next frame once [delayMillis] has elapsed and records it as emitted */
    fun take(nowMillis: Long): PintFrame {
        val next = pending.first()
        pending = pending.drop(1)
        anchorMillis = nowMillis
        lastEmissionMillis = nowMillis
        return next.frame
    }
}

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
