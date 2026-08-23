package io.ericchernuka.pintprogress

import io.hammerhead.karooext.KarooSystemService
import io.hammerhead.karooext.models.OnStreamState
import io.hammerhead.karooext.models.StreamState
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.util.concurrent.atomic.AtomicBoolean

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

/**
 * Adapts the SDK consumer into a finite Flow. The SDK removes its listener after a terminal
 * callback; awaitClose deliberately performs the same idempotent removal to cover cancellation and
 * a terminal/cancellation race without depending on callback ordering.
 */
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
