package io.ericchernuka.pintprogress

import android.app.Activity
import android.os.Bundle
import android.widget.Button
import android.widget.SeekBar
import android.widget.TextView
import io.ericchernuka.pintprogress.core.BeerCaloriesPolicy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

/** Dependency-light launcher screen for extension-wide Pint Progress settings */
class PintSettingsActivity : Activity() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val store by lazy { BeerCaloriesStore(applicationContext) }
    private val slider by lazy { findViewById<SeekBar>(R.id.calories_slider) }
    private val valueLabel by lazy { findViewById<TextView>(R.id.calories_value) }
    private val decreaseButton by lazy { findViewById<Button>(R.id.decrease_calories) }
    private val increaseButton by lazy { findViewById<Button>(R.id.increase_calories) }
    private var value = BeerCaloriesPolicy.DEFAULT
    private var dragging = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pint_settings)

        findViewById<android.view.View>(R.id.back_button).setOnClickListener { finish() }

        slider.max = BeerCaloriesPolicy.STEP_COUNT
        slider.setOnSeekBarChangeListener(
            object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                    if (fromUser) show(BeerCaloriesPolicy.fromSliderProgress(progress))
                }

                override fun onStartTrackingTouch(seekBar: SeekBar) {
                    dragging = true
                }

                override fun onStopTrackingTouch(seekBar: SeekBar) {
                    dragging = false
                    persist(BeerCaloriesPolicy.fromSliderProgress(seekBar.progress))
                }
            },
        )

        decreaseButton.setOnClickListener {
            persist(value - BeerCaloriesPolicy.STEP)
        }
        findViewById<Button>(R.id.reset_calories).setOnClickListener {
            persist(BeerCaloriesPolicy.DEFAULT)
        }
        increaseButton.setOnClickListener {
            persist(value + BeerCaloriesPolicy.STEP)
        }

        show(BeerCaloriesPolicy.DEFAULT)
        scope.launch {
            store.values.collect { storedValue ->
                if (!dragging) show(storedValue)
            }
        }
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    private fun persist(newValue: Int) {
        val normalized = BeerCaloriesPolicy.normalize(newValue)
        show(normalized)
        store.set(normalized)
    }

    private fun show(newValue: Int) {
        value = BeerCaloriesPolicy.normalize(newValue)
        slider.progress = BeerCaloriesPolicy.toSliderProgress(value)
        slider.contentDescription = getString(R.string.calories_slider_accessibility, value)
        valueLabel.text = getString(R.string.calories_value, value)
        valueLabel.contentDescription = getString(R.string.calories_value_accessibility, value)
        decreaseButton.isEnabled = value > BeerCaloriesPolicy.MIN
        increaseButton.isEnabled = value < BeerCaloriesPolicy.MAX
    }
}
