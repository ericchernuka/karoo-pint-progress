package io.ericchernuka.pintprogress

import android.view.View
import android.widget.RemoteViews
import io.ericchernuka.pintprogress.core.PintFieldChrome
import io.ericchernuka.pintprogress.core.PintDisplay
import io.ericchernuka.pintprogress.core.PintFieldLayout
import io.ericchernuka.pintprogress.core.PintFieldTypography
import io.hammerhead.karooext.models.ViewConfig
import kotlin.math.roundToInt

/** Thin Android adapter. Presentation decisions are made in [io.ericchernuka.pintprogress.core.PintPresentation]. */
internal class PintRemoteViews(
    private val packageName: String,
    private val displayDensity: Float,
    scaledDensity: Float,
) {
    private val fontScale = scaledDensity / displayDensity

    fun render(
        display: PintDisplay,
        layout: PintFieldLayout,
        alignment: ViewConfig.Alignment,
        textSizeSp: Int,
        boundariesEnabled: Boolean,
        fieldWidthDp: Int,
    ): RemoteViews {
        val effectiveLayout = layout.forDisplay(display.count.isNotEmpty())

        return RemoteViews(
            packageName,
            effectiveLayout.remoteViewsLayout(),
        ).apply {
            val edgeInsetPx = (PintFieldChrome.edgeInsetDp(boundariesEnabled) * displayDensity).roundToInt()
            setViewPadding(R.id.pint_root, edgeInsetPx, edgeInsetPx, edgeInsetPx, edgeInsetPx)
            setInt(R.id.pint_content, "setGravity", alignment.gravity())
            setImageViewResource(R.id.pint_image, display.asset.drawableRes(effectiveLayout))

            PintFieldTypography.forLayout(
                layout = effectiveLayout,
                karooTextSizeSp = textSizeSp,
                countLength = display.count.length,
                fontScale = fontScale,
                contentWidthDp = fieldWidthDp - (PintFieldChrome.edgeInsetDp(boundariesEnabled) * 2),
            )?.let { typography ->
                setTextViewTextSize(
                    R.id.pint_count,
                    android.util.TypedValue.COMPLEX_UNIT_SP,
                    typography.countTextSizeSp,
                )
                setTextViewTextSize(
                    R.id.pint_count_suffix,
                    android.util.TypedValue.COMPLEX_UNIT_SP,
                    typography.suffixTextSizeSp,
                )
            }

            if (effectiveLayout.showsCount) {
                val countText = display.count
                val countVisibility = if (countText.isEmpty()) View.GONE else View.VISIBLE
                setTextViewText(R.id.pint_count, countText)
                setViewVisibility(R.id.pint_count, countVisibility)
                setViewVisibility(R.id.pint_count_suffix, countVisibility)
            }
        }
    }

    private fun PintFieldLayout.remoteViewsLayout(): Int = when (this) {
        PintFieldLayout.PICKER -> R.layout.pint_progress_picker_view
        PintFieldLayout.REGULAR -> R.layout.pint_progress_view
        PintFieldLayout.COMPACT -> R.layout.pint_progress_compact_view
        PintFieldLayout.ICON_ONLY -> R.layout.pint_progress_icon_view
    }

}
