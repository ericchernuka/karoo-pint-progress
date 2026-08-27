package io.ericchernuka.pintprogress.core

object KarooCallerPolicy { const val KAROO_SYSTEM_PACKAGE = "io.hammerhead.appstore"
    fun allows(packageNames: Array<String>?): Boolean =
        packageNames?.any { it == KAROO_SYSTEM_PACKAGE } == true }
