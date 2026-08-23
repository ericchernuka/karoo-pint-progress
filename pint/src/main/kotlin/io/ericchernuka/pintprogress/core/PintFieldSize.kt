package io.ericchernuka.pintprogress.core

/** Resolves Karoo's physical field size, retaining a grid-based fallback for older hosts. */
data class PintFieldSize(
    val widthPx: Int,
    val heightPx: Int,
) {
    companion object {
        private const val GRID_AXIS_SIZE = 60

        fun resolve(
            viewSize: Pair<Int, Int>,
            gridSize: Pair<Int, Int>,
            screenSize: Pair<Int, Int>,
        ): PintFieldSize = PintFieldSize(
            widthPx = viewSize.first.takeIf { it > 0 }
                ?: screenSize.first * gridSize.first / GRID_AXIS_SIZE,
            heightPx = viewSize.second.takeIf { it > 0 }
                ?: screenSize.second * gridSize.second / GRID_AXIS_SIZE,
        )
    }
}
