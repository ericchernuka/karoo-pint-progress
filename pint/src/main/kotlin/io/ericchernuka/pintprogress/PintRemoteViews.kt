package io.ericchernuka.pintprogress

import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import android.os.Build
import android.util.TypedValue
import android.view.View
import android.widget.RemoteViews
import io.ericchernuka.pintprogress.core.PintAsset
import io.ericchernuka.pintprogress.core.PintFieldLayout
import io.ericchernuka.pintprogress.core.PintFillAsset
import io.ericchernuka.pintprogress.core.countRasterUpwardBiasDp
import io.ericchernuka.pintprogress.core.edgeInsetDp
import io.ericchernuka.pintprogress.core.opticalCenterOffset
import io.ericchernuka.pintprogress.core.opticalTranslationY
import io.ericchernuka.pintprogress.core.resolveTypography
import io.hammerhead.karooext.models.ViewConfig
import kotlin.math.roundToInt

private const val SUFFIX_RASTER_UPWARD_BIAS_DP = 2f

/** Android adapter that delegates presentation decisions to [io.ericchernuka.pintprogress.core.displayFor] */
internal class PintRemoteViews(
    private val packageName: String,
    private val displayDensity: Float,
    scaledDensity: Float,
) {
    private val fontScale = scaledDensity / displayDensity
    private val countPaint = Paint().apply {
        typeface = Typeface.create("sans-serif-condensed", Typeface.BOLD)
        textSize = 100f
        textScaleX = 0.68f
    }
    private val countLineHeightPerTextSize = countPaint.fontMetrics.run { (bottom - top) / countPaint.textSize }
    private val unpaddedCountOpticalOffsetPerTextSize = countPaint.opticalOffsetPerTextSize("100")
    private val suffixOpticalOffsetPerTextSize = countPaint.opticalOffsetPerTextSize("+")

    fun render(
        display: Pair<PintAsset, String>,
        layout: PintFieldLayout,
        alignment: ViewConfig.Alignment,
        textSizeSp: Int,
        boundariesEnabled: Boolean,
        fieldWidthDp: Int,
        fieldHeightDp: Int,
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
                contentHeightDp = fieldHeightDp - (edgeInsetDp(boundariesEnabled) * 2),
                lineHeightPerTextSize = countLineHeightPerTextSize,
                measuredTextWidthPerCountSize = (
                    countPaint.measureText(count) +
                        countPaint.measureText("+") * effectiveLayout.suffixToCountRatio
                    ) / countPaint.textSize,
                maximumScale = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    effectiveLayout.maxScale
                } else {
                    1f
                },
            )?.let { (countSizeSp, suffixSizeSp) ->
                setTextSizeSp(R.id.pint_count, countSizeSp)
                setTextSizeSp(R.id.pint_count_suffix, suffixSizeSp)
                val countOffsetPx =
                    unpaddedCountOpticalOffsetPerTextSize * countSizeSp * displayDensity * fontScale
                setFloat(
                    R.id.pint_count,
                    "setTranslationY",
                    opticalTranslationY(
                        countOffsetPx,
                        upwardBiasPx = effectiveLayout.countRasterUpwardBiasDp(countSizeSp) *
                            displayDensity,
                    ),
                )
                val suffixOffsetPx =
                    suffixOpticalOffsetPerTextSize * suffixSizeSp * displayDensity * fontScale
                setFloat(
                    R.id.pint_count_suffix,
                    "setTranslationY",
                    opticalTranslationY(
                        suffixOffsetPx,
                        upwardBiasPx = SUFFIX_RASTER_UPWARD_BIAS_DP * displayDensity,
                    ),
                )
                val mugScale = requireNotNull(effectiveLayout.mugScaleFor(countSizeSp, fontScale))
                val mugWidthDp = effectiveLayout.nominalMugWidthDp * mugScale
                val mugHeightDp = effectiveLayout.nominalMugHeightDp * mugScale
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && mugScale > 1f) {
                    setViewLayoutWidth(R.id.pint_image, mugWidthDp, TypedValue.COMPLEX_UNIT_DIP)
                    setViewLayoutHeight(R.id.pint_image, mugHeightDp, TypedValue.COMPLEX_UNIT_DIP)
                } else {
                    setInt(
                        R.id.pint_image,
                        "setMaxWidth",
                        (mugWidthDp * displayDensity).roundToInt(),
                    )
                    setInt(
                        R.id.pint_image,
                        "setMaxHeight",
                        (mugHeightDp * displayDensity).roundToInt(),
                    )
                }
            }

            val countText = effectiveLayout.visibleCount(count)
            val countVisibility = if (countText.isEmpty()) View.GONE else View.VISIBLE
            setTextViewText(R.id.pint_count, countText)
            setViewVisibility(R.id.pint_count, countVisibility)
            setViewVisibility(R.id.pint_count_suffix, countVisibility)
        }
    }

    fun renderFill(
        display: Pair<PintFillAsset, String>,
        textSizeSp: Int,
    ): RemoteViews = RemoteViews(packageName, R.layout.pint_progress_fill_view).apply {
        setImageViewResource(R.id.pint_fill_image, display.first.drawableRes())
        setTextViewText(R.id.pint_fill_count, display.second)
        if (textSizeSp > 0) setTextSizeSp(R.id.pint_fill_count, textSizeSp.toFloat())
    }

}

private fun Paint.opticalOffsetPerTextSize(text: String): Float {
    val bounds = Rect()
    getTextBounds(text, 0, text.length, bounds)
    val metrics = fontMetrics
    return opticalCenterOffset(
        metrics.ascent,
        metrics.descent,
        bounds.top.toFloat(),
        bounds.bottom.toFloat(),
    ) / textSize
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
