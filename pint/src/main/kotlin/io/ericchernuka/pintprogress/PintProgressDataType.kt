package io.ericchernuka.pintprogress

import android.content.Context
import android.os.SystemClock
import io.ericchernuka.pintprogress.core.PintFrame
import io.ericchernuka.pintprogress.core.PintFieldLayout
import io.ericchernuka.pintprogress.core.PintPresentation
import io.ericchernuka.pintprogress.core.PintViewReducer
import io.hammerhead.karooext.KarooSystemService
import io.hammerhead.karooext.extension.DataTypeImpl
import io.hammerhead.karooext.internal.ViewEmitter
import io.hammerhead.karooext.models.DataType
import io.hammerhead.karooext.models.UpdateGraphicConfig
import io.hammerhead.karooext.models.ViewConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.launch

class PintProgressDataType(
    private val karooSystem: KarooSystemService,
    extension: String,
) : DataTypeImpl(extension, TYPE_ID) {
    override fun startView(context: Context, config: ViewConfig, emitter: ViewEmitter) {
        emitter.onNext(UpdateGraphicConfig(showHeader = false))

        val renderer = PintRemoteViews(context.packageName)
        val layout = PintFieldLayout.forSize(
            preview = config.preview,
            widthPx = config.viewSize.first,
            heightPx = config.viewSize.second,
        )
        if (config.preview) {
            emitter.updateView(renderer.render(PintPresentation.displayFor(PintViewReducer.previewFrame()), layout))
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

                emitter.updateView(renderer.render(PintPresentation.displayFor(frame), layout))
                lastViewUpdateMillis = SystemClock.elapsedRealtime()
            }

            karooSystem.streamDataFlow(DataType.Type.CALORIES)
                .conflate()
                .collect { state ->
                    reducer.accept(state)?.frames?.forEach { timedFrame ->
                        if (timedFrame.delayMillis > 0) delay(timedFrame.delayMillis)
                        render(timedFrame.frame)
                    }
                }
        }
        emitter.setCancellable(job::cancel)
    }

    private companion object {
        const val TYPE_ID = "pint-progress"
        const val VIEW_UPDATE_INTERVAL_MILLIS = 1_000L
    }
}
