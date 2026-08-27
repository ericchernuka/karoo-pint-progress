package io.ericchernuka.pintprogress

import android.content.Context
import android.content.SharedPreferences
import io.ericchernuka.pintprogress.core.BeerCaloriesPolicy
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

/** App-private persistence shared by the launcher activity and extension service process */
internal class BeerCaloriesStore(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(
        PREFERENCES_NAME,
        Context.MODE_PRIVATE,
    )

    val values: Flow<Int> = callbackFlow {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == CALORIES_PER_BEER) trySend(read())
        }
        preferences.registerOnSharedPreferenceChangeListener(listener)
        trySend(read())
        awaitClose { preferences.unregisterOnSharedPreferenceChangeListener(listener) }
    }
        .distinctUntilChanged()

    fun set(value: Int) {
        preferences.edit()
            .putInt(CALORIES_PER_BEER, BeerCaloriesPolicy.normalize(value))
            .apply()
    }

    private fun read(): Int = BeerCaloriesPolicy.normalize(
        preferences.getInt(CALORIES_PER_BEER, BeerCaloriesPolicy.DEFAULT),
    )

    private companion object {
        const val PREFERENCES_NAME = "pint_progress_settings"
        const val CALORIES_PER_BEER = "calories_per_beer"
    }
}
