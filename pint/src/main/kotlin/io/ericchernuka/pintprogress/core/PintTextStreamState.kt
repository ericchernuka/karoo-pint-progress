package io.ericchernuka.pintprogress.core

import io.hammerhead.karooext.models.DataPoint
import io.hammerhead.karooext.models.DataType
import io.hammerhead.karooext.models.StreamState

/** Converts Karoo calorie states into the native numeric Pints (Text) stream. */
object PintTextStreamState {
    fun from(
        source: StreamState,
        caloriesPerBeer: Int,
        dataTypeId: String,
    ): StreamState = when (source) {
        is StreamState.Streaming -> PintProgressReducer.progressFor(
            calories = source.dataPoint.singleValue,
            caloriesPerBeer = caloriesPerBeer,
        )?.let { progress ->
            StreamState.Streaming(
                DataPoint(
                    dataTypeId = dataTypeId,
                    values = mapOf(DataType.Field.SINGLE to progress.decimalPints()),
                ),
            )
        } ?: StreamState.NotAvailable

        StreamState.Idle -> StreamState.Idle
        StreamState.NotAvailable -> StreamState.NotAvailable
        StreamState.Searching -> StreamState.Searching
    }

    private fun PintProgress.decimalPints(): Double =
        completed.toDouble() + (fillBucket / BUCKETS_PER_TENTH) / 10.0

    private const val BUCKETS_PER_TENTH = 2
}
