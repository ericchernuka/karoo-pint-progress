package io.ericchernuka.pintprogress.core

/** Pints Fill always shows its count, with a dash for unavailable progress */
fun fillDisplayFor(frame: PintFrame): Pair<PintFillAsset, String> = when (frame) {
    PintFrame.Unavailable -> PintFillAsset.FILL_UNAVAILABLE to "—"
    is PintFrame.Steady -> PintFillAsset.entries[frame.progress.fillBucket] to frame.progress.completed.toString()
    is PintFrame.FullBubbles -> PintFillAsset.FILL_FULL_FOAM to frame.completed.toString()
    is PintFrame.Draining -> PintFillAsset.FILL_DRAINING to frame.completed.toString()
}

fun fillPreviewFrames(): List<PintFrame> = listOf(
    PintFrame.Steady(PintProgress(completed = 0, fillBucket = 10)),
    PintFrame.Steady(PintProgress(completed = 0, fillBucket = 16)),
    PintFrame.FullBubbles(completed = 1),
    PintFrame.Draining(completed = 1),
)
