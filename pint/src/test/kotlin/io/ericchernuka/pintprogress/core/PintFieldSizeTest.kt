package io.ericchernuka.pintprogress.core

import org.junit.Assert.assertEquals
import org.junit.Test

class PintFieldSizeTest {
    @Test
    fun `reported physical size takes precedence over grid proportions at density one`() {
        val resolvedSize = PintFieldSize.resolve(
            viewSize = 480 to 200,
            gridSize = 30 to 15,
            screenSize = 480 to 800,
            density = 1f,
        )

        assertEquals(480, resolvedSize.widthDp)
        assertEquals(200, resolvedSize.heightDp)
    }

    @Test
    fun `reported physical size converts to density independent units`() {
        val resolvedSize = PintFieldSize.resolve(
            viewSize = 961 to 401,
            gridSize = 30 to 15,
            screenSize = 480 to 800,
            density = 2f,
        )

        assertEquals(481, resolvedSize.widthDp)
        assertEquals(201, resolvedSize.heightDp)
    }

    @Test
    fun `grid proportions provide each missing dimension before conversion`() {
        val resolvedSize = PintFieldSize.resolve(
            viewSize = 0 to 400,
            gridSize = 30 to 15,
            screenSize = 480 to 800,
            density = 2f,
        )

        assertEquals(120, resolvedSize.widthDp)
        assertEquals(200, resolvedSize.heightDp)

        val heightFallbackSize = PintFieldSize.resolve(
            viewSize = 400 to 0,
            gridSize = 30 to 15,
            screenSize = 480 to 800,
            density = 2f,
        )

        assertEquals(200, heightFallbackSize.widthDp)
        assertEquals(100, heightFallbackSize.heightDp)
    }

    @Test
    fun `invalid density falls back to density one`() {
        val expected = PintFieldSize.resolve(
            viewSize = 480 to 200,
            gridSize = 30 to 15,
            screenSize = 480 to 800,
            density = 1f,
        )

        listOf(0f, -2f, Float.NaN, Float.POSITIVE_INFINITY).forEach { invalidDensity ->
            val resolvedSize = PintFieldSize.resolve(
                viewSize = 480 to 200,
                gridSize = 30 to 15,
                screenSize = 480 to 800,
                density = invalidDensity,
            )

            assertEquals(expected.widthDp, resolvedSize.widthDp)
            assertEquals(expected.heightDp, resolvedSize.heightDp)
        }
    }
}
