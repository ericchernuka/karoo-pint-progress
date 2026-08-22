/**
 * Copyright (c) 2024 SRAM LLC.
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

package io.hammerhead.karooext.internal

import android.os.Bundle
import io.hammerhead.karooext.BUNDLE_VALUE
import io.hammerhead.karooext.aidl.IHandler
import timber.log.Timber

/**
 * @suppress
 */
inline fun <reified T> Bundle.serializableFromBundle(maxChars: Int = Int.MAX_VALUE): T? {
    val encoded = try {
        getString(BUNDLE_VALUE)
    } catch (exception: RuntimeException) {
        Timber.w(exception, "Unable to read a Karoo event bundle")
        return null
    }

    if (encoded == null || encoded.length > maxChars) {
        Timber.w("Karoo event bundle is missing or exceeds its allowed size")
        return null
    }

    return try {
        DefaultJson.decodeFromString(encoded)
    } catch (exception: RuntimeException) {
        Timber.w(exception, "Unable to decode a Karoo event bundle")
        null
    }
}

/**
 * @suppress
 */
inline fun <reified T> createConsumer(
    crossinline onNextCallback: (T) -> Unit,
    noinline onErrorCallback: (String) -> Unit,
    noinline onCompleteCallback: () -> Unit,
): IHandler {
    return object : IHandler.Stub() {
        override fun onNext(bundle: Bundle) {
            bundle.serializableFromBundle<T>()?.let {
                onNextCallback(it)
            }
        }

        override fun onError(msg: String) {
            onErrorCallback.invoke(msg)
        }

        override fun onComplete() {
            onCompleteCallback.invoke()
        }
    }
}
