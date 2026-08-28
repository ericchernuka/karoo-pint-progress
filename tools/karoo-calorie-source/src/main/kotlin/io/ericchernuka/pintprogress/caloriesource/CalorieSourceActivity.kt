package io.ericchernuka.pintprogress.caloriesource

import android.app.Activity
import android.os.Bundle
import android.widget.Button
import android.widget.TextView

class CalorieSourceActivity : Activity() {
    private lateinit var outputStore: CalorieOutputStore
    private lateinit var status: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_calorie_source)
        outputStore = CalorieOutputStore(this)
        status = findViewById(R.id.output_status)

        mapOf(
            R.id.output_50 to CaloriePreset.HALF,
            R.id.output_80 to CaloriePreset.EIGHTY_PERCENT,
            R.id.output_95 to CaloriePreset.NINETY_FIVE_PERCENT,
            R.id.output_one_pint to CaloriePreset.ONE_PINT,
            R.id.output_99 to CaloriePreset.COUNT_99,
            R.id.output_100 to CaloriePreset.COUNT_100,
            R.id.output_multi_pint to CaloriePreset.TWO_AND_A_HALF_PINTS,
        ).forEach { (buttonId, preset) ->
            findViewById<Button>(buttonId).setOnClickListener {
                outputStore.write(outputStore.read().select(preset.calories))
                renderStatus()
            }
        }

        findViewById<Button>(R.id.output_silence).setOnClickListener {
            outputStore.write(outputStore.read().silence())
            renderStatus()
        }
        findViewById<Button>(R.id.output_resume).setOnClickListener {
            outputStore.write(outputStore.read().resume())
            renderStatus()
        }

        renderStatus()
    }

    private fun renderStatus() {
        val output = outputStore.read()
        status.text = if (!output.isEmitting) {
            getString(R.string.status_silent)
        } else {
            getString(R.string.status_emitting, output.targetCalories.toInt())
        }
    }
}
