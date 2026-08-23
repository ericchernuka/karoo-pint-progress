package io.ericchernuka.pintprogress.core

import kotlin.math.roundToInt

/** Resolves Karoo's physical field size into the units used by Android layouts. */
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
            val resolvedWidthPx = viewSize.first.takeIf { it > 0 }
                ?: screenSize.first * gridSize.first / GRID_AXIS_SIZE
            val resolvedHeightPx = viewSize.second.takeIf { it > 0 }
                ?: screenSize.second * gridSize.second / GRID_AXIS_SIZE
            val validDensity = density.takeIf { it.isFinite() && it > 0f } ?: DEFAULT_DENSITY

            return PintFieldSize(
                widthDp = (resolvedWidthPx / validDensity).roundToInt(),
                heightDp = (resolvedHeightPx / validDensity).roundToInt(),
            )
        }
    }
}
