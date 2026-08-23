package io.ericchernuka.pintprogress.core

import kotlin.math.roundToInt

/** Resolves Karoo's physical field size into density-independent layout units. */
class PintFieldSize(
    val widthDp: Int,
    val heightDp: Int,
) {
    companion object {
        private const val GRID_AXIS_SIZE = 60
        private const val DEFAULT_DENSITY = 1f

        fun resolve(
            viewSize: Pair<Int, Int>,
            gridSize: Pair<Int, Int>,
            screenSize: Pair<Int, Int>,
            density: Float,
        ): PintFieldSize {
            val resolvedWidth = viewSize.first.takeIf { it > 0 }
                ?: screenSize.first * gridSize.first / GRID_AXIS_SIZE
            val resolvedHeight = viewSize.second.takeIf { it > 0 }
                ?: screenSize.second * gridSize.second / GRID_AXIS_SIZE
            val validDensity = density.takeIf { it.isFinite() && it > 0f } ?: DEFAULT_DENSITY

            // Convert each resolved physical dimension to dp, rounding to the nearest dp.
            return PintFieldSize(
                widthDp = (resolvedWidth / validDensity).roundToInt(),
                heightDp = (resolvedHeight / validDensity).roundToInt(),
            )
        }
    }
}
