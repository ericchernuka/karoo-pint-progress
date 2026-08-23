package io.ericchernuka.pintprogress

import io.hammerhead.karooext.models.StreamState
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class KarooFlowsTest {
    @Test
    fun `normal states are forwarded in order and cancellation cleans up`() = runBlocking {
        val callbacks = CallbackCapture()
        val states = mutableListOf<StreamState>()
        val collection = launchCollection(callbacks.flow(), states)
        callbacks.registered.await()

        callbacks.state(StreamState.Idle)
        callbacks.state(StreamState.Searching)
        yield()
        collection.cancelAndJoin()

        assertEquals(listOf(StreamState.Idle, StreamState.Searching), states)
        assertEquals(1, callbacks.adapterRemovalAttempts)
        assertFalse(callbacks.active)
    }

    @Test
    fun `completion is visible finite and safe with sdk terminal cleanup`() = runBlocking {
        val callbacks = CallbackCapture()
        val states = mutableListOf<StreamState>()
        val collection = launchCollection(callbacks.flow(), states)
        callbacks.registered.await()

        callbacks.complete()
        callbacks.complete()
        callbacks.state(StreamState.Searching)
        collection.join()

        assertEquals(listOf(StreamState.NotAvailable), states)
        assertEquals(1, callbacks.adapterRemovalAttempts)
        assertEquals(2, callbacks.sdkRemovalAttempts)
        assertFalse(callbacks.active)
    }

    @Test
    fun `error is visible finite and does not retain its message`() = runBlocking {
        val callbacks = CallbackCapture()
        val states = mutableListOf<StreamState>()
        val collection = launchCollection(callbacks.flow(), states)
        callbacks.registered.await()

        callbacks.error("host error text")
        callbacks.error("different host error text")
        collection.join()

        assertEquals(listOf(StreamState.NotAvailable), states)
        assertEquals(1, callbacks.adapterRemovalAttempts)
        assertEquals(2, callbacks.sdkRemovalAttempts)
        assertFalse(callbacks.active)
    }

    @Test
    fun `synchronous terminal callback during registration still cleans up`() = runBlocking {
        val callbacks = CallbackCapture(terminalDuringRegistration = true)

        assertEquals(listOf(StreamState.NotAvailable), callbacks.flow().toList())
        assertEquals(1, callbacks.adapterRemovalAttempts)
        assertEquals(1, callbacks.sdkRemovalAttempts)
        assertFalse(callbacks.active)
    }

    private fun CoroutineScope.launchCollection(
        flow: Flow<StreamState>,
        states: MutableList<StreamState>,
    ): Job = launch { flow.toList(states) }

    private class CallbackCapture(
        private val terminalDuringRegistration: Boolean = false,
    ) {
        val registered = CompletableDeferred<Unit>()
        private var onError: ((String) -> Unit)? = null
        private var onComplete: (() -> Unit)? = null
        private var onState: ((StreamState) -> Unit)? = null
        private var listenerId: String? = null
        var adapterRemovalAttempts = 0
            private set
        var sdkRemovalAttempts = 0
            private set
        var active = false
            private set

        fun flow(): Flow<StreamState> = streamDataFlow(::register, ::adapterRemove)

        private fun register(
            onError: (String) -> Unit,
            onComplete: () -> Unit,
            onState: (StreamState) -> Unit,
        ): String {
            this.onError = onError
            this.onComplete = onComplete
            this.onState = onState
            listenerId = LISTENER_ID
            active = true
            registered.complete(Unit)
            if (terminalDuringRegistration) complete()
            return LISTENER_ID
        }

        fun state(state: StreamState) {
            onState?.invoke(state)
        }

        fun complete() {
            onComplete?.invoke()
            sdkRemove()
        }

        fun error(message: String) {
            onError?.invoke(message)
            sdkRemove()
        }

        private fun adapterRemove(listenerId: String) {
            assertEquals(this.listenerId, listenerId)
            adapterRemovalAttempts += 1
            active = false
        }

        private fun sdkRemove() {
            sdkRemovalAttempts += 1
            active = false
        }

        private companion object {
            const val LISTENER_ID = "listener-id"
        }
    }
}
