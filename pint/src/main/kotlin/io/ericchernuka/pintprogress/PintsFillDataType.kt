package io.ericchernuka.pintprogress

import android.content.Context
import android.graphics.Paint
import android.graphics.Typeface
import android.util.Log
import android.util.TypedValue
import android.widget.RemoteViews
import io.ericchernuka.pintprogress.core.PintFrame
import io.ericchernuka.pintprogress.core.fillDisplayFor
import io.ericchernuka.pintprogress.core.fillPreviewFrames
import io.ericchernuka.pintprogress.core.resolveFieldSize
import io.ericchernuka.pintprogress.core.resolveFillTextSize
import io.hammerhead.karooext.KarooSystemService
import io.hammerhead.karooext.extension.DataTypeImpl
import io.hammerhead.karooext.internal.ViewEmitter
import io.hammerhead.karooext.models.DataType
import io.hammerhead.karooext.models.UpdateGraphicConfig
import io.hammerhead.karooext.models.ViewConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect

/** Pints Fill renders edge to edge without a Karoo header, so the fill owns the whole tile */
internal class PintsFillDataType(
    private val karooSystem: KarooSystemService,
    private val beerCalories: Flow<Int>,
    extension: String,
) : DataTypeImpl(extension, VARIATION.typeId) {
    override fun startView(context: Context, config: ViewConfig, emitter: ViewEmitter) {
        emitter.onNext(UpdateGraphicConfig(showHeader = false))

        val displayMetrics = context.resources.displayMetrics
        val (fieldWidthDp, fieldHeightDp) = resolveFieldSize(
            viewSize = config.viewSize,
            gridSize = config.gridSize,
            screenSize = displayMetrics.widthPixels to displayMetrics.heightPixels,
            density = displayMetrics.density,
        )
        val fontScale = displayMetrics.scaledDensity / displayMetrics.density
        val layoutId = config.alignment.fillRemoteViewsLayout()
        val countPaint = Paint().apply {
            typeface = Typeface.create("sans-serif-condensed", Typeface.NORMAL)
            textSize = 100f
        }
        val lineHeightPerTextSize = countPaint.fontMetrics.run { (bottom - top) / countPaint.textSize }

        fun render(frame: PintFrame): RemoteViews {
            val (asset, count) = fillDisplayFor(frame)
            return RemoteViews(context.packageName, layoutId).apply {
                setImageViewResource(R.id.pint_fill_image, asset.drawableRes())
                setTextViewText(R.id.pint_fill_count, count)
                setTextViewTextSize(
                    R.id.pint_fill_count,
                    TypedValue.COMPLEX_UNIT_SP,
                    resolveFillTextSize(
                        karooTextSizeSp = config.textSize,
                        fontScale = fontScale,
                        contentWidthDp = fieldWidthDp,
                        contentHeightDp = fieldHeightDp,
                        measuredTextWidthPerTextSize = countPaint.measureText(count) / countPaint.textSize,
                        lineHeightPerTextSize = lineHeightPerTextSize,
                    ),
                )
            }
        }
        if (BuildConfig.DEBUG) {
            Log.d(
                LOG_TAG,
                "grid=${config.gridSize} viewPx=${config.viewSize} fieldDp=" +
                    "${fieldWidthDp}x${fieldHeightDp} textSp=${config.textSize} " +
                    "alignment=${config.alignment} boundaries=${config.boundariesEnabled} " +
                    "preview=${config.preview} density=${displayMetrics.density} " +
                    "scaledDensity=${displayMetrics.scaledDensity} variation=$VARIATION",
            )
        }
        emitter.launchCancellable(VARIATION.cancellationLabel(config.preview)) {
            if (config.preview) {
                deviceRuntime().graphicalPreview(fillPreviewFrames()).collect { frame ->
                    emitter.updateView(render(frame))
                }
            } else {
                deviceRuntime().runGraphicalStream(
                    calorieStates = karooSystem.streamDataFlow(DataType.Type.CALORIES),
                    caloriesPerBeer = beerCalories,
                    emit = { frame -> emitter.updateView(render(frame)) },
                )
            }
        }
    }

    private companion object {
        val VARIATION = PintFieldVariation.PINTS_FILL
    }
}

internal fun ViewConfig.Alignment.fillRemoteViewsLayout(): Int = when (this) {
    ViewConfig.Alignment.LEFT -> R.layout.pint_progress_fill_left_view
    ViewConfig.Alignment.CENTER -> R.layout.pint_progress_fill_center_view
    ViewConfig.Alignment.RIGHT -> R.layout.pint_progress_fill_right_view
}
