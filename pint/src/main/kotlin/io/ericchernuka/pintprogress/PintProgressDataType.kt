package io.ericchernuka.pintprogress

import android.content.Context
import android.os.SystemClock
import android.util.Log
import io.ericchernuka.pintprogress.core.PintFrame
import io.ericchernuka.pintprogress.core.PintFieldLayout
import io.ericchernuka.pintprogress.core.PintViewReducer
import io.ericchernuka.pintprogress.core.PintViewUpdate
import io.ericchernuka.pintprogress.core.displayFor
import io.ericchernuka.pintprogress.core.fillDisplayFor
import io.ericchernuka.pintprogress.core.fillPreviewFrames
import io.ericchernuka.pintprogress.core.graphicalPreviewFrames
import io.ericchernuka.pintprogress.core.numericPreviewMessages
import io.ericchernuka.pintprogress.core.numericStateFrom
import io.ericchernuka.pintprogress.core.previewLayoutFor
import io.ericchernuka.pintprogress.core.resolveFieldSize
import io.hammerhead.karooext.KarooSystemService
import io.hammerhead.karooext.extension.DataTypeImpl
import io.hammerhead.karooext.internal.Emitter
import io.hammerhead.karooext.internal.ViewEmitter
import io.hammerhead.karooext.models.DataType
import io.hammerhead.karooext.models.OnStreamState
import io.hammerhead.karooext.models.ShowCustomStreamState
import io.hammerhead.karooext.models.StreamState
import io.hammerhead.karooext.models.UpdateGraphicConfig
import io.hammerhead.karooext.models.UpdateNumericConfig
import io.hammerhead.karooext.models.ViewConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

class PintProgressDataType internal constructor(
    private val karooSystem: KarooSystemService,
    private val beerCalories: Flow<Int>,
    extension: String,
    private val style: PintFieldStyle,
) : DataTypeImpl(extension, style.typeId) {
    override fun startStream(emitter: Emitter<StreamState>) {
        if (style != PintFieldStyle.TEXT) return

        emitter.launchCancellable(style.cancellationLabel(preview = false)) {
            runtime().runNumericStream(
                calorieStates = karooSystem.streamDataFlow(DataType.Type.CALORIES),
                caloriesPerBeer = beerCalories,
                dataTypeId = dataTypeId,
                emit = emitter::onNext,
            )
        }
    }

    override fun startView(context: Context, config: ViewConfig, emitter: ViewEmitter) {
        if (style == PintFieldStyle.TEXT) {
            emitter.onNext(UpdateNumericConfig(formatDataTypeId = DataType.Type.VARIABILITY_INDEX))
            if (config.preview) {
                emitter.launchCancellable(style.cancellationLabel(preview = true)) {
                    runtime().numericPreview().collect { message ->
                        emitter.onNext(ShowCustomStreamState(message = message, color = null))
                    }
                }
            }
            return
        }

        emitter.onNext(UpdateGraphicConfig(showHeader = style != PintFieldStyle.FILL))

        val displayMetrics = context.resources.displayMetrics
        val (fieldWidthDp, fieldHeightDp) = resolveFieldSize(
            viewSize = config.viewSize,
            gridSize = config.gridSize,
            screenSize = displayMetrics.widthPixels to displayMetrics.heightPixels,
            density = displayMetrics.density,
        )
        val baseLayout = PintFieldLayout.forSize(
            preview = config.preview,
            widthDp = fieldWidthDp,
            heightDp = fieldHeightDp,
            boundariesEnabled = config.boundariesEnabled,
        )
        val renderer = PintRemoteViews(
            packageName = context.packageName,
            displayDensity = displayMetrics.density,
            scaledDensity = displayMetrics.scaledDensity,
        )
        fun render(frame: PintFrame): android.widget.RemoteViews {
            if (style == PintFieldStyle.FILL) {
                return renderer.renderFill(
                    display = fillDisplayFor(frame),
                    alignment = config.alignment,
                    textSizeSp = config.textSize,
                    fieldWidthDp = fieldWidthDp,
                    fieldHeightDp = fieldHeightDp,
                )
            }
            val display = displayFor(frame)
            val renderLayout = if (config.preview) {
                previewLayoutFor(
                    hasCompletedCount = display.second.isNotEmpty(),
                    widthDp = fieldWidthDp,
                    heightDp = fieldHeightDp,
                    boundariesEnabled = config.boundariesEnabled,
                )
            } else {
                baseLayout
            }
            return renderer.render(
                display = display,
                layout = renderLayout,
                alignment = config.alignment,
                textSizeSp = config.textSize,
                boundariesEnabled = config.boundariesEnabled,
                fieldWidthDp = fieldWidthDp,
                fieldHeightDp = fieldHeightDp,
            )
        }
        if (BuildConfig.DEBUG) {
            Log.d(
                TAG,
                "grid=${config.gridSize} viewPx=${config.viewSize} fieldDp=" +
                    "${fieldWidthDp}x${fieldHeightDp} textSp=${config.textSize} " +
                    "alignment=${config.alignment} boundaries=${config.boundariesEnabled} " +
                    "preview=${config.preview} density=${displayMetrics.density} " +
                    "scaledDensity=${displayMetrics.scaledDensity} style=$style " +
                    "baseTreatment=$baseLayout",
            )
        }
        if (config.preview) {
            emitter.launchCancellable(style.cancellationLabel(preview = true)) {
                val frames = if (style == PintFieldStyle.FILL) fillPreviewFrames() else graphicalPreviewFrames()
                runtime().graphicalPreview(frames).collect { frame ->
                    emitter.updateView(render(frame))
                }
            }
            return
        }

        emitter.launchCancellable(style.cancellationLabel(preview = false)) {
            runtime().runGraphicalStream(
                calorieStates = karooSystem.streamDataFlow(DataType.Type.CALORIES),
                caloriesPerBeer = beerCalories,
                emit = { frame -> emitter.updateView(render(frame)) },
            )
        }
    }

    private fun runtime() = PintDataFieldRuntime(
        nowMillis = SystemClock::elapsedRealtime,
    )

    private companion object {
        const val TAG = "PintProgressField"
    }
}

internal fun Emitter<*>.launchCancellable(
    label: String,
    block: suspend CoroutineScope.() -> Unit,
) {
    val job = CoroutineScope(Dispatchers.Default).launch(block = block)
    setCancellable {
        job.cancel()
        if (BuildConfig.DEBUG) Log.d("PintProgressField", "cancellation label=$label")
    }
}

internal enum class PintFieldStyle(
    val typeId: String,
) {
    FILL("pint-progress-fill"),
    MUG("pint-progress"),
    TEXT("pint-progress-text"),
}

internal fun PintFieldStyle.cancellationLabel(preview: Boolean): String = when (this) {
    PintFieldStyle.TEXT -> if (preview) "numeric-preview" else "numeric-live"
    PintFieldStyle.MUG -> if (preview) "graphical-preview" else "graphical-live"
    PintFieldStyle.FILL -> if (preview) "fill-preview" else "fill-live"
}

/** Runs the deterministic scheduling policy used by the Android data-field adapter */
internal class PintDataFieldRuntime(
    private val nowMillis: () -> Long,
) {
    suspend fun runNumericStream(
        calorieStates: Flow<StreamState>,
        caloriesPerBeer: Flow<Int>,
        dataTypeId: String,
        emit: suspend (StreamState) -> Unit,
    ) {
        var lastEmissionMillis: Long? = null
        calorieStates
            .combine(caloriesPerBeer) { state, target ->
                numericStateFrom(state, target, dataTypeId)
            }
            .distinctUntilChanged()
            .conflate()
            .collect { state ->
                pace(lastEmissionMillis)?.let { delay(it) }
                emit(state)
                lastEmissionMillis = nowMillis()
            }
    }

    fun numericPreview(): Flow<String> = preview(numericPreviewMessages())

    suspend fun runGraphicalStream(
        calorieStates: Flow<StreamState>,
        caloriesPerBeer: Flow<Int>,
        emit: suspend (PintFrame) -> Unit,
    ) {
        val reducer = PintViewReducer()
        val activeTransition = AtomicReference<ActiveGraphicalTransition?>(null)
        var lastEmissionMillis: Long? = null

        suspend fun emitAfter(delayMillis: Long, frame: () -> PintFrame) {
            if (delayMillis > 0) delay(delayMillis)
            pace(lastEmissionMillis)?.let { delay(it) }
            emit(frame())
            lastEmissionMillis = nowMillis()
        }

        calorieStates
            .combine(caloriesPerBeer) { state, target -> state to target }
            .conflate()
            .mapNotNull { (state, target) ->
                val update = reducer.accept(state, target, activeTransition.get()?.completed)
                    ?: return@mapNotNull null
                if (update is PintViewUpdate.RefreshTransition) {
                    val transition = activeTransition.get()
                    if (transition?.completed == update.steady.progress.completed) {
                        transition.steady.set(update.steady)
                        null
                    } else {
                        PintViewUpdate.Render(update.steady)
                    }
                } else {
                    update
                }
            }
            .collectLatest { update ->
                when (update) {
                    is PintViewUpdate.Render -> emitAfter(0) { update.frame }

                    is PintViewUpdate.BeginTransition -> {
                        val transition = ActiveGraphicalTransition(
                            update.steady.progress.completed,
                            AtomicReference(update.steady),
                        )
                        activeTransition.set(transition)
                        try {
                            update.transientFrames.forEach { timedFrame ->
                                emitAfter(timedFrame.delayMillis) { timedFrame.frame }
                            }
                            emitAfter(update.steadyDelayMillis) {
                                activeTransition.compareAndSet(transition, null)
                                transition.steady.get()
                            }
                        } finally {
                            activeTransition.compareAndSet(transition, null)
                        }
                    }

                    is PintViewUpdate.RefreshTransition -> emitAfter(0) { update.steady }
                }
            }
    }

    fun graphicalPreview(frames: List<PintFrame>): Flow<PintFrame> = preview(frames)

    private fun <T> preview(values: List<T>): Flow<T> = flow {
        while (true) {
            values.forEach { value ->
                emit(value)
                delay(VIEW_UPDATE_INTERVAL_MILLIS)
            }
        }
    }

    private fun pace(lastEmissionMillis: Long?): Long? = lastEmissionMillis?.let { last ->
        (VIEW_UPDATE_INTERVAL_MILLIS - (nowMillis() - last)).takeIf { it > 0 }
    }

    private companion object {
        const val VIEW_UPDATE_INTERVAL_MILLIS = 1_000L
    }
}

private data class ActiveGraphicalTransition(
    val completed: Int,
    val steady: AtomicReference<PintFrame.Steady>,
)

internal fun KarooSystemService.streamDataFlow(dataTypeId: String): Flow<StreamState> =
    streamDataFlow(
        register = { onError, onComplete, onState ->
            addConsumer<OnStreamState>(
                params = OnStreamState.StartStreaming(dataTypeId),
                onError = onError,
                onComplete = onComplete,
                onEvent = { event -> onState(event.state) },
            )
        },
        unregister = ::removeConsumer,
    )

/** Adapts the SDK consumer into a finite Flow with idempotent cleanup for cancellation races */
internal fun streamDataFlow(
    register: (
        onError: (String) -> Unit,
        onComplete: () -> Unit,
        onState: (StreamState) -> Unit,
    ) -> String,
    unregister: (String) -> Unit,
): Flow<StreamState> = callbackFlow {
    val terminated = AtomicBoolean(false)

    fun terminate() {
        if (terminated.compareAndSet(false, true)) {
            trySend(StreamState.NotAvailable)
            close()
        }
    }

    val listenerId = register(
        { _: String -> terminate() },
        { terminate() },
        { state: StreamState ->
            if (!terminated.get()) trySend(state)
        },
    )
    awaitClose { unregister(listenerId) }
}
