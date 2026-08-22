/**
 * Copyright (c) 2025 SRAM LLC.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/*
 * Modified by Eric Chernuka for Pint Progress. See NOTICE for details.
 */

package io.hammerhead.karooext.extension

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.Bundle
import android.os.IBinder
import io.hammerhead.karooext.EXT_LIB_VERSION
import io.hammerhead.karooext.aidl.IHandler
import io.hammerhead.karooext.aidl.IKarooExtension
import io.hammerhead.karooext.internal.Emitter
import io.hammerhead.karooext.internal.ViewEmitter
import io.hammerhead.karooext.internal.serializableFromBundle
import io.hammerhead.karooext.models.BonusAction
import io.hammerhead.karooext.models.DataType
import io.hammerhead.karooext.models.Device
import io.hammerhead.karooext.models.DeviceEvent
import io.hammerhead.karooext.models.ExtensionInfo
import io.hammerhead.karooext.models.FitEffect
import io.hammerhead.karooext.models.MapEffect
import io.hammerhead.karooext.models.StreamState
import io.hammerhead.karooext.models.ViewConfig
import timber.log.Timber
import java.util.concurrent.ConcurrentHashMap

/**
 * Base class for implementation of Karoo Extension.
 *
 * @sample [karooExtensionUsage]
 */
abstract class KarooExtension(
    /**
     * Extension ID, matching [ExtensionInfo.id] from extension manifest.
     *
     * This is different from your application id (com.something) and cannot contain '.'
     */
    val extension: String,
    /**
     * Extension version (separate from [EXT_LIB_VERSION]).
     */
    val version: String,
) : Service() {
    private val emitters = ConcurrentHashMap<String, Emitter<*>>()

    /**
     * Gives an extension a chance to reject Binder calls from an untrusted process. The SDK keeps
     * the default permissive for backward compatibility; production extensions should override it.
     */
    protected open fun isCallerAllowed(callingUid: Int): Boolean = true

    init {
        check(extension.none { it == '.' }) { "extension ID cannot contain '.'" }
    }

    /**
     * @suppress
     */
    final override fun onBind(intent: Intent): IBinder {
        Timber.i("$TAG: extension $extension [$version] started by Karoo System")
        return binder
    }

    private val binder by lazy {
        object : IKarooExtension.Stub() {
            override fun libVersion(): String {
                enforceCallerAllowed()
                return EXT_LIB_VERSION
            }

            override fun startScan(id: String, handler: IHandler) {
                enforceCallerAllowed()
                val emitter = Emitter.create<Device>(packageName, handler)
                registerEmitter(id, emitter)
                Timber.d("$TAG: startScan")
                startScan(emitter)
            }

            override fun stopScan(id: String) {
                enforceCallerAllowed()
                Timber.d("$TAG: stopScan")
                cancelEmitter(id)
            }

            override fun connectDevice(id: String, uid: String, handler: IHandler) {
                enforceCallerAllowed()
                val emitter = Emitter.create<DeviceEvent>(packageName, handler)
                registerEmitter(id, emitter)
                Timber.d("$TAG: connectDevice")
                connectDevice(uid, emitter)
            }

            override fun disconnectDevice(id: String) {
                enforceCallerAllowed()
                Timber.d("$TAG: disconnectDevice")
                cancelEmitter(id)
            }

            override fun startStream(id: String, typeId: String, handler: IHandler) {
                enforceCallerAllowed()
                types.firstOrNull { it.typeId == typeId }?.let {
                    val emitter = Emitter.create<StreamState>(packageName, handler)
                    registerEmitter(id, emitter)
                    Timber.d("$TAG: startStream")
                    it.startStream(emitter)
                }
            }

            override fun stopStream(id: String) {
                enforceCallerAllowed()
                Timber.d("$TAG: stopStream")
                cancelEmitter(id)
            }

            override fun startView(id: String, typeId: String, config: Bundle, handler: IHandler) {
                enforceCallerAllowed()
                val viewConfig = config.serializableFromBundle<ViewConfig>(MAX_VIEW_CONFIG_CHARS) ?: return
                types.firstOrNull { it.typeId == typeId }?.let {
                    val emitter = ViewEmitter(packageName, handler)
                    registerEmitter(id, emitter)
                    Timber.d("$TAG: startView")
                    it.startView(this@KarooExtension, viewConfig, emitter)
                }
            }

            override fun stopView(id: String) {
                enforceCallerAllowed()
                Timber.d("$TAG: stopView")
                cancelEmitter(id)
            }

            override fun startMap(id: String, handler: IHandler) {
                enforceCallerAllowed()
                val emitter = Emitter.create<MapEffect>(packageName, handler)
                registerEmitter(id, emitter)
                Timber.d("$TAG: startMap")
                startMap(emitter)
            }

            override fun stopMap(id: String) {
                enforceCallerAllowed()
                Timber.d("$TAG: stopMap")
                cancelEmitter(id)
            }

            override fun startFit(id: String, handler: IHandler) {
                enforceCallerAllowed()
                val emitter = Emitter.create<FitEffect>(packageName, handler)
                registerEmitter(id, emitter)
                Timber.d("$TAG: startFit")
                startFit(emitter)
            }

            override fun stopFit(id: String) {
                enforceCallerAllowed()
                Timber.d("$TAG: stopFit")
                cancelEmitter(id)
            }

            override fun onBonusAction(actionid: String) {
                enforceCallerAllowed()
                Timber.d("$TAG: onBonusAction")
                this@KarooExtension.onBonusAction(actionid)
            }
        }
    }

    /**
     * Provide a list of pre-defined static data type implementations.
     *
     * This list should match the [DataType.typeId]'s listed in the extension manifest data-type elements.
     */
    open val types: List<DataTypeImpl> = emptyList()

    /**
     * Start to scan for devices.
     *
     * This will only be called if [ExtensionInfo.scansDevices] is true in the extension manifest.
     *
     * @see [Device]
     */
    open fun startScan(emitter: Emitter<Device>) {}

    /**
     * Connect to a device by uid that was previously emitted by [startScan].
     *
     * @see [DeviceEvent]
     */
    open fun connectDevice(uid: String, emitter: Emitter<DeviceEvent>) {}

    /**
     * Start providing effects for the map layer
     *
     * This will be called only if [ExtensionInfo] has `mapLayer` set to true
     *
     * @see [MapEffect]
     * @since 1.1.3
     */
    open fun startMap(emitter: Emitter<MapEffect>) {}

    /**
     * Start providing effects for FIT file writing
     *
     * This will be called only if [ExtensionInfo] has `fitFile` set to true
     *
     * @see [FitEffect]
     */
    open fun startFit(emitter: Emitter<FitEffect>) {}

    /**
     * Called when an [BonusAction] should be performed.
     *
     * Actions defined in [ExtensionInfo] can be assigned to a controller.
     * When the controller button is activated, the configured `actionId` will be passed
     * to the matching extension that defined it.
     *
     * @see [BonusAction]
     *
     * @since 1.1.7
     */
    open fun onBonusAction(actionId: String) {}

    override fun onDestroy() {
        emitters.values.forEach { it.cancel() }
        emitters.clear()
        super.onDestroy()
    }

    private fun enforceCallerAllowed() {
        val callingUid = Binder.getCallingUid()
        if (!isCallerAllowed(callingUid)) {
            Timber.w("$TAG: rejecting Binder call from an untrusted uid")
            throw SecurityException("Untrusted Karoo extension caller")
        }
    }

    private fun registerEmitter(id: String, emitter: Emitter<*>) {
        require(id.length <= MAX_SESSION_ID_LENGTH) { "Extension session id is too long" }

        val previous = emitters.put(id, emitter)
        if (previous == null && emitters.size > MAX_ACTIVE_EMITTERS) {
            emitters.remove(id, emitter)
            throw IllegalStateException("Too many active extension sessions")
        }
        previous?.cancel()
    }

    private fun cancelEmitter(id: String) {
        emitters.remove(id)?.cancel()
    }

    /**
     * @suppress
     */
    companion object {
        private const val TAG = "KarooExtension"
        private const val MAX_ACTIVE_EMITTERS = 64
        private const val MAX_SESSION_ID_LENGTH = 128
        private const val MAX_VIEW_CONFIG_CHARS = 4_096
    }
}
