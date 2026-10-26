package io.ericchernuka.pintprogress.core

const val VIEW_UPDATE_INTERVAL_MILLIS = 1_000L

/** Holds every field to the one-Hz update cap, measured from the last recorded emission */
class OneHzPacer {
    private var lastEmissionMillis: Long? = null

    /** Milliseconds to wait before emitting something due at [dueMillis] */
    fun delayMillis(nowMillis: Long, dueMillis: Long = nowMillis): Long {
        val pacedMillis = lastEmissionMillis?.plus(VIEW_UPDATE_INTERVAL_MILLIS) ?: Long.MIN_VALUE
        return (maxOf(dueMillis, pacedMillis) - nowMillis).coerceAtLeast(0)
    }

    fun emitted(nowMillis: Long) {
        lastEmissionMillis = nowMillis
    }
}
