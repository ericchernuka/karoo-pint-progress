package io.ericchernuka.pintprogress.caloriesource

import org.junit.Assert.assertEquals
import org.junit.Test

class PowerTargetControllerTest {
    private val controller = PowerTargetController()

    @Test
    fun `does not add power before Calories are available`() {
        assertEquals(0.0, controller.watts(currentCalories = null, targetCalories = 180.0), 0.0)
    }

    @Test
    fun `stops at or above the target`() {
        assertEquals(0.0, controller.watts(currentCalories = 180.0, targetCalories = 180.0), 0.0)
        assertEquals(0.0, controller.watts(currentCalories = 181.0, targetCalories = 180.0), 0.0)
    }

    @Test
    fun `budgets only the remaining energy while Calories feedback lags`() {
        assertEquals(50_000.0, controller.watts(currentCalories = 0.0, targetCalories = 90.0), 0.0)
        assertEquals(34_600.0, controller.watts(currentCalories = 0.0, targetCalories = 90.0), 0.0)
        assertEquals(0.0, controller.watts(currentCalories = 0.0, targetCalories = 90.0), 0.0)
    }

    @Test
    fun `caps each one second power sample`() {
        assertEquals(50_000.0, controller.watts(currentCalories = 0.0, targetCalories = 18_000.0), 0.0)
    }

    @Test
    fun `a changed target starts a new budget from confirmed Calories`() {
        controller.watts(currentCalories = 0.0, targetCalories = 90.0)

        assertEquals(50_000.0, controller.watts(currentCalories = 90.0, targetCalories = 180.0), 0.0)
    }

    @Test
    fun `a ride reset clears the previous energy budget`() {
        controller.watts(currentCalories = 165.0, targetCalories = 90.0)

        assertEquals(50_000.0, controller.watts(currentCalories = 0.0, targetCalories = 90.0), 0.0)
    }

    @Test
    fun `stable feedback below target rearms a correction after one minute`() {
        controller.watts(currentCalories = 0.0, targetCalories = 90.0)
        controller.watts(currentCalories = 0.0, targetCalories = 90.0)

        repeat(59) {
            assertEquals(0.0, controller.watts(currentCalories = 80.0, targetCalories = 90.0), 0.0)
        }
        assertEquals(9_400.0, controller.watts(currentCalories = 80.0, targetCalories = 90.0), 0.0)
    }
}
