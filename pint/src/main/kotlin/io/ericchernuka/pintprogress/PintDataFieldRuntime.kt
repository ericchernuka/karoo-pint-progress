package io.ericchernuka.pintprogress

import android.os.SystemClock
import android.util.Log
import io.ericchernuka.pintprogress.core.OneHzPacer
import io.ericchernuka.pintprogress.core.PintFrame
import io.ericchernuka.pintprogress.core.PintFrameScheduler
import io.ericchernuka.pintprogress.core.VIEW_UPDATE_INTERVAL_MILLIS
import io.ericchernuka.pintprogress.core.numericPreviewMessages
import io.ericchernuka.pintprogress.core.numericStateFrom
import io.hammerhead.karooext.KarooSystemService
import io.hammerhead.karooext.internal.Emitter
import io.hammerhead.karooext.models.OnStreamState
import io.hammerhead.karooext.models.StreamState
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

internal fun deviceRuntime() = PintDataFieldRuntime(nowMillis = SystemClock::elapsedRealtime)

internal const val LOG_TAG = "PintProgressField"

internal fun Emitter<*>.launchCancellable(
    label: String,
    block: suspend CoroutineScope.() -> Unit,
) {
    val job = CoroutineScope(Dispatchers.Default).launch(start = CoroutineStart.LAZY, block = block)
    setCancellable {
        job.cancel()
        if (BuildConfig.DEBUG) Log.d(LOG_TAG, "cancellation label=$label")
    }
    job.start()
}

/** Runs the deterministic scheduling policy used by the Android data-field adapter */
internal class PintDataFieldRuntime(
    private val nowMillis: () -> Long,
) {
    suspend fun runNumericStream(
        calorieStates: Flow<StreamState>,
        caloriesPerBeer: Flow<Int>,
        dataTypeId: String,
        emit: suspend (StreamState) -> Unit,
    ) {
        val pacer = OneHzPacer()
        calorieStates
            .combine(caloriesPerBeer) { state, target ->
                numericStateFrom(state, target, dataTypeId)
            }
            .distinctUntilChanged()
            .conflate()
            .collect { state ->
                delay(pacer.delayMillis(nowMillis()))
                emit(state)
                pacer.emitted(nowMillis())
            }
    }

    fun numericPreview(): Flow<String> = preview(numericPreviewMessages())

    suspend fun runGraphicalStream(
        calorieStates: Flow<StreamState>,
        caloriesPerBeer: Flow<Int>,
        emit: suspend (PintFrame) -> Unit,
    ) {
        val scheduler = PintFrameScheduler()
        // collectLatest serializes scheduler access: each block starts only after the previous one ends
        calorieStates
            .combine(caloriesPerBeer) { state, target -> state to target }
            .conflate()
            .collectLatest { (state, target) ->
                scheduler.accept(state, target, nowMillis())
                while (true) {
                    delay(scheduler.delayMillis(nowMillis()) ?: break)
                    emit(scheduler.take(nowMillis()))
                }
            }
    }

    fun graphicalPreview(frames: List<PintFrame>): Flow<PintFrame> = preview(frames)

    private fun <T> preview(values: List<T>): Flow<T> = flow {
        while (true) {
            values.forEach { value ->
                emit(value)
                delay(VIEW_UPDATE_INTERVAL_MILLIS)
            }
        }
    }
}

internal fun KarooSystemService.streamDataFlow(dataTypeId: String): Flow<StreamState> =
    streamDataFlow(
        register = { onError, onComplete, onState ->
            addConsumer<OnStreamState>(
                params = OnStreamState.StartStreaming(dataTypeId),
                onError = onError,
                onComplete = onComplete,
                onEvent = { event -> onState(event.state) },
            )
        },
        unregister = ::removeConsumer,
    )

/** Adapts the SDK consumer into a finite Flow with idempotent cleanup for cancellation races */
internal fun streamDataFlow(
    register: (
        onError: (String) -> Unit,
        onComplete: () -> Unit,
        onState: (StreamState) -> Unit,
    ) -> String,
    unregister: (String) -> Unit,
): Flow<StreamState> = callbackFlow {
    val terminated = AtomicBoolean(false)

    fun terminate() {
        if (terminated.compareAndSet(false, true)) {
            trySend(StreamState.NotAvailable)
            close()
        }
    }

    val listenerId = register(
        { _: String -> terminate() },
        { terminate() },
        { state: StreamState ->
            if (!terminated.get()) trySend(state)
        },
    )
    awaitClose { unregister(listenerId) }
}
