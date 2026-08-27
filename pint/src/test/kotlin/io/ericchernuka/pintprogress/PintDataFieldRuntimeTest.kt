package io.ericchernuka.pintprogress

import io.ericchernuka.pintprogress.core.PintFrame
import io.ericchernuka.pintprogress.core.PintProgress
import io.hammerhead.karooext.models.DataPoint
import io.hammerhead.karooext.models.DataType
import io.hammerhead.karooext.models.StreamState
import kotlinx.coroutines.Job
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PintDataFieldRuntimeTest {
    @Test
    fun `style and preview state select each cancellation route`() {
        assertEquals("numeric-live", PintFieldStyle.TEXT.cancellationLabel(preview = false))
        assertEquals("numeric-preview", PintFieldStyle.TEXT.cancellationLabel(preview = true))
        assertEquals("graphical-live", PintFieldStyle.MUG.cancellationLabel(preview = false))
        assertEquals("graphical-preview", PintFieldStyle.MUG.cancellationLabel(preview = true))
    }

    @Test
    fun `numeric runtime covers stream pacing conflation completion and preview`() = runTest {
        val source = MutableStateFlow(streaming(150.0))
        val target = MutableStateFlow(150)
        val output = mutableListOf<StreamState>()
        val times = mutableListOf<Long>()
        val job = backgroundScope.launch {
            runtime().runNumericStream(source, target, DATA_TYPE_ID) {
                output += it
                times += currentTime
            }
        }

        runCurrent()
        source.value = streaming(150.0)
        source.value = streaming(300.0)
        runCurrent()
        source.value = streaming(450.0)
        source.value = streaming(600.0)
        repeat(2) {
            advanceTimeBy(1_000)
            runCurrent()
        }
        job.cancelAndJoin()

        assertEquals(listOf(1.0, 2.0, 4.0), output.map { (it as StreamState.Streaming).dataPoint.singleValue })
        assertEquals(listOf(0L, 1_000L, 2_000L), times)

        val finite = mutableListOf<StreamState>()
        runtime().runNumericStream(flowOf(streaming(150.0)), flowOf(150), DATA_TYPE_ID) {
            finite += it
        }
        assertEquals(1, finite.size)
        assertEquals(
            listOf("0.5", "0.9", "1", "1.1", "0.5"),
            capturePreview(runtime().numericPreview(), 5),
        )
    }

    @Test
    fun `graphical runtime covers stream transitions cancellation and preview`() = runTest {
        val source = MutableSharedFlow<StreamState>(replay = 1, extraBufferCapacity = 4)
        val target = MutableStateFlow(150)
        val output = mutableListOf<PintFrame>()
        val times = mutableListOf<Long>()
        val job = backgroundScope.launch {
            runtime().runGraphicalStream(source, target) {
                output += it
                times += currentTime
            }
        }

        source.emit(streaming(149.0))
        runCurrent()
        source.emit(streaming(150.0))
        advanceSeconds(3)
        target.value = 300
        advanceSeconds(1)
        source.emit(streaming(150.0))
        runCurrent()
        target.value = 150
        advanceSeconds(1)
        source.emit(streaming(149.0))
        advanceSeconds(1)
        source.emit(streaming(150.0))
        advanceSeconds(1)
        job.cancelAndJoin()

        assertEquals(
            listOf(
                PintFrame.Steady(PintProgress(0, 19)),
                PintFrame.FullBubbles(1),
                PintFrame.Draining(1),
                PintFrame.Steady(PintProgress(1, 0)),
                PintFrame.Steady(PintProgress(0, 10)),
                PintFrame.Steady(PintProgress(1, 0)),
                PintFrame.Steady(PintProgress(0, 19)),
                PintFrame.FullBubbles(1),
            ),
            output,
        )
        assertEquals((0L..7_000L step 1_000L).toList(), times)
        assertEquals(
            listOf(
                PintFrame.Steady(PintProgress(0, 10)),
                PintFrame.Steady(PintProgress(0, 16)),
                PintFrame.FullBubbles(0),
                PintFrame.Steady(PintProgress(1, 10)),
                PintFrame.Steady(PintProgress(99, 10)),
                PintFrame.Steady(PintProgress(100, 10)),
                PintFrame.Steady(PintProgress(0, 10)),
            ),
            capturePreview(runtime().graphicalPreview(), 7),
        )

        val cancelled = mutableListOf<PintFrame>()
        val cancelledJob = backgroundScope.launch {
            runtime().graphicalPreview().collect { cancelled += it }
        }
        runCurrent()
        assertEquals(1, cancelled.size)
        cancelledJob.cancelAndJoin()
        advanceTimeBy(5_000)
        runCurrent()
        assertEquals(1, cancelled.size)
    }

    private suspend fun <T> TestScope.capturePreview(
        flow: Flow<T>,
        count: Int,
    ): List<T> {
        val start = currentTime
        val times = mutableListOf<Long>()
        val output = flow.onEach { times += currentTime }.take(count).toList()
        assertEquals(List(count) { start + it * 1_000L }, times)
        return output
    }

    private fun TestScope.runtime() = PintDataFieldRuntime(
        nowMillis = { currentTime },
    )

    private fun TestScope.advanceSeconds(count: Int) = repeat(count) {
        advanceTimeBy(1_000)
        runCurrent()
    }

    private fun streaming(calories: Double) = StreamState.Streaming(
        DataPoint(
            dataTypeId = DataType.Type.CALORIES,
            values = mapOf(DataType.Field.SINGLE to calories),
        ),
    )

    @Test
    fun `flow forwards normal and terminal states and always cleans up`() = runTest {
        val normalCallbacks = CallbackCapture()
        val normalStates = mutableListOf<StreamState>()
        val normalCollection = launchCollection(normalCallbacks.flow(), normalStates)
        runCurrent()

        normalCallbacks.state(StreamState.Idle)
        normalCallbacks.state(StreamState.Searching)
        runCurrent()
        normalCollection.cancelAndJoin()

        assertEquals(listOf(StreamState.Idle, StreamState.Searching), normalStates)
        assertEquals(1, normalCallbacks.adapterRemovalAttempts)
        listOf<(CallbackCapture) -> Unit>(
            { it.complete(); it.complete() },
            { it.error("host error text"); it.error("different host error text") },
        ).forEach { terminate ->
            val callbacks = CallbackCapture()
            val states = mutableListOf<StreamState>()
            val collection = launchCollection(callbacks.flow(), states)
            runCurrent()

            terminate(callbacks)
            callbacks.state(StreamState.Searching)
            runCurrent()
            collection.join()

            assertEquals(listOf(StreamState.NotAvailable), states)
            assertEquals(1, callbacks.adapterRemovalAttempts)
            assertEquals(2, callbacks.sdkRemovalAttempts)
        }
        val callbacks = CallbackCapture(terminalDuringRegistration = true)

        assertEquals(listOf(StreamState.NotAvailable), callbacks.flow().toList())
        assertEquals(1, callbacks.adapterRemovalAttempts)
        assertEquals(1, callbacks.sdkRemovalAttempts)
    }

    private fun CoroutineScope.launchCollection(
        flow: Flow<StreamState>,
        states: MutableList<StreamState>,
    ): Job = launch { flow.toList(states) }

    private class CallbackCapture(
        private val terminalDuringRegistration: Boolean = false,
    ) {
        private var onError: ((String) -> Unit)? = null
        private var onComplete: (() -> Unit)? = null
        private var onState: ((StreamState) -> Unit)? = null
        var adapterRemovalAttempts = 0
            private set
        var sdkRemovalAttempts = 0
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
            assertEquals(LISTENER_ID, listenerId)
            adapterRemovalAttempts += 1
        }

        private fun sdkRemove() {
            sdkRemovalAttempts += 1
        }

        private companion object {
            const val LISTENER_ID = "listener-id"
        }
    }

    private companion object {
        const val DATA_TYPE_ID = "pint-progress-text"
    }
}
