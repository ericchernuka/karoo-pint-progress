package io.ericchernuka.pintprogress.core

import io.hammerhead.karooext.models.StreamState

/**
 * Converts Karoo stream states into visible render plans and suppresses duplicate visible states.
 * It is synchronous so all product decisions are deterministic and inexpensive to test.
 */
class PintViewReducer {
    private var initialized = false
    private var previous: PintProgress? = null

    fun accept(state: StreamState): RenderPlan? {
        val current = progressFrom(state)
        if (initialized && current == previous) return null

        val plan = PintProgressReducer.plan(previous, current)
        previous = current
        initialized = true
        return plan
    }

    companion object {
        const val PREVIEW_BUCKET = 12

        fun previewFrame(): PintFrame.Steady = PintFrame.Steady(PintProgress(0, PREVIEW_BUCKET))

        private fun progressFrom(state: StreamState): PintProgress? = when (state) {
            is StreamState.Streaming -> PintProgressReducer.progressFor(state.dataPoint.singleValue)
            StreamState.Idle,
            StreamState.NotAvailable,
            StreamState.Searching,
            -> null
        }
    }
}
