package io.ericchernuka.pintprogress

import android.view.View
import android.widget.RemoteViews
import io.ericchernuka.pintprogress.core.PintAsset
import io.ericchernuka.pintprogress.core.PintFieldLayout
import io.ericchernuka.pintprogress.core.edgeInsetDp
import io.ericchernuka.pintprogress.core.resolveTypography
import io.hammerhead.karooext.models.ViewConfig
import kotlin.math.roundToInt

/** Android adapter that delegates presentation decisions to [io.ericchernuka.pintprogress.core.displayFor] */
internal class PintRemoteViews(
    private val packageName: String,
    private val displayDensity: Float,
    scaledDensity: Float,
) {
    private val fontScale = scaledDensity / displayDensity

    fun render(
        display: Pair<PintAsset, String>,
        layout: PintFieldLayout,
        alignment: ViewConfig.Alignment,
        textSizeSp: Int,
        boundariesEnabled: Boolean,
        fieldWidthDp: Int,
    ): RemoteViews {
        val (asset, count) = display
        val effectiveLayout = layout.forDisplay(count.isNotEmpty())

        return RemoteViews(
            packageName,
            effectiveLayout.remoteViewsLayout(alignment),
        ).apply {
            val edgeInsetPx = (edgeInsetDp(boundariesEnabled) * displayDensity).roundToInt()
            setViewPadding(R.id.pint_root, edgeInsetPx, edgeInsetPx, edgeInsetPx, edgeInsetPx)
            setImageViewResource(R.id.pint_image, asset.drawableRes(effectiveLayout))

            resolveTypography(
                layout = effectiveLayout,
                karooTextSizeSp = textSizeSp,
                countLength = count.length,
                fontScale = fontScale,
                contentWidthDp = fieldWidthDp - (edgeInsetDp(boundariesEnabled) * 2),
            )?.let { (countSizeSp, suffixSizeSp) ->
                setTextSizeSp(R.id.pint_count, countSizeSp)
                setTextSizeSp(R.id.pint_count_suffix, suffixSizeSp)
            }

            val countText = effectiveLayout.visibleCount(count)
            val countVisibility = if (countText.isEmpty()) View.GONE else View.VISIBLE
            setTextViewText(R.id.pint_count, countText)
            setViewVisibility(R.id.pint_count, countVisibility)
            setViewVisibility(R.id.pint_count_suffix, countVisibility)
        }
    }

}

/** Selects XML layouts because RemoteViews blocks runtime gravity changes */
internal fun PintFieldLayout.remoteViewsLayout(alignment: ViewConfig.Alignment): Int = when (this) {
    PintFieldLayout.REGULAR -> when (alignment) {
        ViewConfig.Alignment.LEFT -> R.layout.pint_progress_regular_left_view
        ViewConfig.Alignment.CENTER -> R.layout.pint_progress_regular_center_view
        ViewConfig.Alignment.RIGHT -> R.layout.pint_progress_regular_right_view
    }
    PintFieldLayout.COMPACT -> when (alignment) {
        ViewConfig.Alignment.LEFT -> R.layout.pint_progress_compact_left_view
        ViewConfig.Alignment.CENTER -> R.layout.pint_progress_compact_center_view
        ViewConfig.Alignment.RIGHT -> R.layout.pint_progress_compact_right_view
    }
    PintFieldLayout.PICKER,
    PintFieldLayout.ICON_ONLY,
    -> when (alignment) {
        ViewConfig.Alignment.LEFT -> R.layout.pint_progress_adaptive_left_view
        ViewConfig.Alignment.CENTER -> R.layout.pint_progress_adaptive_center_view
        ViewConfig.Alignment.RIGHT -> R.layout.pint_progress_adaptive_right_view
    }
}

private fun RemoteViews.setTextSizeSp(viewId: Int, size: Float) =
    setTextViewTextSize(viewId, android.util.TypedValue.COMPLEX_UNIT_SP, size)
