package io.ericchernuka.pintprogress.core

/** Selects the host treatment for the text-only field. */
enum class PintTextLayout {
    PICKER,
    LIVE,
    ;

    companion object {
        fun forMode(preview: Boolean): PintTextLayout = if (preview) PICKER else LIVE
    }
}
