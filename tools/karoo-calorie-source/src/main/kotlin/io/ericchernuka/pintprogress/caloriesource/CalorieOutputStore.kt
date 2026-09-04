package io.ericchernuka.pintprogress.caloriesource

import android.content.Context

class CalorieOutputStore(context: Context) {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun read(): CalorieOutputState {
        return CalorieOutputState(
            targetCalories = preferences.getFloat(TARGET_VALUE, DEFAULT_TARGET).toDouble(),
            isEmitting = preferences.getBoolean(IS_EMITTING, false),
        )
    }

    fun write(state: CalorieOutputState) {
        preferences.edit()
            .putBoolean(IS_EMITTING, state.isEmitting)
            .putFloat(TARGET_VALUE, state.targetCalories.toFloat())
            .apply()
    }

    private companion object {
        const val PREFERENCES_NAME = "calorie-output"
        const val IS_EMITTING = "is-emitting"
        const val TARGET_VALUE = "target-calories"
        val DEFAULT_TARGET = CaloriePreset.NINETY_FIVE_PERCENT.calories.toFloat()
    }
}
