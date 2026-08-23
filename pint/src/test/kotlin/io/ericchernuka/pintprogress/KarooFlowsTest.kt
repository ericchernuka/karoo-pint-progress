package io.ericchernuka.pintprogress

import io.hammerhead.karooext.models.StreamState
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
        yield()

        callbacks.state(StreamState.Idle)
        callbacks.state(StreamState.Searching)
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
        yield()

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
        yield()

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
        yield()

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
            return "listener-id".also { listenerId = it }
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
