package io.ericchernuka.pintprogress.core

/** Static drawable frames generated in `tools/generate-drawables.mjs`. */
enum class PintAsset {
    PINT_00,
    PINT_05,
    PINT_10,
    PINT_15,
    PINT_20,
    PINT_25,
    PINT_30,
    PINT_35,
    PINT_40,
    PINT_45,
    PINT_50,
    PINT_55,
    PINT_60,
    PINT_65,
    PINT_70,
    PINT_75,
    PINT_80,
    PINT_85,
    PINT_90,
    PINT_95,
    FULL_BUBBLES,
    DRAINING,
    UNAVAILABLE,
}

data class PintDisplay(
    val asset: PintAsset,
    val count: String,
)

object PintPresentation {
    fun displayFor(frame: PintFrame): PintDisplay = when (frame) {
        PintFrame.Unavailable -> PintDisplay(
            asset = PintAsset.UNAVAILABLE,
            count = "",
        )
        is PintFrame.Steady -> PintDisplay(
            asset = PintAsset.entries[frame.progress.fillBucket],
            count = frame.progress.completed.counterText(),
        )
        is PintFrame.FullBubbles -> PintDisplay(
            asset = PintAsset.FULL_BUBBLES,
            count = frame.completed.counterText(),
        )
        is PintFrame.Draining -> PintDisplay(
            asset = PintAsset.DRAINING,
            count = frame.completed.counterText(),
        )
    }

    private fun Int.counterText(): String = if (this == 0) "" else "$this+"
}
