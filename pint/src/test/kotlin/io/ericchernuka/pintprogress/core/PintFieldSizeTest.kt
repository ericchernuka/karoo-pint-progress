package io.ericchernuka.pintprogress.core

import org.junit.Assert.assertEquals
import org.junit.Test

class PintFieldSizeTest {
    @Test
    fun `reported physical size takes precedence over grid proportions`() {
        assertEquals(
            PintFieldSize(widthPx = 480, heightPx = 200),
            PintFieldSize.resolve(
                viewSize = 480 to 200,
                gridSize = 30 to 15,
                screenSize = 480 to 800,
            ),
        )
    }

    @Test
    fun `grid proportions provide size when a host reports zero dimensions`() {
        assertEquals(
            PintFieldSize(widthPx = 240, heightPx = 200),
            PintFieldSize.resolve(
                viewSize = 0 to 0,
                gridSize = 30 to 15,
                screenSize = 480 to 800,
            ),
        )
    }
}
