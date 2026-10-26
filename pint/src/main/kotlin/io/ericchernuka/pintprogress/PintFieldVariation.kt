package io.ericchernuka.pintprogress

/** Durable Karoo type identity for each field variation; see ADR 0001 and ADR 0002 */
internal enum class PintFieldVariation(
    val typeId: String,
    private val cancellationRoute: String,
) {
    PINTS_FILL("pint-progress-fill", "fill"),
    PINTS_COUNT("pint-progress-text", "numeric"),
    ;

    fun cancellationLabel(preview: Boolean): String =
        "$cancellationRoute-${if (preview) "preview" else "live"}"
}
