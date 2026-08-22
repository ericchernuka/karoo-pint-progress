package io.ericchernuka.pintprogress

import android.view.Gravity
import android.view.View
import android.widget.RemoteViews
import io.ericchernuka.pintprogress.core.PintAsset
import io.ericchernuka.pintprogress.core.PintDisplay
import io.ericchernuka.pintprogress.core.PintFieldLayout
import io.hammerhead.karooext.models.ViewConfig

/** Thin Android adapter. Presentation decisions are made in [io.ericchernuka.pintprogress.core.PintPresentation]. */
internal class PintRemoteViews(private val packageName: String) {
    fun render(
        display: PintDisplay,
        layout: PintFieldLayout,
        alignment: ViewConfig.Alignment,
    ): RemoteViews = RemoteViews(
        packageName,
        layout.remoteViewsLayout(),
    ).apply {
        setInt(R.id.pint_content, "setGravity", alignment.gravity())
        setImageViewResource(R.id.pint_image, display.asset.drawableRes(layout))

        if (layout.showsCount) {
            val countText = display.count
            val countVisibility = if (countText.isEmpty()) View.GONE else View.VISIBLE
            setTextViewText(R.id.pint_count, countText)
            setViewVisibility(R.id.pint_count, countVisibility)
            setViewVisibility(R.id.pint_count_suffix, countVisibility)
        }
    }

    private fun PintFieldLayout.remoteViewsLayout(): Int = when (this) {
        PintFieldLayout.PICKER -> R.layout.pint_progress_picker_view
        PintFieldLayout.REGULAR -> R.layout.pint_progress_view
        PintFieldLayout.COMPACT -> R.layout.pint_progress_compact_view
        PintFieldLayout.ICON_ONLY -> R.layout.pint_progress_icon_view
    }

    private fun ViewConfig.Alignment.gravity(): Int = when (this) {
        ViewConfig.Alignment.LEFT -> Gravity.LEFT
        ViewConfig.Alignment.CENTER -> Gravity.CENTER_HORIZONTAL
        ViewConfig.Alignment.RIGHT -> Gravity.RIGHT
    } or Gravity.CENTER_VERTICAL

    private fun PintAsset.drawableRes(layout: PintFieldLayout): Int = when (layout) {
        PintFieldLayout.PICKER -> R.drawable.pint_50_compact
        PintFieldLayout.REGULAR -> regularDrawableRes()
        PintFieldLayout.COMPACT -> compactDrawableRes()
        PintFieldLayout.ICON_ONLY -> iconDrawableRes()
    }

    private fun PintAsset.regularDrawableRes(): Int = when (this) {
        PintAsset.PINT_00 -> R.drawable.pint_00
        PintAsset.PINT_05 -> R.drawable.pint_05
        PintAsset.PINT_10 -> R.drawable.pint_10
        PintAsset.PINT_15 -> R.drawable.pint_15
        PintAsset.PINT_20 -> R.drawable.pint_20
        PintAsset.PINT_25 -> R.drawable.pint_25
        PintAsset.PINT_30 -> R.drawable.pint_30
        PintAsset.PINT_35 -> R.drawable.pint_35
        PintAsset.PINT_40 -> R.drawable.pint_40
        PintAsset.PINT_45 -> R.drawable.pint_45
        PintAsset.PINT_50 -> R.drawable.pint_50
        PintAsset.PINT_55 -> R.drawable.pint_55
        PintAsset.PINT_60 -> R.drawable.pint_60
        PintAsset.PINT_65 -> R.drawable.pint_65
        PintAsset.PINT_70 -> R.drawable.pint_70
        PintAsset.PINT_75 -> R.drawable.pint_75
        PintAsset.PINT_80 -> R.drawable.pint_80
        PintAsset.PINT_85 -> R.drawable.pint_85
        PintAsset.PINT_90 -> R.drawable.pint_90
        PintAsset.PINT_95 -> R.drawable.pint_95
        PintAsset.FULL_BUBBLES -> R.drawable.pint_full_bubbles
        PintAsset.DRAINING -> R.drawable.pint_draining
        PintAsset.UNAVAILABLE -> R.drawable.pint_unavailable
    }

    private fun PintAsset.compactDrawableRes(): Int = when (this) {
        PintAsset.PINT_00 -> R.drawable.pint_00_compact
        PintAsset.PINT_05 -> R.drawable.pint_05_compact
        PintAsset.PINT_10 -> R.drawable.pint_10_compact
        PintAsset.PINT_15 -> R.drawable.pint_15_compact
        PintAsset.PINT_20 -> R.drawable.pint_20_compact
        PintAsset.PINT_25 -> R.drawable.pint_25_compact
        PintAsset.PINT_30 -> R.drawable.pint_30_compact
        PintAsset.PINT_35 -> R.drawable.pint_35_compact
        PintAsset.PINT_40 -> R.drawable.pint_40_compact
        PintAsset.PINT_45 -> R.drawable.pint_45_compact
        PintAsset.PINT_50 -> R.drawable.pint_50_compact
        PintAsset.PINT_55 -> R.drawable.pint_55_compact
        PintAsset.PINT_60 -> R.drawable.pint_60_compact
        PintAsset.PINT_65 -> R.drawable.pint_65_compact
        PintAsset.PINT_70 -> R.drawable.pint_70_compact
        PintAsset.PINT_75 -> R.drawable.pint_75_compact
        PintAsset.PINT_80 -> R.drawable.pint_80_compact
        PintAsset.PINT_85 -> R.drawable.pint_85_compact
        PintAsset.PINT_90 -> R.drawable.pint_90_compact
        PintAsset.PINT_95 -> R.drawable.pint_95_compact
        PintAsset.FULL_BUBBLES -> R.drawable.pint_full_bubbles_compact
        PintAsset.DRAINING -> R.drawable.pint_draining_compact
        PintAsset.UNAVAILABLE -> R.drawable.pint_unavailable_compact
    }

    private fun PintAsset.iconDrawableRes(): Int = when (this) {
        PintAsset.PINT_00 -> R.drawable.pint_00_icon
        PintAsset.PINT_05 -> R.drawable.pint_05_icon
        PintAsset.PINT_10 -> R.drawable.pint_10_icon
        PintAsset.PINT_15 -> R.drawable.pint_15_icon
        PintAsset.PINT_20 -> R.drawable.pint_20_icon
        PintAsset.PINT_25 -> R.drawable.pint_25_icon
        PintAsset.PINT_30 -> R.drawable.pint_30_icon
        PintAsset.PINT_35 -> R.drawable.pint_35_icon
        PintAsset.PINT_40 -> R.drawable.pint_40_icon
        PintAsset.PINT_45 -> R.drawable.pint_45_icon
        PintAsset.PINT_50 -> R.drawable.pint_50_icon
        PintAsset.PINT_55 -> R.drawable.pint_55_icon
        PintAsset.PINT_60 -> R.drawable.pint_60_icon
        PintAsset.PINT_65 -> R.drawable.pint_65_icon
        PintAsset.PINT_70 -> R.drawable.pint_70_icon
        PintAsset.PINT_75 -> R.drawable.pint_75_icon
        PintAsset.PINT_80 -> R.drawable.pint_80_icon
        PintAsset.PINT_85 -> R.drawable.pint_85_icon
        PintAsset.PINT_90 -> R.drawable.pint_90_icon
        PintAsset.PINT_95 -> R.drawable.pint_95_icon
        PintAsset.FULL_BUBBLES -> R.drawable.pint_full_bubbles_icon
        PintAsset.DRAINING -> R.drawable.pint_draining_icon
        PintAsset.UNAVAILABLE -> R.drawable.pint_unavailable_icon
    }
}
