package io.ericchernuka.pintprogress

import io.ericchernuka.pintprogress.core.PintFrame
import io.ericchernuka.pintprogress.core.PintProgress
import io.hammerhead.karooext.models.DataPoint
import io.hammerhead.karooext.models.DataType
import io.hammerhead.karooext.models.StreamState
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.yield
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PintDataFieldRuntimeTest {
    @Test
    fun `numeric stream converts distinct states and spaces emissions`() = runBlocking {
        val source = MutableSharedFlow<StreamState>(replay = 1, extraBufferCapacity = 8)
        val target = MutableStateFlow(150)
        val clock = TestClock()
        val output = mutableListOf<StreamState>()
        val firstEmitted = CompletableDeferred<Unit>()
        val secondEmitted = CompletableDeferred<Unit>()
        val runtime = runtime(clock)
        val job = launch {
            runtime.runNumericStream(source, target, DATA_TYPE_ID) {
                output += it
                firstEmitted.complete(Unit)
                if (output.size == 2) secondEmitted.complete(Unit)
            }
        }

        source.emit(streaming(150.0))
        awaitSignal(firstEmitted)
        source.emit(streaming(150.0))
        source.emit(streaming(300.0))
        awaitSignal(secondEmitted)
        job.cancelAndJoin()

        assertEquals(2, output.size)
        assertEquals(1.0, (output[0] as StreamState.Streaming).dataPoint.singleValue!!, 0.0)
        assertEquals(2.0, (output[1] as StreamState.Streaming).dataPoint.singleValue!!, 0.0)
        assertEquals(listOf(1_000L), clock.waits)
    }

    @Test
    fun `numeric stream conflates to latest pending state`() = runBlocking {
        val source = MutableStateFlow(streaming(150.0))
        val target = MutableStateFlow(150)
        val clock = TestClock()
        val firstStarted = CompletableDeferred<Unit>()
        val releaseFirst = CompletableDeferred<Unit>()
        val secondEmitted = CompletableDeferred<Unit>()
        val output = mutableListOf<StreamState>()
        val job = launch {
            runtime(clock).runNumericStream(source, target, DATA_TYPE_ID) {
                output += it
                if (output.size == 1) {
                    firstStarted.complete(Unit)
                    releaseFirst.await()
                }
                if (output.size == 2) secondEmitted.complete(Unit)
            }
        }

        firstStarted.await()
        source.value = streaming(225.0)
        source.value = streaming(300.0)
        releaseFirst.complete(Unit)
        awaitSignal(secondEmitted)
        job.cancelAndJoin()

        assertEquals(2, output.size)
        assertEquals(2.0, (output[1] as StreamState.Streaming).dataPoint.singleValue!!, 0.0)
        assertEquals(listOf(1_000L), clock.waits)
    }

    @Test
    fun `numeric preview repeats its ordered messages`() = runBlocking {
        val output = mutableListOf<String>()
        val fifthMessage = CompletableDeferred<Unit>()
        val job = launch {
            runtime(TestClock()).runNumericPreview {
                output += it
                if (output.size == 5) fifthMessage.complete(Unit)
            }
        }
        awaitSignal(fifthMessage)
        job.cancelAndJoin()

        assertEquals(listOf("0.5", "0.9", "1", "1.1", "0.5"), output)
    }

    @Test
    fun `numeric preview cancellation during wait prevents later callbacks`() = runBlocking {
        val output = mutableListOf<String>()
        val first = CompletableDeferred<Unit>()
        val waitStarted = CompletableDeferred<Unit>()
        val gate = CompletableDeferred<Unit>()
        val job = launch {
            PintDataFieldRuntime({ 0L }) { millis ->
                assertEquals(1_000L, millis)
                waitStarted.complete(Unit)
                gate.await()
            }.runNumericPreview {
                output += it
                first.complete(Unit)
            }
        }
        first.await()
        waitStarted.await()
        job.cancelAndJoin()
        gate.complete(Unit)

        assertEquals(listOf("0.5"), output)
    }

    @Test
    fun `graphical stream starts steady without a false completion`() = runBlocking {
        val source = MutableSharedFlow<StreamState>(replay = 1, extraBufferCapacity = 2)
        val target = MutableStateFlow(150)
        val output = mutableListOf<PintFrame>()
        val attached = CompletableDeferred<Unit>()
        val job = launch {
            runtime(TestClock()).runGraphicalStream(source, target) {
                output += it
                attached.complete(Unit)
            }
        }

        source.emit(streaming(150.0))
        attached.await()
        job.cancelAndJoin()

        assertEquals(listOf(PintFrame.Steady(PintProgress(1, 0))), output)
    }

    @Test
    fun `graphical stream executes full drain steady order`() = runBlocking {
        val source = MutableSharedFlow<StreamState>(replay = 1, extraBufferCapacity = 4)
        val target = MutableStateFlow(150)
        val clock = TestClock()
        val output = mutableListOf<PintFrame>()
        val firstRendered = CompletableDeferred<Unit>()
        val allRendered = CompletableDeferred<Unit>()
        val job = launch {
            runtime(clock).runGraphicalStream(source, target) {
                output += it
                firstRendered.complete(Unit)
                if (output.size == 4) allRendered.complete(Unit)
            }
        }

        source.emit(streaming(149.0))
        awaitSignal(firstRendered)
        source.emit(streaming(150.0))
        awaitSignal(allRendered)
        job.cancelAndJoin()

        assertEquals(
            listOf(
                PintFrame.Steady(PintProgress(0, 19)),
                PintFrame.FullBubbles(1),
                PintFrame.Draining(1),
                PintFrame.Steady(PintProgress(1, 0)),
            ),
            output,
        )
        assertEquals(listOf(1_000L, 1_000L, 1_000L), clock.waits)
    }

    @Test
    fun `graphical target change establishes a steady baseline`() = runBlocking {
        val source = MutableSharedFlow<StreamState>(replay = 1, extraBufferCapacity = 4)
        val target = MutableStateFlow(150)
        val output = mutableListOf<PintFrame>()
        val firstRendered = CompletableDeferred<Unit>()
        val secondRendered = CompletableDeferred<Unit>()
        val job = launch {
            runtime(TestClock()).runGraphicalStream(source, target) {
                output += it
                firstRendered.complete(Unit)
                if (output.size == 2) secondRendered.complete(Unit)
            }
        }

        source.emit(streaming(150.0))
        awaitSignal(firstRendered)
        target.value = 300
        source.emit(streaming(150.0))
        awaitSignal(secondRendered)
        job.cancelAndJoin()

        assertEquals(
            listOf(
                PintFrame.Steady(PintProgress(1, 0)),
                PintFrame.Steady(PintProgress(0, 10)),
            ),
            output,
        )
    }

    @Test
    fun `graphical preview repeats ordered frames and waits one second`() = runBlocking {
        val output = mutableListOf<PintFrame>()
        val clock = TestClock()
        val fourthWait = CompletableDeferred<Unit>()
        val job = launch {
            PintDataFieldRuntime({ clock.nowMillis }) { millis ->
                clock.waits += millis
                clock.nowMillis += millis
                if (clock.waits.size == 4) fourthWait.complete(Unit)
                yield()
            }.runGraphicalPreview { output += it }
        }
        awaitSignal(fourthWait)
        job.cancelAndJoin()

        assertEquals(
            listOf(
                PintFrame.Steady(PintProgress(0, 10)),
                PintFrame.Steady(PintProgress(0, 16)),
                PintFrame.FullBubbles(0),
                PintFrame.Steady(PintProgress(0, 10)),
            ),
            output,
        )
        assertEquals(List(4) { 1_000L }, clock.waits)
    }

    @Test
    fun `graphical preview cancellation during wait prevents later callbacks`() = runBlocking {
        val output = mutableListOf<PintFrame>()
        val first = CompletableDeferred<Unit>()
        val waitStarted = CompletableDeferred<Unit>()
        val gate = CompletableDeferred<Unit>()
        val job = launch {
            PintDataFieldRuntime({ 0L }) { millis ->
                assertEquals(1_000L, millis)
                waitStarted.complete(Unit)
                gate.await()
            }.runGraphicalPreview {
                output += it
                first.complete(Unit)
            }
        }

        awaitSignal(first)
        awaitSignal(waitStarted)
        job.cancelAndJoin()
        gate.complete(Unit)

        assertEquals(listOf(PintFrame.Steady(PintProgress(0, 10))), output)
    }

    @Test
    fun `graphical stream cancellation during pending delay prevents later callbacks`() = runBlocking {
        val source = MutableSharedFlow<StreamState>(replay = 1, extraBufferCapacity = 4)
        val target = MutableStateFlow(150)
        val output = mutableListOf<PintFrame>()
        val clock = TestClock()
        val firstRendered = CompletableDeferred<Unit>()
        val waitStarted = CompletableDeferred<Unit>()
        val gate = CompletableDeferred<Unit>()
        var waitCount = 0
        val job = launch {
            PintDataFieldRuntime({ clock.nowMillis }) { millis ->
                clock.waits += millis
                clock.nowMillis += millis
                waitCount += 1
                if (waitCount == 2) {
                    waitStarted.complete(Unit)
                    gate.await()
                }
            }.runGraphicalStream(source, target) {
                output += it
                firstRendered.complete(Unit)
            }
        }

        source.emit(streaming(149.0))
        awaitSignal(firstRendered)
        source.emit(streaming(150.0))
        waitStarted.await()
        job.cancelAndJoin()
        gate.complete(Unit)

        assertEquals(2, output.size)
        assertTrue(output[0] is PintFrame.Steady)
        assertTrue(output[1] is PintFrame.FullBubbles)
    }

    @Test
    fun `finite streams complete and suppress duplicate graphical state`() = runBlocking {
        val runtime = runtime(TestClock())
        val numericOutput = mutableListOf<StreamState>()
        runtime.runNumericStream(
            calorieStates = flowOf(streaming(150.0)),
            caloriesPerBeer = flowOf(150),
            dataTypeId = DATA_TYPE_ID,
            emit = { numericOutput += it },
        )
        val graphicalOutput = mutableListOf<PintFrame>()
        runtime.runGraphicalStream(
            calorieStates = flow {
                emit(streaming(150.0))
                emit(streaming(150.0))
            },
            caloriesPerBeer = flowOf(150),
            emit = { graphicalOutput += it },
        )

        assertEquals(1, numericOutput.size)
        assertEquals(listOf(PintFrame.Steady(PintProgress(1, 0))), graphicalOutput)
    }

    private fun runtime(clock: TestClock) = PintDataFieldRuntime(
        nowMillis = { clock.nowMillis },
        waitMillis = { millis ->
            clock.waits += millis
            clock.nowMillis += millis
            yield()
        },
    )

    private suspend fun awaitSignal(signal: CompletableDeferred<Unit>) {
        withTimeout(TEST_TIMEOUT_MILLIS) { signal.await() }
    }

    private fun streaming(calories: Double) = StreamState.Streaming(
        DataPoint(
            dataTypeId = DataType.Type.CALORIES,
            values = mapOf(DataType.Field.SINGLE to calories),
        ),
    )

    private class TestClock {
        var nowMillis = 0L
        val waits = mutableListOf<Long>()
    }

    private companion object {
        const val DATA_TYPE_ID = "pint-progress-text"
        const val TEST_TIMEOUT_MILLIS = 5_000L
    }
}
