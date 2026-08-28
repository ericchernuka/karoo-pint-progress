package io.ericchernuka.pintprogress

import io.hammerhead.karooext.internal.Emitter
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
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

    private class CancellableEmitter : Emitter<Any> {
        var callback: (() -> Unit)? = null

        override fun onNext(t: Any) = Unit

        override fun onError(t: Throwable) = Unit

        override fun onComplete() = Unit

        override fun setCancellable(cancellable: () -> Unit) {
            callback = cancellable
        }

        override fun cancel() {
            callback?.invoke()
        }
    }
}
