package io.ericchernuka.pintprogress

import android.content.Context
import io.hammerhead.karooext.KarooSystemService
import io.hammerhead.karooext.extension.DataTypeImpl
import io.hammerhead.karooext.internal.Emitter
import io.hammerhead.karooext.internal.ViewEmitter
import io.hammerhead.karooext.models.DataType
import io.hammerhead.karooext.models.ShowCustomStreamState
import io.hammerhead.karooext.models.StreamState
import io.hammerhead.karooext.models.UpdateNumericConfig
import io.hammerhead.karooext.models.ViewConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect

/** Pints Count publishes a numeric stream and lets Karoo own its viewport and formatting */
internal class PintsCountDataType(
    private val karooSystem: KarooSystemService,
    private val beerCalories: Flow<Int>,
    extension: String,
) : DataTypeImpl(extension, VARIATION.typeId) {
    override fun startStream(emitter: Emitter<StreamState>) {
        emitter.launchCancellable(VARIATION.cancellationLabel(preview = false)) {
            deviceRuntime().runNumericStream(
                calorieStates = karooSystem.streamDataFlow(DataType.Type.CALORIES),
                caloriesPerBeer = beerCalories,
                dataTypeId = dataTypeId,
                emit = emitter::onNext,
            )
        }
    }

    override fun startView(context: Context, config: ViewConfig, emitter: ViewEmitter) {
        emitter.onNext(UpdateNumericConfig(formatDataTypeId = DataType.Type.VARIABILITY_INDEX))
        if (!config.preview) return
        emitter.launchCancellable(VARIATION.cancellationLabel(preview = true)) {
            deviceRuntime().numericPreview().collect { message ->
                emitter.onNext(ShowCustomStreamState(message = message, color = null))
            }
        }
    }

    private companion object {
        val VARIATION = PintFieldVariation.PINTS_COUNT
    }
}
