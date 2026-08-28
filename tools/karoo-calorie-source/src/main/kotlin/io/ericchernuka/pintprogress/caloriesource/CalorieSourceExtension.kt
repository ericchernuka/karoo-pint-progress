package io.ericchernuka.pintprogress.caloriesource

import io.hammerhead.karooext.KarooSystemService
import io.hammerhead.karooext.extension.KarooExtension
import io.hammerhead.karooext.internal.Emitter
import io.hammerhead.karooext.models.ConnectionStatus
import io.hammerhead.karooext.models.DataPoint
import io.hammerhead.karooext.models.DataType
import io.hammerhead.karooext.models.Device
import io.hammerhead.karooext.models.DeviceEvent
import io.hammerhead.karooext.models.OnConnectionStatus
import io.hammerhead.karooext.models.OnDataPoint
import io.hammerhead.karooext.models.OnStreamState
import io.hammerhead.karooext.models.StreamState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class CalorieSourceExtension : KarooExtension(EXTENSION_ID, BuildConfig.VERSION_NAME) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val outputStore by lazy { CalorieOutputStore(this) }
    private val karooSystem by lazy { KarooSystemService(this) }
    private val powerController = PowerTargetController()
    private var calorieConsumerId: String? = null
    @Volatile private var currentCalories: Double? = null

    override fun onCreate() {
        super.onCreate()
        calorieConsumerId = karooSystem.addConsumer<OnStreamState>(
            params = OnStreamState.StartStreaming(DataType.Type.CALORIES),
            onEvent = { event ->
                currentCalories = (event.state as? StreamState.Streaming)?.dataPoint?.singleValue
            },
        )
        karooSystem.connect()
    }

    override fun startScan(emitter: Emitter<Device>) {
        emitter.onNext(DEVICE)
    }

    override fun connectDevice(uid: String, emitter: Emitter<DeviceEvent>) {
        if (uid != DEVICE_UID) {
            emitter.onError(IllegalArgumentException("Unknown device: $uid"))
            return
        }

        emitter.onNext(OnConnectionStatus(ConnectionStatus.CONNECTED))
        val job = scope.launch {
            while (isActive) {
                val output = outputStore.read()
                if (output.isEmitting) {
                    emitter.onNext(
                        OnDataPoint(
                            DataPoint(
                                dataTypeId = DataType.Source.SPEED,
                                values = mapOf(DataType.Field.SPEED to TEST_SPEED_METERS_PER_SECOND),
                                sourceId = DEVICE_UID,
                            ),
                        ),
                    )
                    emitter.onNext(
                        OnDataPoint(
                            DataPoint(
                                dataTypeId = DataType.Source.POWER,
                                values = mapOf(
                                    DataType.Field.POWER to powerController.watts(
                                        currentCalories = currentCalories,
                                        targetCalories = output.targetCalories,
                                    ),
                                ),
                                sourceId = DEVICE_UID,
                            ),
                        ),
                    )
                }
                delay(EMISSION_INTERVAL_MILLIS)
            }
        }
        emitter.setCancellable(job::cancel)
    }

    override fun onDestroy() {
        scope.cancel()
        calorieConsumerId?.let(karooSystem::removeConsumer)
        karooSystem.disconnect()
        super.onDestroy()
    }

    override fun isCallerAllowed(callingUid: Int): Boolean =
        allowsKarooCaller(packageManager.getPackagesForUid(callingUid))

    private companion object {
        const val EXTENSION_ID = "pintprogress-calorie-source"
        const val DEVICE_UID = "pintprogress-calorie-source-device"
        const val EMISSION_INTERVAL_MILLIS = 1_000L
        const val TEST_SPEED_METERS_PER_SECOND = 5.0
        val DEVICE = Device(
            extension = EXTENSION_ID,
            uid = DEVICE_UID,
            dataTypes = listOf(DataType.Source.POWER, DataType.Source.SPEED),
            displayName = "Pint QA Calorie Driver",
        )
    }
}
