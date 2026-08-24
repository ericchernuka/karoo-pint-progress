package io.ericchernuka.pintprogress

import android.widget.RemoteViews
import io.ericchernuka.pintprogress.core.PintFieldChrome
import io.ericchernuka.pintprogress.core.PintTextDisplay
import io.ericchernuka.pintprogress.core.PintTextTypography
import io.hammerhead.karooext.models.ViewConfig
import kotlin.math.roundToInt

/** Thin Android adapter for the text-only graphical data field. */
internal class PintTextRemoteViews(
    private val packageName: String,
    private val displayDensity: Float,
    scaledDensity: Float,
) {
    private val fontScale = scaledDensity / displayDensity

    fun render(
        display: PintTextDisplay,
        alignment: ViewConfig.Alignment,
        textSizeSp: Int,
        boundariesEnabled: Boolean,
        fieldWidthDp: Int,
    ): RemoteViews = RemoteViews(packageName, R.layout.pint_progress_text_view).apply {
        val edgeInsetDp = PintFieldChrome.edgeInsetDp(boundariesEnabled)
        val edgeInsetPx = (edgeInsetDp * displayDensity).roundToInt()
        setViewPadding(R.id.pint_text_root, edgeInsetPx, edgeInsetPx, edgeInsetPx, edgeInsetPx)
        setInt(R.id.pint_text_value, "setGravity", alignment.gravity())
        setTextViewText(R.id.pint_text_value, display.value)

        val typography = PintTextTypography.forField(
            karooTextSizeSp = textSizeSp,
            value = display.value,
            fontScale = fontScale,
            contentWidthDp = fieldWidthDp - (edgeInsetDp * 2),
        )
        setTextViewTextSize(
            R.id.pint_text_value,
            android.util.TypedValue.COMPLEX_UNIT_SP,
            typography.textSizeSp,
        )
    }
}
