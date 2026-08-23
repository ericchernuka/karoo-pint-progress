package io.ericchernuka.pintprogress

import android.view.Gravity
import io.hammerhead.karooext.models.ViewConfig

/** Maps Karoo's user setting to the gravity of the complete count-and-mug group. */
internal fun ViewConfig.Alignment.gravity(): Int = when (this) {
    ViewConfig.Alignment.LEFT -> Gravity.LEFT
    ViewConfig.Alignment.CENTER -> Gravity.CENTER_HORIZONTAL
    ViewConfig.Alignment.RIGHT -> Gravity.RIGHT
} or Gravity.CENTER_VERTICAL
