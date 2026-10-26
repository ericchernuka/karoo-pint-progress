package io.ericchernuka.pintprogress

import io.ericchernuka.pintprogress.core.allowsKarooCaller
import io.hammerhead.karooext.KarooSystemService
import io.hammerhead.karooext.extension.KarooExtension

class PintProgressExtension : KarooExtension(EXTENSION_ID, BuildConfig.VERSION_NAME) {
    private val karooSystem by lazy { KarooSystemService(this) }
    private val beerCalories by lazy { BeerCaloriesStore(this).values }

    override val types by lazy {
        listOf(
            PintsFillDataType(karooSystem, beerCalories, extension),
            PintsCountDataType(karooSystem, beerCalories, extension),
        )
    }

    override fun onCreate() {
        super.onCreate()
        karooSystem.connect()
    }

    override fun onDestroy() {
        karooSystem.disconnect()
        super.onDestroy()
    }

    override fun isCallerAllowed(callingUid: Int): Boolean =
        allowsKarooCaller(packageManager.getPackagesForUid(callingUid))

    private companion object {
        const val EXTENSION_ID = "pintprogress"
    }
}
