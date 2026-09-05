package io.ericchernuka.pintprogress

import io.hammerhead.karooext.internal.Emitter
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit.SECONDS

class PintCancellationReleaseTest {
    @Test
    fun `preview cancellation callback cancels numeric and graphical jobs`() = runBlocking {
        for (style in PintFieldStyle.entries) {
            val label = style.cancellationLabel(preview = true)
            val emitter = CancellableEmitter()
            val started = CountDownLatch(1)
            val cancelled = CountDownLatch(1)

            emitter.launchCancellable(label) {
                started.countDown()
                try {
                    awaitCancellation()
                } finally {
                    cancelled.countDown()
                }
            }

            assertTrue(started.await(1, SECONDS))
            requireNotNull(emitter.callback).invoke()
            assertTrue(cancelled.await(1, SECONDS))
        }
    }

    @Test
    fun `cancelled emitter does not start work before registration`() {
        val started = CountDownLatch(1)
        var startedBeforeRegistration = false
        val emitter = CancellableEmitter {
            startedBeforeRegistration = started.await(1, SECONDS)
        }
        emitter.cancel()

        emitter.launchCancellable("already-cancelled") { started.countDown() }

        assertFalse(startedBeforeRegistration)
        assertEquals(1L, started.count)
    }

    private class CancellableEmitter(
        private val beforeRegistration: () -> Unit = {},
    ) : Emitter<Any> {
        private var cancelled = false
        var callback: (() -> Unit)? = null

        override fun onNext(t: Any) = Unit

        override fun onError(t: Throwable) = Unit

        override fun onComplete() = Unit

        override fun setCancellable(cancellable: () -> Unit) {
            beforeRegistration()
            callback = cancellable
            if (cancelled) cancellable()
        }

        override fun cancel() {
            cancelled = true
            callback?.invoke()
        }
    }
}
