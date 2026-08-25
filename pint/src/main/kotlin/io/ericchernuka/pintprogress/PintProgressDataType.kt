package io.ericchernuka.pintprogress

import android.content.Context
import android.os.SystemClock
import android.util.Log
import android.widget.RemoteViews
import io.ericchernuka.pintprogress.core.PintFrame
import io.ericchernuka.pintprogress.core.PintFieldLayout
import io.ericchernuka.pintprogress.core.PintFieldSize
import io.ericchernuka.pintprogress.core.PintPresentation
import io.ericchernuka.pintprogress.core.PintTextStreamState
import io.ericchernuka.pintprogress.core.PintViewReducer
import io.hammerhead.karooext.KarooSystemService
import io.hammerhead.karooext.extension.DataTypeImpl
import io.hammerhead.karooext.internal.Emitter
import io.hammerhead.karooext.internal.ViewEmitter
import io.hammerhead.karooext.models.DataType
import io.hammerhead.karooext.models.StreamState
import io.hammerhead.karooext.models.UpdateGraphicConfig
import io.hammerhead.karooext.models.UpdateNumericConfig
import io.hammerhead.karooext.models.ViewConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

class PintProgressDataType internal constructor(
    private val karooSystem: KarooSystemService,
    private val beerCalories: Flow<Int>,
    extension: String,
    private val style: PintFieldStyle,
) : DataTypeImpl(extension, style.typeId) {
    override fun startStream(emitter: Emitter<StreamState>) {
        if (style != PintFieldStyle.TEXT) return

        val job = CoroutineScope(Dispatchers.Default).launch {
            var lastStreamUpdateMillis: Long? = null
            karooSystem.streamDataFlow(DataType.Type.CALORIES)
                .combine(beerCalories) { source, caloriesPerBeer ->
                    PintTextStreamState.from(
                        source = source,
                        caloriesPerBeer = caloriesPerBeer,
                        dataTypeId = dataTypeId,
                    )
                }
                .distinctUntilChanged()
                .conflate()
                .collect { state ->
                    lastStreamUpdateMillis?.let { lastUpdateMillis ->
                        val waitMillis = VIEW_UPDATE_INTERVAL_MILLIS -
                            (SystemClock.elapsedRealtime() - lastUpdateMillis)
                        if (waitMillis > 0) delay(waitMillis)
                    }
                    emitter.onNext(state)
                    lastStreamUpdateMillis = SystemClock.elapsedRealtime()
                }
        }
        emitter.setCancellable(job::cancel)
    }

    override fun startView(context: Context, config: ViewConfig, emitter: ViewEmitter) {
        if (style == PintFieldStyle.TEXT) {
            emitter.onNext(UpdateNumericConfig(formatDataTypeId = DataType.Type.VARIABILITY_INDEX))
            return
        }

        emitter.onNext(UpdateGraphicConfig(showHeader = true))

        val displayMetrics = context.resources.displayMetrics
        val fieldSize = PintFieldSize.resolve(
            viewSize = config.viewSize,
            gridSize = config.gridSize,
            screenSize = displayMetrics.widthPixels to displayMetrics.heightPixels,
            density = displayMetrics.density,
        )
        val fieldRenderer = renderer(
            context = context,
            config = config,
            fieldSize = fieldSize,
        )
        if (BuildConfig.DEBUG) {
            Log.d(
                TAG,
                "grid=${config.gridSize} viewPx=${config.viewSize} fieldDp=" +
                    "${fieldSize.widthDp}x${fieldSize.heightDp} textSp=${config.textSize} " +
                    "alignment=${config.alignment} boundaries=${config.boundariesEnabled} " +
                    "preview=${config.preview} density=${displayMetrics.density} " +
                    "scaledDensity=${displayMetrics.scaledDensity} style=$style " +
                    "treatment=${fieldRenderer.treatment}",
            )
        }
        if (config.preview) {
            val job = CoroutineScope(Dispatchers.Default).launch {
                while (true) {
                    previewFrames().forEach { frame ->
                        emitter.updateView(fieldRenderer.render(frame))
                        delay(VIEW_UPDATE_INTERVAL_MILLIS)
                    }
                }
            }
            emitter.setCancellable(job::cancel)
            return
        }

        val job = CoroutineScope(Dispatchers.Default).launch {
            val reducer = PintViewReducer()
            var lastViewUpdateMillis: Long? = null

            suspend fun render(frame: PintFrame) {
                lastViewUpdateMillis?.let { lastUpdateMillis ->
                    val waitMillis = VIEW_UPDATE_INTERVAL_MILLIS -
                        (SystemClock.elapsedRealtime() - lastUpdateMillis)
                    if (waitMillis > 0) delay(waitMillis)
                }

                emitter.updateView(fieldRenderer.render(frame))
                lastViewUpdateMillis = SystemClock.elapsedRealtime()
            }

            karooSystem.streamDataFlow(DataType.Type.CALORIES)
                .combine(beerCalories) { state, caloriesPerBeer -> state to caloriesPerBeer }
                .conflate()
                .collect { (state, caloriesPerBeer) ->
                    reducer.accept(state, caloriesPerBeer)?.frames?.forEach { timedFrame ->
                        if (timedFrame.delayMillis > 0) delay(timedFrame.delayMillis)
                        render(timedFrame.frame)
                    }
                }
        }
        emitter.setCancellable(job::cancel)
    }

    private fun renderer(
        context: Context,
        config: ViewConfig,
        fieldSize: PintFieldSize,
    ): FieldRenderer {
        val renderer = PintRemoteViews(
            packageName = context.packageName,
            displayDensity = context.resources.displayMetrics.density,
            scaledDensity = context.resources.displayMetrics.scaledDensity,
        )
        val layout = PintFieldLayout.forSize(
            preview = config.preview,
            widthDp = fieldSize.widthDp,
            heightDp = fieldSize.heightDp,
            boundariesEnabled = config.boundariesEnabled,
        )
        return FieldRenderer(treatment = layout.name) { frame ->
            renderer.render(
                display = PintPresentation.displayFor(frame),
                layout = layout,
                alignment = config.alignment,
                textSizeSp = config.textSize,
                boundariesEnabled = config.boundariesEnabled,
                fieldWidthDp = fieldSize.widthDp,
            )
        }
    }

    private fun previewFrames(): List<PintFrame> = PintViewReducer.previewFrames()

    private companion object {
        const val TAG = "PintProgressField"
        const val VIEW_UPDATE_INTERVAL_MILLIS = 1_000L
    }
}

private class FieldRenderer(
    val treatment: String,
    val render: (PintFrame) -> RemoteViews,
)

internal enum class PintFieldStyle(
    val typeId: String,
) {
    MUG("pint-progress"),
    TEXT("pint-progress-text"),
}
