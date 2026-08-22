package io.ericchernuka.pintprogress

import android.view.View
import android.widget.RemoteViews
import io.ericchernuka.pintprogress.core.PintAsset
import io.ericchernuka.pintprogress.core.PintDisplay

/** Thin Android adapter. Presentation decisions are made in [io.ericchernuka.pintprogress.core.PintPresentation]. */
internal class PintRemoteViews(private val packageName: String) {
    fun render(display: PintDisplay): RemoteViews = RemoteViews(packageName, R.layout.pint_progress_view).apply {
        setImageViewResource(R.id.pint_image, display.asset.drawableRes())

        val countText = display.count
        val countVisibility = if (countText.isEmpty()) View.GONE else View.VISIBLE
        setTextViewText(R.id.pint_count, countText)
        setViewVisibility(R.id.pint_count, countVisibility)
        setViewVisibility(R.id.pint_count_suffix, countVisibility)
    }

    private fun PintAsset.drawableRes(): Int = when (this) {
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
}
