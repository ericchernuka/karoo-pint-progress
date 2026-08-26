package io.ericchernuka.pintprogress

import io.ericchernuka.pintprogress.core.PintFrame
import io.ericchernuka.pintprogress.core.PintTextStreamState
import io.ericchernuka.pintprogress.core.PintViewReducer
import io.hammerhead.karooext.models.StreamState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.collect

/** Runs the deterministic scheduling policy used by the Android data-field adapter. */
internal class PintDataFieldRuntime(
    private val nowMillis: () -> Long,
    private val waitMillis: suspend (Long) -> Unit,
) {
    suspend fun runNumericStream(
        calorieStates: Flow<StreamState>,
        caloriesPerBeer: Flow<Int>,
        dataTypeId: String,
        emit: suspend (StreamState) -> Unit,
    ) {
        var lastEmissionMillis: Long? = null
        calorieStates
            .combine(caloriesPerBeer) { state, target ->
                PintTextStreamState.from(state, target, dataTypeId)
            }
            .distinctUntilChanged()
            .conflate()
            .collect { state ->
                pace(lastEmissionMillis)?.let { waitMillis(it) }
                emit(state)
                lastEmissionMillis = nowMillis()
            }
    }

    suspend fun runNumericPreview(emit: suspend (String) -> Unit) {
        while (true) {
            PintTextStreamState.previewMessages().forEach { message ->
                emit(message)
                waitMillis(VIEW_UPDATE_INTERVAL_MILLIS)
            }
        }
    }

    suspend fun runGraphicalStream(
        calorieStates: Flow<StreamState>,
        caloriesPerBeer: Flow<Int>,
        emit: suspend (PintFrame) -> Unit,
    ) {
        val reducer = PintViewReducer()
        var lastEmissionMillis: Long? = null
        calorieStates
            .combine(caloriesPerBeer) { state, target -> state to target }
            .conflate()
            .collect { (state, target) ->
                reducer.accept(state, target)?.frames?.forEach { timedFrame ->
                    if (timedFrame.delayMillis > 0) waitMillis(timedFrame.delayMillis)
                    pace(lastEmissionMillis)?.let { waitMillis(it) }
                    emit(timedFrame.frame)
                    lastEmissionMillis = nowMillis()
                }
            }
    }

    suspend fun runGraphicalPreview(emit: suspend (PintFrame) -> Unit) {
        while (true) {
            PintViewReducer.previewFrames().forEach { frame ->
                emit(frame)
                waitMillis(VIEW_UPDATE_INTERVAL_MILLIS)
            }
        }
    }

    private fun pace(lastEmissionMillis: Long?): Long? = lastEmissionMillis?.let { last ->
        (VIEW_UPDATE_INTERVAL_MILLIS - (nowMillis() - last)).takeIf { it > 0 }
    }

    private companion object {
        const val VIEW_UPDATE_INTERVAL_MILLIS = 1_000L
    }
}
