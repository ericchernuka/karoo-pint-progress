package io.ericchernuka.pintprogress.core

/**
 * The Karoo system service is the only process that should control this extension's public
 * Binder interface. Android assigns package names to UIDs, so another installed app cannot claim
 * this identity alongside the system app.
 */
object KarooCallerPolicy {
    const val KAROO_SYSTEM_PACKAGE = "io.hammerhead.appstore"

    fun allows(packageNames: Array<String>?): Boolean =
        packageNames?.any { it == KAROO_SYSTEM_PACKAGE } == true
}
