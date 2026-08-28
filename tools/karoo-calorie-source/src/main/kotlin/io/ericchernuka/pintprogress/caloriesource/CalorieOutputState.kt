package io.ericchernuka.pintprogress.caloriesource

enum class CaloriePreset(val calories: Double) {
    HALF(90.0),
    EIGHTY_PERCENT(144.0),
    NINETY_FIVE_PERCENT(171.0),
    ONE_PINT(180.0),
    COUNT_99(17_820.0),
    COUNT_100(18_000.0),
    TWO_AND_A_HALF_PINTS(450.0),
}

data class CalorieOutputState(
    val targetCalories: Double = CaloriePreset.NINETY_FIVE_PERCENT.calories,
    val isEmitting: Boolean = true,
) {
    fun select(calories: Double) = CalorieOutputState(calories, isEmitting = true)

    fun silence() = copy(isEmitting = false)

    fun resume() = copy(isEmitting = true)
}

internal fun allowsKarooCaller(packageNames: Array<String>?): Boolean =
    packageNames?.any { it == KAROO_SYSTEM_PACKAGE } == true

private const val KAROO_SYSTEM_PACKAGE = "io.hammerhead.appstore"
