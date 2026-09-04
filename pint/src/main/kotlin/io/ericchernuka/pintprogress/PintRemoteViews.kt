package io.ericchernuka.pintprogress

import android.graphics.Paint
import android.graphics.Typeface
import android.widget.RemoteViews
import io.ericchernuka.pintprogress.core.PintFillAsset
import io.ericchernuka.pintprogress.core.resolveFillTextSize
import io.hammerhead.karooext.models.ViewConfig

/** Android adapter for the Pints Fill graphical field */
internal class PintRemoteViews(
    private val packageName: String,
    scaledDensity: Float,
    density: Float,
) {
    private val fontScale = scaledDensity / density
    private val fillCountPaint = Paint().apply {
        typeface = Typeface.create("sans-serif-condensed", Typeface.NORMAL)
        textSize = 100f
        textScaleX = 1f
    }
    private val fillCountLineHeightPerTextSize = fillCountPaint.fontMetrics.run {
        (bottom - top) / fillCountPaint.textSize
    }

    fun renderFill(
        display: Pair<PintFillAsset, String>,
        alignment: ViewConfig.Alignment,
        textSizeSp: Int,
        fieldWidthDp: Int,
        fieldHeightDp: Int,
    ): RemoteViews = RemoteViews(packageName, alignment.fillRemoteViewsLayout()).apply {
        setImageViewResource(R.id.pint_fill_image, display.first.drawableRes())
        setTextViewText(R.id.pint_fill_count, display.second)
        setTextSizeSp(
            R.id.pint_fill_count,
            resolveFillTextSize(
                karooTextSizeSp = textSizeSp,
                fontScale = fontScale,
                contentWidthDp = fieldWidthDp,
                contentHeightDp = fieldHeightDp,
                measuredTextWidthPerTextSize = fillCountPaint.measureText(display.second) /
                    fillCountPaint.textSize,
                lineHeightPerTextSize = fillCountLineHeightPerTextSize,
            ),
        )
    }
}

internal fun ViewConfig.Alignment.fillRemoteViewsLayout(): Int = when (this) {
    ViewConfig.Alignment.LEFT -> R.layout.pint_progress_fill_left_view
    ViewConfig.Alignment.CENTER -> R.layout.pint_progress_fill_center_view
    ViewConfig.Alignment.RIGHT -> R.layout.pint_progress_fill_right_view
}

private fun RemoteViews.setTextSizeSp(viewId: Int, size: Float) =
    setTextViewTextSize(viewId, android.util.TypedValue.COMPLEX_UNIT_SP, size)
