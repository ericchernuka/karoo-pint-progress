package io.ericchernuka.pintprogress.core

/** Text-only presentation, expressed in completed pints plus a floored tenth of the next pint. */
data class PintTextDisplay(
    val value: String,
)

object PintTextPresentation {
    fun displayFor(frame: PintFrame): PintTextDisplay = when (frame) {
        PintFrame.Unavailable -> PintTextDisplay(UNAVAILABLE_VALUE)
        is PintFrame.Steady -> PintTextDisplay(frame.progress.tenthText())
        is PintFrame.FullBubbles -> PintTextDisplay(frame.completed.fullText())
        is PintFrame.Draining -> PintTextDisplay(frame.completed.fullText())
    }

    /** Representative production values for Karoo's page-editor preview. */
    fun previewFrames(): List<PintFrame> = listOf(
        PintFrame.Steady(PintProgress(completed = 0, fillBucket = 10)),
        PintFrame.Steady(PintProgress(completed = 0, fillBucket = 16)),
        PintFrame.FullBubbles(completed = 1),
    )

    private fun PintProgress.tenthText(): String = "$completed.${fillBucket / BUCKETS_PER_TENTH}"

    private fun Int.fullText(): String = "$this.0"

    private const val BUCKETS_PER_TENTH = 2
    private const val UNAVAILABLE_VALUE = "—"
}
