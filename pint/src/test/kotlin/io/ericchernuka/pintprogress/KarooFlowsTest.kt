package io.ericchernuka.pintprogress

import io.hammerhead.karooext.models.StreamState
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import org.junit.Assert.assertEquals
import org.junit.Test

class KarooFlowsTest {
    @Test
    fun `normal states are forwarded in order`() = runBlocking {
        val callbacks = CallbackCapture()
        val states = mutableListOf<StreamState>()
        val flow = streamDataFlow(callbacks::register) { callbacks.unregister(it) }
        val collection = launchCollection(flow, states)
        callbacks.registered.await()

        callbacks.state(StreamState.Idle)
        callbacks.state(StreamState.Searching)
        // Allow callbackFlow deliveries to reach the collector before cancellation.
        yield()
        collection.cancelAndJoin()

        assertEquals(listOf(StreamState.Idle, StreamState.Searching), states)
        assertEquals(1, callbacks.unregisterCount)
    }

    @Test
    fun `completion emits one unavailable state terminates and unregisters once`() = runBlocking {
        val callbacks = CallbackCapture()
        val states = mutableListOf<StreamState>()
        val flow = streamDataFlow(callbacks::register) { callbacks.unregister(it) }
        val collection = launchCollection(flow, states)
        callbacks.registered.await()

        callbacks.complete()
        callbacks.complete()
        collection.join()

        assertEquals(listOf(StreamState.NotAvailable), states)
        assertEquals(1, callbacks.unregisterCount)
    }

    @Test
    fun `error emits one unavailable state terminates and unregisters once without retaining message`() = runBlocking {
        val callbacks = CallbackCapture()
        val states = mutableListOf<StreamState>()
        val flow = streamDataFlow(callbacks::register) { callbacks.unregister(it) }
        val collection = launchCollection(flow, states)
        callbacks.registered.await()

        callbacks.error("host error text")
        callbacks.error("duplicate host error text")
        collection.join()

        assertEquals(listOf(StreamState.NotAvailable), states)
        assertEquals(1, callbacks.unregisterCount)
    }

    @Test
    fun `cancellation unregisters once`() = runBlocking {
        val callbacks = CallbackCapture()
        val flow = streamDataFlow(callbacks::register) { callbacks.unregister(it) }
        val collection = launch {
            flow.toList()
        }
        callbacks.registered.await()

        collection.cancelAndJoin()

        assertEquals(1, callbacks.unregisterCount)
    }

    private fun CoroutineScope.launchCollection(
        flow: kotlinx.coroutines.flow.Flow<StreamState>,
        states: MutableList<StreamState>,
    ): Job =
        launch {
            flow.toList(states)
        }

    private class CallbackCapture {
        val registered = CompletableDeferred<Unit>()
        private var onError: ((String) -> Unit)? = null
        private var onComplete: (() -> Unit)? = null
        private var onState: ((StreamState) -> Unit)? = null
        private var listenerId: String? = null
        var unregisterCount = 0
            private set

        fun register(
            onError: (String) -> Unit,
            onComplete: () -> Unit,
            onState: (StreamState) -> Unit,
        ): String {
            this.onError = onError
            this.onComplete = onComplete
            this.onState = onState
            val id = "listener-id"
            listenerId = id
            registered.complete(Unit)
            return id
        }

        fun unregister(listenerId: String) {
            assertEquals(this.listenerId, listenerId)
            unregisterCount += 1
        }

        fun state(state: StreamState) {
            onState?.invoke(state)
        }

        fun complete() {
            onComplete?.invoke()
        }

        fun error(message: String) {
            onError?.invoke(message)
        }
    }
}
