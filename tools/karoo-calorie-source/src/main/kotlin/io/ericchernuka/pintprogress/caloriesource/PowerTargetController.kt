package io.ericchernuka.pintprogress.caloriesource

class PowerTargetController {
    private var activeTargetCalories: Double? = null
    private var plannedCalories: Double? = null
    private var lastCurrentCalories: Double? = null
    private var stableFeedbackTicks = 0

    fun watts(currentCalories: Double?, targetCalories: Double): Double {
        currentCalories ?: return 0.0
        val previousCalories = lastCurrentCalories
        val feedbackChanged = previousCalories != currentCalories
        if (previousCalories?.let { currentCalories < it } == true) {
            activeTargetCalories = null
            plannedCalories = null
            stableFeedbackTicks = 0
        }
        lastCurrentCalories = currentCalories
        if (activeTargetCalories != targetCalories) {
            activeTargetCalories = targetCalories
            plannedCalories = currentCalories
            stableFeedbackTicks = 0
        }

        var planned = maxOf(currentCalories, plannedCalories ?: currentCalories)
        if (currentCalories >= targetCalories) {
            stableFeedbackTicks = 0
            return 0.0
        }
        if (planned >= targetCalories) {
            stableFeedbackTicks = if (feedbackChanged) 1 else stableFeedbackTicks + 1
            if (stableFeedbackTicks < CORRECTION_DELAY_TICKS) return 0.0

            planned = currentCalories
            plannedCalories = currentCalories
            stableFeedbackTicks = 0
        }

        val remaining = targetCalories - planned
        val watts = (remaining * JOULES_PER_CALORIE).coerceAtMost(MAXIMUM_WATTS)
        plannedCalories = planned + watts / JOULES_PER_CALORIE
        return watts
    }

    private companion object {
        const val JOULES_PER_CALORIE = 940.0
        const val MAXIMUM_WATTS = 50_000.0
        const val CORRECTION_DELAY_TICKS = 60
    }
}
