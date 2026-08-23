package io.ericchernuka.pintprogress.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class PintFieldSizeTest {
    @Test
    fun `reported physical size takes precedence over grid proportions`() {
        val reportedSize = PintFieldSize(widthPx = 480, heightPx = 200)

        assertEquals(
            reportedSize,
            PintFieldSize.resolve(
                viewSize = 480 to 200,
                gridSize = 30 to 15,
                screenSize = 480 to 800,
            ),
        )
        assertEquals(480, reportedSize.widthPx)
        assertEquals(200, reportedSize.heightPx)
        assertEquals(480, reportedSize.component1())
        assertEquals(200, reportedSize.component2())
        assertEquals(reportedSize, reportedSize.copy())
        assertEquals(PintFieldSize(widthPx = 481, heightPx = 200), reportedSize.copy(widthPx = 481))
        assertEquals(PintFieldSize(widthPx = 480, heightPx = 201), reportedSize.copy(heightPx = 201))
        assertEquals(reportedSize.hashCode(), reportedSize.copy().hashCode())
        assertEquals("PintFieldSize(widthPx=480, heightPx=200)", reportedSize.toString())
        assertEquals(reportedSize, reportedSize)
        assertNotEquals(reportedSize, "480 by 200")
        assertNotEquals(reportedSize, PintFieldSize(widthPx = 481, heightPx = 200))
        assertNotEquals(reportedSize, PintFieldSize(widthPx = 480, heightPx = 201))
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
