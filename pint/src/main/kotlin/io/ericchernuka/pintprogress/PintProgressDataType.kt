package io.ericchernuka.pintprogress
import android.content.Context
import android.os.SystemClock
import android.util.Log
import android.widget.RemoteViews
import io.ericchernuka.pintprogress.core.PintFrame
import io.ericchernuka.pintprogress.core.PintFieldLayout
import io.ericchernuka.pintprogress.core.PintFieldSize
import io.ericchernuka.pintprogress.core.PintPresentation
import io.hammerhead.karooext.KarooSystemService
import io.hammerhead.karooext.extension.DataTypeImpl
import io.hammerhead.karooext.internal.Emitter
import io.hammerhead.karooext.internal.ViewEmitter
import io.hammerhead.karooext.models.DataType
import io.hammerhead.karooext.models.ShowCustomStreamState
import io.hammerhead.karooext.models.StreamState
import io.hammerhead.karooext.models.UpdateGraphicConfig
import io.hammerhead.karooext.models.UpdateNumericConfig
import io.hammerhead.karooext.models.ViewConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
class PintProgressDataType internal constructor(private val karooSystem: KarooSystemService, private val beerCalories: Flow<Int>, extension: String, private val style: PintFieldStyle,
) : DataTypeImpl(extension, style.typeId) { override fun startStream(emitter: Emitter<StreamState>) { if (style != PintFieldStyle.TEXT) return
        val job = CoroutineScope(Dispatchers.Default).launch { runtime().runNumericStream(calorieStates = karooSystem.streamDataFlow(DataType.Type.CALORIES), caloriesPerBeer = beerCalories, dataTypeId = dataTypeId, emit = emitter::onNext, ) }
        emitter.setCancellable(job::cancel) }
    override fun startView(context: Context, config: ViewConfig, emitter: ViewEmitter) { if (style == PintFieldStyle.TEXT) { emitter.onNext(UpdateNumericConfig(formatDataTypeId = DataType.Type.VARIABILITY_INDEX))
            if (config.preview) { val job = CoroutineScope(Dispatchers.Default).launch { runtime().runNumericPreview { message ->
                        emitter.onNext(ShowCustomStreamState(message = message, color = null)) } }
                emitter.setCancellable(job::cancel) }
            return }
        emitter.onNext(UpdateGraphicConfig(showHeader = true))
        val displayMetrics = context.resources.displayMetrics
        val fieldSize = PintFieldSize.resolve(viewSize = config.viewSize, gridSize = config.gridSize, screenSize = displayMetrics.widthPixels to displayMetrics.heightPixels, density = displayMetrics.density, )
        val fieldRenderer = renderer(context = context, config = config, fieldSize = fieldSize, )
        if (BuildConfig.DEBUG) { Log.d(TAG, "grid=${config.gridSize} viewPx=${config.viewSize} fieldDp=" +
                    "${fieldSize.widthDp}x${fieldSize.heightDp} textSp=${config.textSize} " +
                    "alignment=${config.alignment} boundaries=${config.boundariesEnabled} " +
                    "preview=${config.preview} density=${displayMetrics.density} " +
                    "scaledDensity=${displayMetrics.scaledDensity} style=$style " +
                    "treatment=${fieldRenderer.treatment}", ) }
        if (config.preview) { val job = CoroutineScope(Dispatchers.Default).launch { runtime().runGraphicalPreview { frame ->
                    emitter.updateView(fieldRenderer.render(frame)) } }
            emitter.setCancellable(job::cancel)
            return }
        val job = CoroutineScope(Dispatchers.Default).launch { runtime().runGraphicalStream(calorieStates = karooSystem.streamDataFlow(DataType.Type.CALORIES), caloriesPerBeer = beerCalories, emit = { frame -> emitter.updateView(fieldRenderer.render(frame)) }, ) }
        emitter.setCancellable(job::cancel) }
    private fun renderer(context: Context, config: ViewConfig, fieldSize: PintFieldSize, ): FieldRenderer { val renderer = PintRemoteViews(packageName = context.packageName, displayDensity = context.resources.displayMetrics.density, scaledDensity = context.resources.displayMetrics.scaledDensity, )
        val layout = PintFieldLayout.forSize(preview = config.preview, widthDp = fieldSize.widthDp, heightDp = fieldSize.heightDp, boundariesEnabled = config.boundariesEnabled, )
        return FieldRenderer(treatment = layout.name) { frame ->
            renderer.render(display = PintPresentation.displayFor(frame), layout = layout, alignment = config.alignment, textSizeSp = config.textSize, boundariesEnabled = config.boundariesEnabled, fieldWidthDp = fieldSize.widthDp, ) } }
    private fun runtime() = PintDataFieldRuntime(nowMillis = SystemClock::elapsedRealtime, waitMillis = { millis -> delay(millis) }, )
    private companion object { const val TAG = "PintProgressField" } }
private class FieldRenderer(val treatment: String, val render: (PintFrame) -> RemoteViews,
)
internal enum class PintFieldStyle(val typeId: String,
) { MUG("pint-progress"), TEXT("pint-progress-text"), }
