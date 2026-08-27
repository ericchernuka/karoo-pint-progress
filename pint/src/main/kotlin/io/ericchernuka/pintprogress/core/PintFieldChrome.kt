package io.ericchernuka.pintprogress.core

object PintFieldChrome { const val DEFAULT_EDGE_INSET_DP = 2
    const val BOUNDARY_EDGE_INSET_DP = 6
    fun edgeInsetDp(boundariesEnabled: Boolean): Int = if (boundariesEnabled) { BOUNDARY_EDGE_INSET_DP } else { DEFAULT_EDGE_INSET_DP } }
