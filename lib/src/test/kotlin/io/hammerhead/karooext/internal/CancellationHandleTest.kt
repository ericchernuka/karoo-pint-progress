package io.hammerhead.karooext.internal

import org.junit.Assert.assertEquals
import org.junit.Test

class CancellationHandleTest {
    @Test
    fun `cancels an action registered before cancellation exactly once`() {
        val handle = CancellationHandle()
        var cancellations = 0

        handle.set { cancellations += 1 }
        handle.cancel()
        handle.cancel()

        assertEquals(1, cancellations)
    }

    @Test
    fun `cancels an action registered after cancellation`() {
        val handle = CancellationHandle()
        var cancellations = 0

        handle.cancel()
        handle.set { cancellations += 1 }

        assertEquals(1, cancellations)
    }
}
