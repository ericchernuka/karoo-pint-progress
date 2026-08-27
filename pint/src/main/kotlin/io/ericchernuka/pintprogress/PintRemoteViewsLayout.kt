package io.ericchernuka.pintprogress
import io.hammerhead.karooext.models.ViewConfig
import io.ericchernuka.pintprogress.core.PintFieldLayout

internal fun PintFieldLayout.remoteViewsLayout(alignment: ViewConfig.Alignment): Int = when (this) { PintFieldLayout.REGULAR -> when (alignment) { ViewConfig.Alignment.LEFT -> R.layout.pint_progress_regular_left_view
        ViewConfig.Alignment.CENTER -> R.layout.pint_progress_regular_center_view
        ViewConfig.Alignment.RIGHT -> R.layout.pint_progress_regular_right_view }
    PintFieldLayout.COMPACT -> when (alignment) { ViewConfig.Alignment.LEFT -> R.layout.pint_progress_compact_left_view
        ViewConfig.Alignment.CENTER -> R.layout.pint_progress_compact_center_view
        ViewConfig.Alignment.RIGHT -> R.layout.pint_progress_compact_right_view }
    PintFieldLayout.PICKER, PintFieldLayout.ICON_ONLY, -> when (alignment) { ViewConfig.Alignment.LEFT -> R.layout.pint_progress_adaptive_left_view
        ViewConfig.Alignment.CENTER -> R.layout.pint_progress_adaptive_center_view
        ViewConfig.Alignment.RIGHT -> R.layout.pint_progress_adaptive_right_view } }
